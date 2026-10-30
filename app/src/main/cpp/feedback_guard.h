#pragma once
#include "audio_core.h"
namespace lm {
// Conservative protection for a microphone feeding a loudspeaker. Not a substitute for
// physical separation, a sensible device volume, or the device's acoustic echo canceller.
class FeedbackGuard {
    std::atomic<bool> enabled{false},tripped{false},resetRequested{false};
    std::array<std::atomic<float>,4> frequencies{};
    Biquad filters[2][4];float cached[4]{},blend[4]{};
    std::array<float,96> bins{},coefficients{};std::array<float,2048> window{};
    float candidate=0,initialLevel=0;int held=0,loud=0,next=0,incidents=0;double lastIncident=-100;
    void clearDetector(){candidate=initialLevel=0;held=loud=next=incidents=0;lastIncident=-100;for(auto& f:frequencies)f=0;}
public:
    void init(int sr,bool on){enabled=on;tripped=false;resetRequested=false;clearDetector();
        for(int i=0;i<96;i++){bins[i]=600.f*std::pow(2.f,i/24.f);coefficients[i]=2*std::cos(2*PI*bins[i]/sr);}
        for(int i=0;i<2048;i++)window[i]=.5f-.5f*std::cos(2*PI*i/2047);
        for(int i=0;i<4;i++){cached[i]=blend[i]=0;filters[0][i]=Biquad{};filters[1][i]=Biquad{};}
    }
    void reset(){tripped=false;resetRequested=true;}
    bool muted()const{return tripped;}
    int count()const{int n=0;for(auto& f:frequencies)if(f.load()>0)n++;return n;}
    void analyze(const float* input,int n,int sr,double seconds){
        if(!enabled)return;
        if(resetRequested.exchange(false))clearDetector();
        if(tripped)return;
        double energy=0;for(int i=0;i<n;i++)energy+=input[i]*input[i];float level=std::sqrt(energy/n);
        if(level>.65f){if(++loud>=2)tripped=true;}else loud=0;
        if(level<.025f){held=0;return;}
        double sum=0,best=0;int index=0;
        for(int k=0;k<96&&bins[k]<std::min(8000.f,sr*.45f);k++){
            double q1=0,q2=0;for(int i=0;i<n;i++){double q=input[i]*window[i]+coefficients[k]*q1-q2;q2=q1;q1=q;}
            double power=std::max(0.,q1*q1+q2*q2-coefficients[k]*q1*q2);sum+=power;if(power>best){best=power;index=k;}
        }
        float tone=bins[index];if(sum<=0||best/sum<.23){held=0;return;}
        if(held==0||std::abs(tone-candidate)>candidate*.045f){candidate=tone;initialLevel=level;held=1;return;}
        held++;
        if(held>=3&&level>initialLevel*1.6f){
            for(auto& f:frequencies)if(std::abs(f.load()-tone)<tone*.08f)return;
            frequencies[next++%4]=tone;held=0;
            incidents=seconds-lastIncident<10?incidents+1:1;lastIncident=seconds;
            if(incidents>=3)tripped=true;
        }
    }
    void prepare(int sr){for(int i=0;i<4;i++){
        float f=frequencies[i];if(f>0&&f!=cached[i]){
            float w=2*PI*f/sr,alpha=std::sin(w)/(2*18.f),a0=1+alpha;
            for(int ch=0;ch<2;ch++){auto& b=filters[ch][i];b=Biquad{};b.b0=1/a0;b.b1=-2*std::cos(w)/a0;b.b2=1/a0;b.a1=b.b1;b.a2=(1-alpha)/a0;}
        }cached[i]=f;
    }}
    void process(float& left,float& right){if(!enabled)return;
        for(int i=0;i<4;i++){
            float target=cached[i]>0?.88f:0;blend[i]+=(target-blend[i])*.002f;
            if(blend[i]>.0001f){left+=(filters[0][i].tick(left)-left)*blend[i];right+=(filters[1][i].tick(right)-right)*blend[i];}
        }
        if(tripped){left=right=0;return;}
        left=std::clamp(left,-.45f,.45f);right=std::clamp(right,-.45f,.45f);
    }
};
}
