#pragma once
#include <algorithm>
#include <array>
#include <atomic>
#include <cmath>

// Two overlapping, variable-delay grains use the input signal itself. The
// delay modulation changes playback speed while the Hann windows hide resets.
namespace vh {
constexpr float pi = 3.14159265358979323846f;
class Grain {
    std::array<float,8192> history{};
    unsigned write=0; float phase=0, ratio=1;
public:
    void reset(){history.fill(0);write=0;phase=0;ratio=1;}
    float tick(float input,float target,int rate){
        history[write]=input;
        ratio += (target-ratio)*.001f;
        const float length=std::min(4000.f,rate*.045f);
        auto head=[&](float p){
            float delay=8.f+p*length;
            float read=float(write)-delay;
            while(read<0)read+=history.size();
            unsigned a=unsigned(read)%history.size(),b=(a+1)%history.size();
            float frac=read-std::floor(read);
            float window=.5f-.5f*std::cos(2*pi*p);
            return (history[a]*(1-frac)+history[b]*frac)*window;
        };
        float other=phase+.5f;if(other>=1)other-=1;
        float out=head(phase)+head(other);
        phase+=(1-ratio)/length;
        if(phase<0)phase+=1;
        if(phase>=1)phase-=1;
        write=(write+1)%history.size();return out;
    }
};
class Harmony {
    Grain voices[2];float gains[2]{0,0},ratios[2]{1,1};
    std::atomic<bool> on{false};std::atomic<int> key{0},scale{0},preset{0},anchor{-1};
    std::atomic<float> wet{.5f},dry{1},spread{.5f};
public:
    std::atomic<int> inputNote{-1},note1{-1},note2{-1};std::atomic<float> level{0};
    bool enabled() const{return on.load(std::memory_order_relaxed);}
    void configure(bool enabled,int k,int s,int p,float w,float d,float sp){
        key=std::clamp(k,0,11);scale=std::clamp(s,0,4);preset=std::clamp(p,0,3);
        wet=std::isfinite(w)?std::clamp(w,0.f,1.f):0;dry=std::isfinite(d)?std::clamp(d,0.f,1.f):1;spread=std::isfinite(sp)?std::clamp(sp,0.f,1.f):0;on=enabled;if(!enabled)anchor=-1;
    }
    void disable(){on=false;anchor=-1;}
    void clearPitch(){anchor=-1;inputNote=-1;note1=-1;note2=-1;level=0;}
    void tick(float l,float r,float frequency,float confidence,float rms,int rate,float& outL,float& outR){
        static constexpr int scales[5][7]={{0,2,4,5,7,9,11},{0,2,3,5,7,8,10},{0,2,3,5,7,8,11},{0,2,3,5,7,9,10},{0,2,4,5,7,9,10}};
        bool valid=std::isfinite(frequency)&&frequency>40&&frequency<1500&&confidence>=.85f&&rms>=.007f;
        float pitch=valid?69+12*std::log2(frequency/440.f):0;
        int midi=valid?int(std::lround(pitch)):-1;
        int stable=anchor.load(std::memory_order_relaxed);
        if(!valid||!on.load(std::memory_order_relaxed))stable=-1;
        else if(stable<0||std::fabs(pitch-stable)>.7f)stable=midi;
        anchor.store(stable,std::memory_order_relaxed);
        inputNote=midi;level=std::isfinite(rms)?rms:0;
        int target[2]={-1,-1};int p=preset.load(),k=key.load(),s=scale.load();
        if(valid&&stable>=0){
            int root=stable,best=100,degree=0;
            for(int n=stable-6;n<=stable+6;n++)for(int d=0;d<7;d++)if(((n-k)%12+12)%12==scales[s][d]&&std::abs(n-stable)<best){best=std::abs(n-stable);root=n;degree=d;}
            auto degreeNote=[&](int offset){int x=degree+offset,oct=0;while(x<0){x+=7;--oct;}while(x>=7){x-=7;++oct;}return root+scales[s][x]-scales[s][degree]+12*oct;};
            if(p==3){target[0]=stable-12;target[1]=stable+12;}
            else if(p==2){target[0]=degreeNote(-2);target[1]=degreeNote(2);}
            else {target[0]=degreeNote(2);if(p==1)target[1]=degreeNote(4);}
            for(int i=0;i<2;i++)if(target[i]>=0)ratios[i]=std::pow(2.f,(target[i]-stable)/12.f);
        }
        bool active=on.load(std::memory_order_relaxed)&&valid;
        note1=active?target[0]:-1;note2=active?target[1]:-1;
        for(int i=0;i<2;i++) gains[i]+=((active&&target[i]>=0?wet.load():0.f)-gains[i])*.001f;
        float mono=(l+r)*.5f,a=voices[0].tick(mono,ratios[0],rate),b=voices[1].tick(mono,ratios[1],rate);
        float d=on.load()?dry.load():1.f,pan=spread.load();
        outL=l*d+gains[0]*a*(1+pan)*.5f+gains[1]*b*(1-pan)*.5f;
        outR=r*d+gains[0]*a*(1-pan)*.5f+gains[1]*b*(1+pan)*.5f;
    }
};
}
