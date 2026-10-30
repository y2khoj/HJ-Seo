#pragma once
#include <algorithm>
#include <array>
#include <atomic>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <limits>
#include <thread>
#include <vector>

namespace vh {
// Single audio callback producer. Control methods may be called from other
// threads, but prepare/load/sampleRate are serialized by the caller.
class Looper {
    std::array<std::vector<float>,5> pcm;
    std::atomic<int> selected[4]{{0},{1},{2},{3}};
    int pending=4;
    std::atomic<bool> exclusive{false};
    std::atomic<unsigned> callbackReaders{0},snapshotReaders{0};
    std::atomic_flag swapGuard = ATOMIC_FLAG_INIT;
    std::atomic<int> mask{0},playing{0},armed{-1},recording{-1},revision{0},last{-1},error{0};
    std::atomic<uint64_t> position{0},remaining{0};
    std::atomic<unsigned> cancelSerial{0};
    unsigned recordingSerial=0;
    std::atomic<float> volume[4]{{1},{1},{1},{1}},pan[4]{{0},{0},{0},{0}};
    std::atomic<bool> muted[4]{{false},{false},{false},{false}};
    float smoothL[4]{1,1,1,1},smoothR[4]{1,1,1,1};
    int rate=48000,bpm=90,meter=0,bars=4;
    uint64_t length=0,barFrames=0,writePosition=0;
    bool recordingDone=false;
    void lockControl(){exclusive.store(true);while(callbackReaders.load()||snapshotReaders.load())std::this_thread::yield();}
    void unlockControl(){exclusive.store(false);}
    bool enterCallback(){callbackReaders.fetch_add(1);if(exclusive.load()){callbackReaders.fetch_sub(1);return false;}return true;}
    void leaveCallback(){callbackReaders.fetch_sub(1);}
    bool enterSnapshot(){snapshotReaders.fetch_add(1);if(exclusive.load()){snapshotReaders.fetch_sub(1);return false;}return true;}
    void leaveSnapshot(){snapshotReaders.fetch_sub(1);}
    static uint16_t u16(const unsigned char* p){return uint16_t(p[0])|(uint16_t(p[1])<<8);}
    static uint32_t u32(const unsigned char* p){return uint32_t(u16(p))|(uint32_t(u16(p+2))<<16);}
    static void put16(FILE* f,uint16_t n){unsigned char b[2]={uint8_t(n),uint8_t(n>>8)};std::fwrite(b,1,2,f);}
    static void put32(FILE* f,uint32_t n){put16(f,uint16_t(n));put16(f,uint16_t(n>>16));}
    int startRecording(int slot){
        unsigned serial=cancelSerial.load();
        if(slot<0||!armed.compare_exchange_strong(slot,-1))return -1;
        recordingSerial=serial;writePosition=0;recordingDone=false;recording=slot;
        if(cancelSerial.load()!=serial){int expected=slot;recording.compare_exchange_strong(expected,-1);return -1;}
        return slot;
    }
    void geometry(){double beats=meter==0?4.:meter==1?3.:2.;barFrames=uint64_t(std::llround(rate*60.*beats/bpm));length=barFrames*bars;}
public:
    bool prepare(int tempo,int met,int count){
        if(tempo<40||tempo>240||met<0||met>2||(count!=4&&count!=8))return false;
        lockControl();
        if(playing.load()||armed.load()!=-1||recording.load()!=-1){unlockControl();return false;}
        if(mask.load()&&(tempo!=bpm||met!=meter||count!=bars)){unlockControl();return false;}
        if(length&&tempo==bpm&&met==meter&&count==bars){unlockControl();return true;}
        int oldBpm=bpm,oldMeter=meter,oldBars=bars;uint64_t oldLength=length,oldBar=barFrames;
        bpm=tempo;meter=met;bars=count;geometry();
        try {std::array<std::vector<float>,5> fresh;for(auto& v:fresh)v.resize(size_t(length)*2);pcm.swap(fresh);}
        catch(...){bpm=oldBpm;meter=oldMeter;bars=oldBars;length=oldLength;barFrames=oldBar;error=1;unlockControl();return false;}
        for(int i=0;i<4;i++)selected[i]=i;
        pending=4;position=0;remaining=0;error=0;unlockControl();return true;
    }
    // Called before output callback starts. Preserve existing layers when the
    // device changes rate by resampling on the caller thread.
    bool sampleRate(int newRate){
        if(newRate<8000||newRate>192000)return false;
        lockControl();if(rate==newRate){unlockControl();return true;}
        if(armed.load()!=-1||recording.load()!=-1){unlockControl();return false;}if(!length){rate=newRate;unlockControl();return true;}
        int oldRate=rate;uint64_t oldLength=length,oldBar=barFrames;rate=newRate;geometry();
        try {std::array<std::vector<float>,5> fresh;for(auto& v:fresh)v.resize(size_t(length)*2);
            int active=mask.load();for(int s=0;s<4;s++)if(active&(1<<s)){const auto& src=pcm[selected[s].load()];auto& dst=fresh[s];for(uint64_t i=0;i<length;i++){double p=double(i)*oldRate/newRate;uint64_t a=std::min(uint64_t(p),oldLength-1),b=std::min(a+1,oldLength-1);float t=float(p-a);for(int c=0;c<2;c++)dst[i*2+c]=src[a*2+c]*(1-t)+src[b*2+c]*t;}}
            pcm.swap(fresh);for(int i=0;i<4;i++)selected[i]=i;pending=4;
        }catch(...){rate=oldRate;length=oldLength;barFrames=oldBar;error=1;unlockControl();return false;}
        position=std::min(uint64_t(std::llround(double(position.load())*newRate/oldRate)),length-1);remaining=0;unlockControl();return true;
    }
    void transport(int command){
        if(!enterSnapshot())return;
        if(command==0){cancel();playing=0;position=0;remaining=0;}
        else if(command==1){if(length)playing=1;}
        else if(command==2){cancel();playing=0;remaining=0;}
        leaveSnapshot();
    }
    bool arm(int slot){
        if(slot<0||slot>3||!enterSnapshot())return false;
        if(!length||recording.load()!=-1){leaveSnapshot();return false;}
        int expected=-1;bool ok=armed.compare_exchange_strong(expected,slot);
        leaveSnapshot();return ok;
    }
    void cancel(){cancelSerial.fetch_add(1);armed=-1;recording=-1;remaining=0;}
    void layer(int slot,float v,float p,bool mute){if(slot<0||slot>3)return;volume[slot]=std::isfinite(v)?std::clamp(v,0.f,1.f):0;pan[slot]=std::isfinite(p)?std::clamp(p,-1.f,1.f):0;muted[slot]=mute;}
    void clear(int slot){if(slot>=0&&slot<4)mask.fetch_and(~(1<<slot));}
    // One frame: current live vocal only is recorded; loop playback is returned separately.
    void tick(float liveL,float liveR,float& loopL,float& loopR,float* countClick=nullptr){
        loopL=loopR=0;if(countClick)*countClick=0;if(!enterCallback())return;
        if(!length){leaveCallback();return;}
        int a=armed.load(std::memory_order_acquire),r=recording.load(std::memory_order_acquire);
        uint64_t pos=position.load(std::memory_order_relaxed),left=remaining.load(std::memory_order_relaxed);
        if(a>=0&&r<0&&!playing.load()&&left==0){left=barFrames;remaining=left;position=0;pos=0;playing=1;}
        if(playing.load()&&left){
            if(countClick){
                const uint64_t elapsed=barFrames-left;
                const double beatFrames=rate*60./bpm;
                const int beat=int(std::floor(double(elapsed)/beatFrames+1e-9));
                const uint64_t start=uint64_t(std::llround(beat*beatFrames));
                const double t=double(elapsed-start)/rate;
                if(t>=0&&t<.04)*countClick=float(.07*std::exp(-80*t)*std::sin(6.283185307179586*(beat==0?1500:1000)*t));
            }
            --left;remaining=left;if(!left&&a>=0)r=startRecording(a);
        }
        else if(playing.load()){
            if(a>=0&&r<0&&pos==0)r=startRecording(a);
            int active=mask.load(std::memory_order_acquire);
            for(int i=0;i<4;i++){
                float v=muted[i].load()?0:volume[i].load(),p=pan[i].load();float gl=v*(p>0?1-p:1),gr=v*(p<0?1+p:1);
                smoothL[i]+=(gl-smoothL[i])*.002f;smoothR[i]+=(gr-smoothR[i])*.002f;
                if(active&(1<<i)){const auto& src=pcm[selected[i].load(std::memory_order_acquire)];loopL+=src[pos*2]*smoothL[i];loopR+=src[pos*2+1]*smoothR[i];}
            }
            if(r>=0&&recordingSerial==cancelSerial.load()&&!recordingDone){auto& dst=pcm[pending];dst[writePosition*2]=liveL;dst[writePosition*2+1]=liveR;if(++writePosition==length)recordingDone=true;}
            if(++pos==length)pos=0;
            position=pos;
        }
        if(recordingDone&&recording.load()>=0&&recordingSerial==cancelSerial.load()&&!swapGuard.test_and_set(std::memory_order_acquire)){
            int slot=recording.load();
            if(slot>=0&&recordingSerial==cancelSerial.load()&&recording.compare_exchange_strong(slot,-1)){
                int old=selected[slot].exchange(pending,std::memory_order_acq_rel);pending=old;mask.fetch_or(1<<slot);last=slot;revision.fetch_add(1,std::memory_order_release);
            }
            recordingDone=false;swapGuard.clear(std::memory_order_release);
        }
        leaveCallback();
    }
    bool saveLayer(int slot,const char* path){
        if(slot<0||slot>3||!path||!enterSnapshot())return false;
        while(swapGuard.test_and_set(std::memory_order_acquire))std::this_thread::yield();
        if(!(mask.load(std::memory_order_acquire)&(1<<slot))){swapGuard.clear(std::memory_order_release);leaveSnapshot();return false;}
        std::vector<float> copy;int fileRate=rate;
        try{copy=pcm[selected[slot].load(std::memory_order_acquire)];}catch(...){swapGuard.clear(std::memory_order_release);leaveSnapshot();return false;}swapGuard.clear(std::memory_order_release);leaveSnapshot();
        if(copy.size()>std::numeric_limits<uint32_t>::max()/2)return false;
        FILE* f=std::fopen(path,"wb");if(!f)return false;uint32_t bytes=uint32_t(copy.size()*2);
        std::fwrite("RIFF",1,4,f);put32(f,bytes+36);std::fwrite("WAVEfmt ",1,8,f);put32(f,16);put16(f,1);put16(f,2);put32(f,fileRate);put32(f,fileRate*4);put16(f,4);put16(f,16);std::fwrite("data",1,4,f);put32(f,bytes);
        std::array<int16_t,4096> block{};bool ok=!std::ferror(f);for(size_t i=0;i<copy.size()&&ok;){size_t n=std::min(block.size(),copy.size()-i);for(size_t j=0;j<n;j++)block[j]=int16_t(std::lround(std::clamp(copy[i+j],-1.f,1.f)*32767));ok=std::fwrite(block.data(),sizeof(int16_t),n,f)==n;i+=n;}if(std::fclose(f)!=0)ok=false;return ok;
    }
    bool loadLayer(int slot,const char* path){
        if(slot<0||slot>3||!path)return false;
        FILE* f=std::fopen(path,"rb");if(!f)return false;
        unsigned char head[12];if(std::fread(head,1,12,f)!=12||std::memcmp(head,"RIFF",4)||std::memcmp(head+8,"WAVE",4)){std::fclose(f);return false;}
        uint32_t sourceRate=0,dataBytes=0;long dataOffset=0;bool fmt=false;unsigned char chunk[8];
        while(std::fread(chunk,1,8,f)==8){uint32_t n=u32(chunk+4);long start=std::ftell(f);if(!std::memcmp(chunk,"fmt ",4)&&n>=16){unsigned char x[16];if(std::fread(x,1,16,f)!=16)break;sourceRate=u32(x+4);fmt=u16(x)==1&&u16(x+2)==2&&u16(x+12)==4&&u16(x+14)==16&&sourceRate>=8000&&sourceRate<=192000;}else if(!std::memcmp(chunk,"data",4)){dataBytes=n;dataOffset=start;}if(n>uint32_t(std::numeric_limits<long>::max()-start-1)||std::fseek(f,start+long(n)+(n&1),SEEK_SET))break;}
        if(!fmt||!dataOffset||dataBytes%4||!dataBytes||dataBytes>192000u*4u*60u||std::fseek(f,dataOffset,SEEK_SET)){std::fclose(f);return false;}
        std::vector<int16_t> source;try{source.resize(dataBytes/2);}catch(...){std::fclose(f);return false;}bool ok=std::fread(source.data(),1,dataBytes,f)==dataBytes;std::fclose(f);if(!ok)return false;
        lockControl();if(!length||playing.load()||armed.load()!=-1||recording.load()!=-1){unlockControl();return false;}
        uint64_t sourceFrames=source.size()/2,expected=uint64_t(std::llround(double(length)*sourceRate/rate));if(sourceFrames+1<expected||sourceFrames>expected+1){unlockControl();return false;}
        auto& dst=pcm[pending];for(uint64_t i=0;i<length;i++){double p=double(i)*sourceRate/rate;uint64_t a=std::min(uint64_t(p),sourceFrames-1),b=std::min(a+1,sourceFrames-1);float t=float(p-a);for(int c=0;c<2;c++)dst[i*2+c]=(source[a*2+c]*(1-t)+source[b*2+c]*t)/32768.f;}
        int old=selected[slot].exchange(pending);pending=old;mask.fetch_or(1<<slot);unlockControl();return true;
    }
    void state(double* out){while(!enterSnapshot())std::this_thread::yield();out[0]=playing.load();out[1]=double(position.load())/rate;out[2]=double(length)/rate;out[3]=double(remaining.load())/(rate*60./bpm);out[4]=armed.load();out[5]=recording.load();out[6]=revision.load();out[7]=mask.load();out[12]=error.load();out[13]=last.load();leaveSnapshot();}
};
}
