#include "../app/src/main/cpp/audio_core.h"
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
    // A note crossing a pause is split; notes inside the pause are omitted.
    auto retained=recordedNotes({{60,90,9,12},{64,90,12.5,13.5},{67,90,14,17}},{{10,2},{14,2}});
    assert(retained.size()==2);assert(retained[0].midi==60&&retained[0].start==0&&retained[0].end==2);
    assert(retained[1].midi==67&&retained[1].start==2&&retained[1].end==4);
    auto crossing=recordedNotes({{60,90,0,10}},{{1,2},{5,1},{8,0}});
    assert(crossing.size()==2&&crossing[1].start==2&&crossing[1].end==3);
    assert(recordedNotes({{60,90,0,1}},{}).empty());
    DSP dsp;dsp.init(48000);float params[]={1,.55,.4,2.2,.2,280,.25,0,0,1,1,0,0,0,0,78,0,0,0,1};dsp.configure(params);double tail=0;
    for(int i=0;i<48000*3;i++){float l,r;dsp.tick(i==0?.7f:0,params,l,r);assert(std::isfinite(l)&&std::isfinite(r));assert(std::abs(l)<2&&std::abs(r)<2);if(i>4800&&i<24000)tail+=l*l+r*r;}
    assert(tail>.00001);std::cout<<"PASS: pitch at 44.1/48 kHz, silence gate, SMF structure, SPSC wrapping, stable DSP/reverb tail, pause-compacted MIDI timeline\n";
}
