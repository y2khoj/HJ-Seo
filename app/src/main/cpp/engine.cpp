#include "audio_core.h"
#include "feedback_guard.h"
#include "record_alignment.h"
#include <aaudio/AAudio.h>
#include <jni.h>
#include <android/log.h>
#include <chrono>
#include <cstdio>
#include <mutex>
#include <string>
#include <thread>
#include <unistd.h>
using namespace lm;
static double monoSeconds(){timespec t{};clock_gettime(CLOCK_MONOTONIC,&t);return t.tv_sec+t.tv_nsec/1e9;}
class Engine {
public:
    std::atomic<float> params[20];
    AAudioStream *input=nullptr,*output=nullptr;
    std::atomic<bool> running{false},recording{false},writerRunning{false},recordPaused{false},segmentPending{false};
    std::atomic<int> recordWriters{0},fault{0};
    std::atomic<uint64_t> frames{0},recordFrames{0};
    std::atomic<float> frequency{0},inRms{0},outRms{0},beat{0};
    std::atomic<bool> overflow{false},diskError{false};
    std::atomic<double> recordStartClock{0};
    std::atomic<uint64_t> segmentSourceStart{0};uint64_t segmentOutputStart=0;std::vector<RecordedSpan> recordedSpans;
    RecordAlignment alignment;int recordSyncMs=0;std::atomic<int> requestedBursts{2};std::atomic<float> guideFrequency{0},pitchConfidence{0},pitchLevel{0};
    float guideEnvelope=0,lastGuideFrequency=440;
    int sr=48000;DSP dsp;PlaybackMixer playback;FeedbackGuard feedback;int monitorMode=0;std::atomic<float> monitorLevel{1};Ring<32768> pitchRing;Ring<1048576> recordRing;
    std::thread analyzer,writer;
    std::mutex notesMutex;std::vector<Note> notes;Note active;bool hasActive=false;
    double candidateTime=0,lastSeen=0,recordStartTime=0;int candidate=-1,candidateCount=0;
    std::array<float,4096> inputBuffer{};std::array<float,8192> recordBuffer{},capturedBuffer{};
    uint32_t noiseState=12345678;double transport=0;bool bandWasOn=false;
    std::vector<float> localPCM;std::atomic<double> localDurationSeconds{0};int localRate=48000;std::atomic<bool> localPlaying{false};
    std::atomic<uint64_t> localCursor{0};std::atomic<bool> localBusy{false};
    Engine(){float defaults[]={1,.55,.27,2.2,.14,280,.25,0,0,1,1,0,0,0,0,78,0,0,0,1};for(int i=0;i<20;i++)params[i]=defaults[i];}
    ~Engine(){stop();}
    static aaudio_data_callback_result_t data(AAudioStream*,void* user,void* buffer,int32_t n){return static_cast<Engine*>(user)->render(static_cast<float*>(buffer),n);}
    static void error(AAudioStream*,void* user,aaudio_result_t e){auto* self=static_cast<Engine*>(user);self->params[11]=0;self->fault=e;self->running=false;}
    bool start(int device,int outputDevice,int mode){
        if(output)stop();guideFrequency=0;guideEnvelope=0;pitchConfidence=0;pitchLevel=0;playback.reset();params[11]=0;monitorMode=mode;monitorLevel=mode>0?.18f:1.f;fault=0;frequency=0;frames=0;transport=0;pitchRing.reset();
        {std::lock_guard<std::mutex> lock(notesMutex);notes.clear();hasActive=false;candidate=-1;candidateCount=0;}
        AAudioStreamBuilder* b=nullptr;AAudio_createStreamBuilder(&b);
        AAudioStreamBuilder_setDirection(b,AAUDIO_DIRECTION_OUTPUT);
        AAudioStreamBuilder_setFormat(b,AAUDIO_FORMAT_PCM_FLOAT);AAudioStreamBuilder_setChannelCount(b,2);
        if(outputDevice>0)AAudioStreamBuilder_setDeviceId(b,outputDevice);
        AAudioStreamBuilder_setUsage(b,mode==1?AAUDIO_USAGE_VOICE_COMMUNICATION:AAUDIO_USAGE_MEDIA);AAudioStreamBuilder_setContentType(b,mode==1?AAUDIO_CONTENT_TYPE_SPEECH:AAUDIO_CONTENT_TYPE_MUSIC);
        AAudioStreamBuilder_setPerformanceMode(b,AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
        AAudioStreamBuilder_setSharingMode(b,mode==2?AAUDIO_SHARING_MODE_SHARED:AAUDIO_SHARING_MODE_EXCLUSIVE);
        AAudioStreamBuilder_setDataCallback(b,data,this);AAudioStreamBuilder_setErrorCallback(b,error,this);
        auto result=AAudioStreamBuilder_openStream(b,&output);
        if(result!=AAUDIO_OK){AAudioStreamBuilder_setSharingMode(b,AAUDIO_SHARING_MODE_SHARED);result=AAudioStreamBuilder_openStream(b,&output);}
        AAudioStreamBuilder_delete(b);
        if(result!=AAUDIO_OK){fault=result;output=nullptr;return false;}
        if(outputDevice>0&&AAudioStream_getDeviceId(output)!=outputDevice){AAudioStream_close(output);output=nullptr;fault=AAUDIO_ERROR_UNAVAILABLE;return false;}
        if(AAudioStream_getFormat(output)!=AAUDIO_FORMAT_PCM_FLOAT||AAudioStream_getChannelCount(output)!=2){AAudioStream_close(output);output=nullptr;fault=AAUDIO_ERROR_INVALID_FORMAT;return false;}
        sr=AAudioStream_getSampleRate(output);
        AAudioStream_setBufferSizeInFrames(output,std::min(AAudioStream_getBufferCapacityInFrames(output),AAudioStream_getFramesPerBurst(output)*requestedBursts.load()));
        AAudio_createStreamBuilder(&b);AAudioStreamBuilder_setDirection(b,AAUDIO_DIRECTION_INPUT);
        AAudioStreamBuilder_setFormat(b,AAUDIO_FORMAT_PCM_FLOAT);AAudioStreamBuilder_setChannelCount(b,1);
        AAudioStreamBuilder_setSampleRate(b,sr);AAudioStreamBuilder_setPerformanceMode(b,mode>0?AAUDIO_PERFORMANCE_MODE_NONE:AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
        AAudioStreamBuilder_setInputPreset(b,mode>0?AAUDIO_INPUT_PRESET_VOICE_COMMUNICATION:AAUDIO_INPUT_PRESET_VOICE_RECOGNITION);
        if(mode>0)AAudioStreamBuilder_setSessionId(b,AAUDIO_SESSION_ID_ALLOCATE);
        if(device>0)AAudioStreamBuilder_setDeviceId(b,device);
        AAudioStreamBuilder_setErrorCallback(b,error,this);result=AAudioStreamBuilder_openStream(b,&input);AAudioStreamBuilder_delete(b);
        if(result!=AAUDIO_OK){fault=result;AAudioStream_close(output);output=nullptr;input=nullptr;return false;}
        if(mode>0&&device>0&&AAudioStream_getDeviceId(input)!=device){AAudioStream_close(input);input=nullptr;AAudioStream_close(output);output=nullptr;fault=AAUDIO_ERROR_UNAVAILABLE;return false;}
        if(AAudioStream_getFormat(input)!=AAUDIO_FORMAT_PCM_FLOAT||AAudioStream_getChannelCount(input)!=1||AAudioStream_getSampleRate(input)!=sr){AAudioStream_close(input);input=nullptr;AAudioStream_close(output);output=nullptr;fault=AAUDIO_ERROR_INVALID_FORMAT;return false;}
        dsp.init(sr);feedback.init(sr,mode>0);running=true;
        result=AAudioStream_requestStart(input);if(result==AAUDIO_OK)result=AAudioStream_requestStart(output);
        if(result!=AAUDIO_OK){fault=result;stop();return false;}
        analyzer=std::thread([this]{analyze();});return true;
    }
    void stop(){guideFrequency=0;running=false;stopRecord();
        if(output){AAudioStream_requestStop(output);AAudioStream_close(output);output=nullptr;}
        if(input){AAudioStream_requestStop(input);AAudioStream_close(input);input=nullptr;}
        if(analyzer.joinable())analyzer.join();frequency=0;inRms=0;outRms=0;params[11]=0;localPlaying=false;
    }
    float noise(){noiseState^=noiseState<<13;noiseState^=noiseState>>17;noiseState^=noiseState<<5;return int32_t(noiseState)/2147483648.f;}
    float band(const float* p,uint64_t sampleFrame){
        bool on=p[13]>.5f;if(on&&!bandWasOn)transport=0;bandWasOn=on;
        if(!on&&p[17]<.5f)return 0;
        double b=transport,phase=b-std::floor(b);transport+=p[15]/(60.*sr);beat=b;
        float sample=0;
        if(on){int style=int(p[14]);int chords[4][4]={{0,9,5,7},{0,9,2,7},{0,7,9,5},{9,5,0,7}};
            int bar=int(b/4)%4,root=48+chords[style%4][bar]+int(p[16]);bool minor=(root-int(p[16]))%12==9||(root-int(p[16]))%12==2;
            double sec=double(sampleFrame)/sr;float within=std::fmod(b,4.);
            float env=std::exp(-within*(style==2?1.7f:.35f));int intervals[]={0,minor?3:4,7,11};
            for(int v=0;v<(style==1?4:3);v++){float f=hz(root+12+intervals[v]);float arpeggio=style==2?std::exp(-std::fmod(b+v*.5,2.)*4.):env;
                sample+=(std::sin(2*PI*f*sec)+.16*std::sin(4*PI*f*sec))*.055f*arpeggio;}
            sample+=std::sin(2*PI*hz(root-12)*sec)*.10f*std::exp(-phase*3.f);
            if(style!=2||int(b)%4==0){float t=phase*60.f/p[15];sample+=std::sin(2*PI*(48*t+5*(1-std::exp(-t*25))))*.27*std::exp(-t*20);}
            if(int(b)%2==1)sample+=noise()*.11f*std::exp(-phase*18);
            double hat=std::fmod(b*(style==3?4:2),1.);sample+=noise()*.035f*std::exp(-hat*25);
        }
        if(p[17]>.5f){float t=phase*60.f/p[15];sample+=std::sin(2*PI*(int(b)%4==0?1500:1000)*t)*.12f*std::exp(-t*90);}
        return sample*p[1];
    }
    aaudio_data_callback_result_t render(float* out,int n){
        if(!running){std::fill(out,out+n*2,0);return AAUDIO_CALLBACK_RESULT_CONTINUE;}
        // Bluetooth and non-fast paths may request larger callback blocks.
        if(n>4096){for(int offset=0;offset<n;offset+=4096)render(out+offset*2,std::min(4096,n-offset));return AAUDIO_CALLBACK_RESULT_CONTINUE;}
        recordWriters.fetch_add(1);
        int got=AAudioStream_read(input,inputBuffer.data(),n,0);if(got<0){fault=got;params[11]=0;running=false;got=0;}
        std::fill(inputBuffer.begin()+got,inputBuffer.begin()+n,0);
        pitchRing.push(inputBuffer.data(),n);float p[20];for(int i=0;i<20;i++)p[i]=params[i].load(std::memory_order_relaxed);dsp.configure(p);feedback.prepare(sr);
        float inE=0,outE=0;uint64_t base=frames.load(std::memory_order_relaxed);
        bool local=localPlaying.load(std::memory_order_acquire);localBusy=true;
        // Recheck after entering the reader section so PCM replacement can wait safely.
        local=local&&localPlaying.load(std::memory_order_acquire);uint64_t cursor=localCursor;
        playback.read(capturedBuffer.data(),n*2);
        for(int i=0;i<n;i++){
            float l,r;dsp.tick(inputBuffer[i],p,l,r);float music=band(p,base+i),ml=music,mr=music;
            if(local){double pos=double(cursor)*localRate/sr;size_t index=size_t(pos);float mix=pos-index;
                if((index+1)*2+1<localPCM.size()){ml+=(localPCM[index*2]*(1-mix)+localPCM[(index+1)*2]*mix)*p[1];mr+=(localPCM[index*2+1]*(1-mix)+localPCM[(index+1)*2+1]*mix)*p[1];cursor++;}
                else {local=false;localPlaying=false;}}
            float synth=0,f=frequency.load(std::memory_order_relaxed);if(p[12]>.5&&f>0)synth=std::sin(2*PI*hz(int(std::round(69+12*std::log2(f/440))))*double(base+i)/sr)*.07f;
            float vl=l+synth,vr=r+synth,bl=ml+capturedBuffer[i*2],br=mr+capturedBuffer[i*2+1];
            if(recording&&!recordPaused)alignment.tick(vl,vr,bl,br);
            float recL=limit(vl+bl),recR=limit(vr+br);
            float liveL=(l+synth)*monitorLevel.load(std::memory_order_relaxed),liveR=(r+synth)*monitorLevel.load(std::memory_order_relaxed);feedback.process(liveL,liveR);
            float gf=guideFrequency.load();if(gf>0)lastGuideFrequency=gf;guideEnvelope+=((gf>0?1.f:0.f)-guideEnvelope)*.002f;float guide=.07f*guideEnvelope*(std::sin(2*PI*lastGuideFrequency*(base+i)/sr)+.22f*std::sin(4*PI*lastGuideFrequency*(base+i)/sr));
            out[i*2]=limit(ml+liveL*p[11]+guide);out[i*2+1]=limit(mr+liveR*p[11]+guide);
            recordBuffer[i*2]=recL;recordBuffer[i*2+1]=recR;inE+=inputBuffer[i]*inputBuffer[i];outE+=out[i*2]*out[i*2];
        }
        localCursor=cursor;localBusy=false;frames.fetch_add(n,std::memory_order_relaxed);inRms=std::sqrt(inE/n);outRms=std::sqrt(outE/n);
        if(recording.load(std::memory_order_acquire)&&!recordPaused.load(std::memory_order_acquire)){
            if(segmentPending.exchange(false))segmentSourceStart=base;
            if(recordFrames.load()==0)recordStartClock=monoSeconds();
            size_t written=recordRing.push(recordBuffer.data(),n*2);if(written!=size_t(n*2))overflow=true;recordFrames.fetch_add(n);
        }
        recordWriters.fetch_sub(1);return AAUDIO_CALLBACK_RESULT_CONTINUE;
    }
    void finalize(double end){if(hasActive){active.end=std::max(active.start,end);if(active.end-active.start>=.06)notes.push_back(active);hasActive=false;if(notes.size()>20000)notes.erase(notes.begin(),notes.begin()+1000);}}
    void analyze(){std::array<float,2048> window{};std::array<float,4096> fresh{};size_t filled=0;uint64_t lastAnalyzed=0;int feedbackTick=0;
        while(running){size_t n=pitchRing.pop(fresh.data(),fresh.size());
            if(n){if(n>=window.size()){std::copy(fresh.begin()+n-window.size(),fresh.begin()+n,window.begin());filled=window.size();}
                else {std::move(window.begin()+n,window.end(),window.begin());std::copy(fresh.begin(),fresh.begin()+n,window.end()-n);filled=std::min(window.size(),filled+n);}}
            uint64_t nowFrame=frames.load();
            if(filled==window.size()&&nowFrame-lastAnalyzed>=uint64_t(sr/50)){
                lastAnalyzed=nowFrame;if(++feedbackTick%5==0&&params[11]>.5f&&monitorLevel>0)feedback.analyze(window.data(),window.size(),sr,double(nowFrame)/sr);Pitch pitch=detectPitch(window.data(),window.size(),sr);frequency=pitch.frequency;pitchConfidence=pitch.confidence;pitchLevel=pitch.rms;
                double t=std::max(0.,double(nowFrame)/sr-.022);
                std::lock_guard<std::mutex> lock(notesMutex);
                if(pitch.frequency>0){int midi=std::lround(69+12*std::log2(pitch.frequency/440));
                    if(candidate!=midi){candidate=midi;candidateCount=1;candidateTime=t;}else candidateCount++;
                    if(hasActive&&active.midi==midi){lastSeen=t;active.end=t;}
                    else if(candidateCount>=3){finalize(candidateTime);active={midi,std::clamp(int(pitch.rms*350)+45,30,127),candidateTime,t};hasActive=true;lastSeen=t;}
                } else {candidate=-1;candidateCount=0;if(hasActive&&t-lastSeen>.09)finalize(lastSeen+.025);}
            }
            std::this_thread::sleep_for(std::chrono::milliseconds(8));
        }
        std::lock_guard<std::mutex> lock(notesMutex);finalize(double(frames.load())/sr);
    }
    std::vector<Note> snapshot(){std::lock_guard<std::mutex> lock(notesMutex);auto out=notes;if(hasActive&&active.end-active.start>=.06)out.push_back(active);return out;}
    void clearNotes(){std::lock_guard<std::mutex> lock(notesMutex);notes.clear();hasActive=false;candidate=-1;candidateCount=0;}
    static void le16(FILE* f,uint16_t v){uint8_t b[]={uint8_t(v),uint8_t(v>>8)};fwrite(b,1,2,f);}
    static void le32(FILE* f,uint32_t v){uint8_t b[]={uint8_t(v),uint8_t(v>>8),uint8_t(v>>16),uint8_t(v>>24)};fwrite(b,1,4,f);}
    void wavHeader(FILE* f,uint32_t bytes){fseek(f,0,SEEK_SET);fwrite("RIFF",1,4,f);le32(f,bytes+36);fwrite("WAVEfmt ",1,8,f);le32(f,16);le16(f,1);le16(f,2);le32(f,sr);le32(f,sr*4);le16(f,4);le16(f,16);fwrite("data",1,4,f);le32(f,bytes);}
    bool beginRecord(const std::string& path){if(!running||recording)return false;FILE* f=fopen(path.c_str(),"wb");if(!f)return false;
        alignment.init(sr,recordSyncMs);wavHeader(f,0);recordRing.reset();recordPaused=false;segmentPending=true;segmentOutputStart=0;recordedSpans.clear();recordFrames=0;recordStartClock=0;overflow=false;diskError=false;recordStartTime=double(frames.load())/sr;
        writerRunning=true;recording=true;writer=std::thread([this,f]{std::array<float,8192> data{};std::array<int16_t,8192> pcm{};uint32_t bytes=0;
            while(writerRunning||recordRing.available()) {size_t n=recordRing.pop(data.data(),data.size());if(n){for(size_t i=0;i<n;i++)pcm[i]=std::lround(limit(data[i])*32767);
                    size_t written=fwrite(pcm.data(),2,n,f);if(written!=n)diskError=true;bytes+=written*2;}else std::this_thread::sleep_for(std::chrono::milliseconds(4));}
            wavHeader(f,bytes);if(fclose(f)!=0)diskError=true;
        });return true;
    }
    void closeRecordedSpan(){uint64_t count=recordFrames.load()-segmentOutputStart;if(count)recordedSpans.push_back({double(segmentSourceStart.load())/sr,double(count)/sr});segmentOutputStart=recordFrames.load();}
    void pauseRecord(bool pause){
        if(!recording||recordPaused.load()==pause)return;
        if(pause){recordPaused=true;while(recordWriters.load()>0)std::this_thread::yield();closeRecordedSpan();}
        else {segmentPending=true;recordPaused=false;}
    }
    void stopRecord(){bool wasRecording=recording.exchange(false);while(recordWriters.load()>0)std::this_thread::yield();if(wasRecording&&!recordPaused)closeRecordedSpan();recordPaused=false;
        if(wasRecording){for(int i=0;i<alignment.tail();i++){float vl=0,vr=0,ml=0,mr=0;alignment.tick(vl,vr,ml,mr);float pair[]={limit(vl+ml),limit(vr+mr)};while(recordRing.push(pair,2)!=2)std::this_thread::sleep_for(std::chrono::milliseconds(1));recordFrames++;}}
        writerRunning=false;if(writer.joinable())writer.join();}
    bool exportMidi(const std::string& path,int bpm,bool onlyRecord){auto data=snapshot();if(onlyRecord){data=recordedNotes(data,recordedSpans);for(auto& n:data){n.start+=double(alignment.vocalFrames)/sr;n.end+=double(alignment.vocalFrames)/sr;}}
        auto bytes=midiFile(data,bpm);FILE* f=fopen(path.c_str(),"wb");if(!f)return false;bool ok=fwrite(bytes.data(),1,bytes.size(),f)==bytes.size();fclose(f);return ok;}
};
static Engine engine;
static std::string str(JNIEnv* env,jstring s){const char* p=env->GetStringUTFChars(s,nullptr);std::string out(p);env->ReleaseStringUTFChars(s,p);return out;}
#define JNI(name) extern "C" JNIEXPORT
JNI(jboolean) jboolean JNICALL Java_pro_livemic_studio_NativeAudio_start(JNIEnv*,jclass,jint d,jint out,jint mode){return engine.start(d,out,mode);}
JNI(void) void JNICALL Java_pro_livemic_studio_NativeAudio_stop(JNIEnv*,jclass){engine.stop();}
JNI(void) void JNICALL Java_pro_livemic_studio_NativeAudio_set(JNIEnv*,jclass,jint k,jfloat v){if(k>=0&&k<20&&std::isfinite(v))engine.params[k]=v;}
JNI(jfloatArray) jfloatArray JNICALL Java_pro_livemic_studio_NativeAudio_stats(JNIEnv* e,jclass){float f=engine.frequency;float mf=f>0?69+12*std::log2(f/440):0;
    float values[]={f,f>0?(mf-std::round(mf))*100:0,f>0?std::round(mf):-1,engine.inRms,engine.outRms,engine.beat,float(engine.sr),
        engine.output?1000.f*AAudioStream_getBufferSizeInFrames(engine.output)/engine.sr:0,float(double(engine.recordFrames.load())/engine.sr),
        engine.output?float(AAudioStream_getXRunCount(engine.output)):0,engine.running?1.f:0,engine.recording?1.f:0,float(engine.snapshot().size()),engine.overflow||engine.diskError?1.f:0,float(engine.fault),float(double(engine.frames.load())/engine.sr),engine.localPlaying?1.f:0,engine.feedback.muted()?1.f:0,float(engine.feedback.count()),float(engine.monitorMode),engine.monitorLevel,engine.params[11]};
    auto out=e->NewFloatArray(22);e->SetFloatArrayRegion(out,0,22,values);return out;}
JNI(jfloatArray) jfloatArray JNICALL Java_pro_livemic_studio_NativeAudio_roll(JNIEnv* e,jclass){auto data=engine.snapshot();if(data.size()>240)data.erase(data.begin(),data.end()-240);
    std::vector<float> values;for(auto n:data){values.push_back(n.start);values.push_back(n.end-n.start);values.push_back(n.midi);}
    auto out=e->NewFloatArray(values.size());if(!values.empty())e->SetFloatArrayRegion(out,0,values.size(),values.data());return out;}
JNI(jboolean) jboolean JNICALL Java_pro_livemic_studio_NativeAudio_beginRecord(JNIEnv* e,jclass,jstring p){return engine.beginRecord(str(e,p));}
JNI(void) void JNICALL Java_pro_livemic_studio_NativeAudio_pauseRecord(JNIEnv*,jclass,jboolean pause){engine.pauseRecord(pause);}
JNI(void) void JNICALL Java_pro_livemic_studio_NativeAudio_endRecord(JNIEnv*,jclass){engine.stopRecord();}
JNI(jboolean) jboolean JNICALL Java_pro_livemic_studio_NativeAudio_exportMidi(JNIEnv* e,jclass,jstring p,jint bpm,jboolean rec){return engine.exportMidi(str(e,p),bpm,rec);}
JNI(void) void JNICALL Java_pro_livemic_studio_NativeAudio_clearNotes(JNIEnv*,jclass){engine.clearNotes();}
JNI(jdouble) jdouble JNICALL Java_pro_livemic_studio_NativeAudio_recordClock(JNIEnv*,jclass){return engine.recordStartClock;}
JNI(void) void JNICALL Java_pro_livemic_studio_NativeAudio_loadLocal(JNIEnv* e,jclass,jfloatArray pcm,jint rate){engine.localPlaying=false;while(engine.localBusy.load())std::this_thread::yield();
    int n=e->GetArrayLength(pcm);engine.localPCM.resize(n);e->GetFloatArrayRegion(pcm,0,n,engine.localPCM.data());engine.localRate=rate;engine.localDurationSeconds=double(n)/(2*rate);engine.localCursor=0;}
JNI(void) void JNICALL Java_pro_livemic_studio_NativeAudio_playLocal(JNIEnv*,jclass,jboolean play){if(play&&!engine.localPCM.empty()){if(double(engine.localCursor.load())*engine.localRate/engine.sr>=engine.localPCM.size()/2-1)engine.localCursor=0;engine.params[13]=0;engine.localPlaying=true;}else engine.localPlaying=false;}
JNI(jboolean) jboolean JNICALL Java_pro_livemic_studio_NativeAudio_loadLocalFile(JNIEnv* e,jclass,jstring path,jint rate,jint channels){
    if(rate<8000||rate>192000||channels<1||channels>2)return false;
    engine.localPlaying=false;while(engine.localBusy.load())std::this_thread::yield();
    FILE* f=fopen(str(e,path).c_str(),"rb");if(!f)return false;fseek(f,0,SEEK_END);long length=ftell(f);rewind(f);
    if(length<=0||length>long(rate)*channels*2*8*60){fclose(f);return false;}
    size_t count=length/(channels*2);try{engine.localPCM.resize(count*2);}catch(...){fclose(f);return false;}
    std::array<int16_t,4096> buffer{};size_t pos=0;while(pos<count){size_t n=fread(buffer.data(),channels*2,std::min(size_t(4096/channels),count-pos),f);if(!n)break;
        for(size_t i=0;i<n;i++){engine.localPCM[(pos+i)*2]=buffer[i*channels]/32768.f;engine.localPCM[(pos+i)*2+1]=buffer[i*channels+(channels==2?1:0)]/32768.f;}pos+=n;}
    fclose(f);engine.localPCM.resize(pos*2);engine.localRate=rate;engine.localDurationSeconds=double(pos)/rate;engine.localCursor=0;return pos>0;
}

JNI(void) void JNICALL Java_pro_livemic_studio_NativeAudio_enablePlaybackCapture(JNIEnv*,jclass,jboolean enable,jfloat gain){engine.playback.configure(enable,gain);}
JNI(jint) jint JNICALL Java_pro_livemic_studio_NativeAudio_pushPlayback(JNIEnv* e,jclass,jfloatArray data,jint count){
    if(count<=0)return 0;count=std::min(count,e->GetArrayLength(data));count=std::min(count,8192);count-=count%2;
    std::array<float,8192> buffer{};e->GetFloatArrayRegion(data,0,count,buffer.data());
    size_t n=engine.playback.push(buffer.data(),count);if(n<size_t(count)&&engine.recording)engine.overflow=true;return n;
}

JNI(jint) jint JNICALL Java_pro_livemic_studio_NativeAudio_inputSession(JNIEnv*,jclass){return engine.input?AAudioStream_getSessionId(engine.input):0;}
JNI(void) void JNICALL Java_pro_livemic_studio_NativeAudio_monitorLevel(JNIEnv*,jclass,jfloat level){engine.monitorLevel=std::clamp(level,0.f,engine.monitorMode>0?.65f:1.f);}
JNI(void) void JNICALL Java_pro_livemic_studio_NativeAudio_resetFeedback(JNIEnv*,jclass){engine.monitorLevel=engine.monitorMode>0?.18f:1.f;engine.feedback.reset();}

JNI(jintArray) jintArray JNICALL Java_pro_livemic_studio_NativeAudio_latencyInfo(JNIEnv* e,jclass){int v[]={engine.output?AAudioStream_getFramesPerBurst(engine.output):0,engine.output?AAudioStream_getBufferCapacityInFrames(engine.output):0,engine.output?AAudioStream_getBufferSizeInFrames(engine.output):0,engine.sr,engine.output?AAudioStream_getXRunCount(engine.output):0};auto a=e->NewIntArray(5);e->SetIntArrayRegion(a,0,5,v);return a;}
JNI(jboolean) jboolean JNICALL Java_pro_livemic_studio_NativeAudio_bufferBursts(JNIEnv*,jclass,jint bursts){bursts=std::clamp(int(bursts),1,8);if(engine.recording)return false;if(engine.output){int n=std::min(AAudioStream_getBufferCapacityInFrames(engine.output),AAudioStream_getFramesPerBurst(engine.output)*bursts);if(AAudioStream_setBufferSizeInFrames(engine.output,n)<0)return false;}engine.requestedBursts=bursts;return true;}
JNI(void) void JNICALL Java_pro_livemic_studio_NativeAudio_recordSync(JNIEnv*,jclass,jint ms){if(!engine.recording)engine.recordSyncMs=std::clamp(int(ms),-500,500);}
JNI(void) void JNICALL Java_pro_livemic_studio_NativeAudio_guideTone(JNIEnv*,jclass,jfloat f){engine.guideFrequency=std::isfinite(f)&&f>=75&&f<=1100?f:0;}
JNI(jfloatArray) jfloatArray JNICALL Java_pro_livemic_studio_NativeAudio_pitchFrame(JNIEnv* e,jclass){float v[]={engine.frequency,engine.pitchConfidence,engine.pitchLevel,float(double(engine.frames.load())/engine.sr)};auto a=e->NewFloatArray(4);e->SetFloatArrayRegion(a,0,4,v);return a;}

JNI(jdoubleArray) jdoubleArray JNICALL Java_pro_livemic_studio_NativeAudio_localPlaybackInfo(JNIEnv* e,jclass){double values[]={double(engine.localCursor.load())/engine.sr,engine.localDurationSeconds.load()};auto a=e->NewDoubleArray(2);e->SetDoubleArrayRegion(a,0,2,values);return a;}
