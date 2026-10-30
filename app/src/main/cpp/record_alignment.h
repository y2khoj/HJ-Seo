#pragma once
#include <vector>
#include <algorithm>
namespace lm {
// Two stereo delay lines used exclusively by the recording bus.
class RecordAlignment {
    std::vector<float> voice,music;size_t vp=0,mp=0;int remaining=0;
    static void delay(std::vector<float>& b,size_t& p,float& l,float& r){if(b.empty())return;float a=b[p],c=b[p+1];b[p]=l;b[p+1]=r;l=a;r=c;p=(p+2)%b.size();}
public:
    int vocalFrames=0,backingFrames=0;
    void init(int rate,int ms){ms=std::clamp(ms,-500,500);vocalFrames=std::max(0,ms)*rate/1000;backingFrames=std::max(0,-ms)*rate/1000;voice.assign(vocalFrames*2,0);music.assign(backingFrames*2,0);vp=mp=0;remaining=std::max(vocalFrames,backingFrames);}
    void tick(float& vl,float& vr,float& ml,float& mr){delay(voice,vp,vl,vr);delay(music,mp,ml,mr);}
    int tail()const{return remaining;}
};
}
