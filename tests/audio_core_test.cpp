#include "../app/src/main/cpp/audio_core.h"
#include "../app/src/main/cpp/feedback_guard.h"
#include <cassert>
#include <iostream>
using namespace lm;
int main(){
    for(int sr:{44100,48000})for(float frequency:{82.4069f,130.8128f,220.f,440.f,880.f,1046.502f}){
        float data[2048];for(int i=0;i<2048;i++)data[i]=.3f*std::sin(2*PI*frequency*i/sr)+.07f*std::sin(4*PI*frequency*i/sr);
        auto p=detectPitch(data,2048,sr);double cents=1200*std::log2(p.frequency/frequency);assert(p.frequency>0&&std::abs(cents)<8);}
    float silence[2048]={};assert(detectPitch(silence,2048,48000).frequency==0);
    auto midi=midiFile({{60,100,0,.5},{64,80,.5,1}},120);assert(std::string(midi.begin(),midi.begin()+4)=="MThd");assert(midi[12]==1&&midi[13]==224);
    uint32_t len=(uint32_t(midi[18])<<24)|(uint32_t(midi[19])<<16)|(uint32_t(midi[20])<<8)|midi[21];assert(len+22==midi.size());
    assert(midi[midi.size()-3]==255&&midi[midi.size()-2]==47&&midi.back()==0);
    Ring<8> ring;float input[]={1,2,3,4,5,6},out[8];assert(ring.push(input,6)==6);assert(ring.pop(out,4)==4);assert(out[0]==1&&out[3]==4);assert(ring.push(input,6)==6);assert(ring.push(input,1)==0);assert(ring.pop(out,8)==8);assert(out[0]==5&&out[1]==6&&out[7]==6);
    DSP dsp;dsp.init(48000);float params[]={1,.55,.4,2.2,.2,280,.25,0,0,1,1,0,0,0,0,78,0,0,0,1};dsp.configure(params);double tail=0;
    for(int i=0;i<48000*3;i++){float l,r;dsp.tick(i==0?.7f:0,params,l,r);assert(std::isfinite(l)&&std::isfinite(r));assert(std::abs(l)<2&&std::abs(r)<2);if(i>4800&&i<24000)tail+=l*l+r*r;}
    assert(tail>.00001);std::cout<<"PASS: pitch at 44.1/48 kHz, silence gate, SMF structure, SPSC wrapping, stable DSP/reverb tail\n";
    FeedbackGuard guard;guard.init(48000,true);float wave[2048];
    for(int k=0;k<8;k++)guard.analyze(silence,2048,48000,k*.1);
    assert(!guard.muted()&&guard.count()==0);
    float amplitudes[]={.05f,.075f,.12f,.19f,.3f};
    for(int k=0;k<5;k++){for(int i=0;i<2048;i++)wave[i]=amplitudes[k]*std::sin(2*PI*1500*i/48000);guard.analyze(wave,2048,48000,1+k*.1);}
    assert(guard.count()>=1&&!guard.muted());guard.prepare(48000);double filtered=0,original=0;
    for(int i=0;i<24000;i++){float x=.3f*std::sin(2*PI*1500*i/48000),l=x,r=x;guard.process(l,r);assert(std::isfinite(l));if(i>12000){filtered+=l*l;original+=x*x;}}
    assert(filtered<original*.4);
    for(int i=0;i<2048;i++)wave[i]=std::sin(2*PI*1500*i/48000);
    guard.analyze(wave,2048,48000,5);guard.analyze(wave,2048,48000,5.1);assert(guard.muted());
    float l=.4f,r=.4f;guard.process(l,r);assert(l==0&&r==0);
    guard.reset();guard.analyze(silence,2048,48000,6);assert(!guard.muted()&&guard.count()==0);
    std::cout<<"PASS: silence stability, growing-tone suppression, overload mute and explicit reset\n";
    PlaybackMixer playback;float stereo[]={.4f,-.2f,.8f,-.6f},mixed[6];
    assert(playback.push(stereo,4)==0);
    playback.configure(true,.5f);assert(playback.push(stereo,4)==4);playback.read(mixed,6);
    assert(std::abs(mixed[0]-.2f)<1e-6&&std::abs(mixed[1]+.1f)<1e-6&&std::abs(mixed[2]-.4f)<1e-6);
    assert(mixed[4]==0&&mixed[5]==0);
    assert(playback.push(stereo,4)==4);playback.configure(false,1);playback.read(mixed,4);
    for(float v:mixed)assert(v==0);
    playback.configure(true,1);playback.read(mixed,6);for(float v:mixed)assert(v==0);
    assert(playback.push(stereo,4)==4);playback.reset();playback.configure(true,1);playback.read(mixed,6);for(float v:mixed)assert(v==0);
    std::cout<<"PASS: captured stereo gain, underflow silence, disabled capture drain, fresh-session isolation\n";
}
