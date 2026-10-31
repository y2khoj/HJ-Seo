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
    unsigned write=0; float phase=0, ratio=1, length=2160;
public:
    void reset(){history.fill(0);write=0;phase=0;ratio=1;}
    float tick(float input,float target,float windowLength){
        history[write]=input;
        ratio += (target-ratio)*.001f;
        length += (windowLength-length)*.0001f;
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
    Grain voices[2]; float gains[2]{0,0},ratios[2]{1,1}, dryGain=1, panGain=.5f, windowLength=2160;
    int countdown=0, heldFrames=0, stable=-1, targets[2]{-1,-1}, lastRate=0; float smoothing=.001f;
    unsigned observedRevision=~0u, observedReset=0;
    std::atomic<unsigned> configRevision{0}, resetRevision{0};
    std::atomic<bool> on{false};std::atomic<int> key{0},scale{0},preset{0};
    std::atomic<float> wet{.5f},dry{1},spread{.5f};
    float wetTarget=0, dryTarget=1, panTarget=.5f; bool active=false;
public:
    std::atomic<int> inputNote{-1},note1{-1},note2{-1};std::atomic<float> level{0};
    bool enabled() const{return on.load(std::memory_order_relaxed);}
    void configure(bool enabled,int k,int s,int p,float w,float d,float sp){
        key=std::clamp(k,0,11);scale=std::clamp(s,0,4);preset=std::clamp(p,0,3);
        wet=std::isfinite(w)?std::clamp(w,0.f,1.f):0;dry=std::isfinite(d)?std::clamp(d,0.f,1.f):1;spread=std::isfinite(sp)?std::clamp(sp,0.f,1.f):0;on=enabled;
        if(!enabled)resetRevision.fetch_add(1,std::memory_order_relaxed);
        configRevision.fetch_add(1,std::memory_order_release);
    }
    void disable(){on=false;resetRevision.fetch_add(1,std::memory_order_relaxed);configRevision.fetch_add(1,std::memory_order_release);}
    void clearPitch(){inputNote=-1;note1=-1;note2=-1;level=0;resetRevision.fetch_add(1,std::memory_order_relaxed);configRevision.fetch_add(1,std::memory_order_release);}
    void tick(float l,float r,float frequency,float confidence,float rms,int rate,float& outL,float& outR){
        static constexpr int scales[5][7]={{0,2,4,5,7,9,11},{0,2,3,5,7,8,10},{0,2,3,5,7,8,11},{0,2,3,5,7,9,10},{0,2,4,5,7,9,10}};
        const unsigned rev=configRevision.load(std::memory_order_acquire);
        if (--countdown<=0 || rev!=observedRevision || rate!=lastRate){
            const int step=std::max(1,rate/100); countdown=step; lastRate=rate; smoothing=1.f-std::exp(-1.f/(std::max(rate,1)*.015f));
            if(rev!=observedRevision){
                unsigned reset=resetRevision.load(std::memory_order_relaxed);
                if(reset!=observedReset){stable=-1;heldFrames=0;observedReset=reset;}
                observedRevision=rev;
            }
            bool enabled=on.load(std::memory_order_relaxed);
            bool valid=std::isfinite(frequency)&&frequency>40&&frequency<1500&&std::isfinite(confidence)&&confidence>=.85f&&std::isfinite(rms)&&rms>=.007f;
            int midi=-1;
            if(valid){
                float pitch=69+12*std::log2(frequency/440.f);midi=int(std::lround(pitch));
                if(enabled){
                if(stable<0||std::fabs(pitch-stable)>.7f)stable=midi;
                heldFrames=std::max(1,rate*80/1000);
                // Four to twelve periods, constrained to a useful 30–60 ms window.
                windowLength=std::min(4000.f,std::clamp(8.f*rate/frequency,rate*.030f,rate*.060f));
                } else {stable=-1;heldFrames=0;}
            }else if(!enabled){stable=-1;heldFrames=0;}
            else {heldFrames=std::max(0,heldFrames-step);if(!heldFrames)stable=-1;}
            inputNote=midi;level=std::isfinite(rms)?rms:0;
            targets[0]=targets[1]=-1;
            int p=preset.load(std::memory_order_relaxed),k=key.load(std::memory_order_relaxed),s=scale.load(std::memory_order_relaxed);
            if(stable>=0){
                int root=stable,best=100,degree=0;
                for(int n=stable-6;n<=stable+6;n++)for(int d=0;d<7;d++)if(((n-k)%12+12)%12==scales[s][d]&&std::abs(n-stable)<best){best=std::abs(n-stable);root=n;degree=d;}
                auto degreeNote=[&](int offset){int x=degree+offset,oct=0;while(x<0){x+=7;--oct;}while(x>=7){x-=7;++oct;}return root+scales[s][x]-scales[s][degree]+12*oct;};
                if(p==3){targets[0]=stable-12;targets[1]=stable+12;}
                else if(p==2){targets[0]=degreeNote(-2);targets[1]=degreeNote(2);}
                else {targets[0]=degreeNote(2);if(p==1)targets[1]=degreeNote(4);}
                for(int i=0;i<2;i++)if(targets[i]>=0)ratios[i]=std::pow(2.f,(targets[i]-stable)/12.f);
            }
            active=enabled&&stable>=0;note1=active?targets[0]:-1;note2=active?targets[1]:-1;
            wetTarget=wet.load(std::memory_order_relaxed)*(targets[1]>=0?.70710678f:1.f);
            dryTarget=enabled?dry.load(std::memory_order_relaxed):1.f;panTarget=spread.load(std::memory_order_relaxed);
        }
        // Time constants are expressed in seconds so behavior follows device rate.
        float smooth=smoothing;
        for(int i=0;i<2;i++)gains[i]+=((active&&targets[i]>=0?wetTarget:0.f)-gains[i])*smooth;
        dryGain+=(dryTarget-dryGain)*smooth;panGain+=(panTarget-panGain)*smooth;
        float mono=(l+r)*.5f,a=voices[0].tick(mono,ratios[0],windowLength),b=voices[1].tick(mono,ratios[1],windowLength);
        outL=l*dryGain+gains[0]*a*(1+panGain)*.5f+gains[1]*b*(1-panGain)*.5f;
        outR=r*dryGain+gains[0]*a*(1-panGain)*.5f+gains[1]*b*(1+panGain)*.5f;
    }
};
}
