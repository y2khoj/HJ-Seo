#include "../app/src/main/cpp/vocal_harmony.h"
#include "../app/src/main/cpp/vocal_looper.h"
#include <unistd.h>
#include <atomic>
#include <cstdlib>
#include <new>
std::atomic<unsigned long> allocations{0};
void* operator new(std::size_t n){allocations.fetch_add(1);if(void* p=std::malloc(n?n:1))return p;throw std::bad_alloc();}
__attribute__((noinline)) void operator delete(void* p) noexcept{std::free(p);}
__attribute__((noinline)) void operator delete(void* p,std::size_t) noexcept{std::free(p);}
#include <cassert>
#include <cmath>
#include <cstdio>
int main(){
    {
        vh::Looper loop;assert(loop.sampleRate(8000));assert(loop.prepare(240,0,4));
        constexpr int bar=8000, length=bar*4;double state[14]{};float l,r;
        assert(loop.arm(0));loop.state(state);assert(state[4]==0);
        for(int i=0;i<bar;i++)loop.tick(.25f,.5f,l,r);
        loop.state(state);assert(state[5]==0&&state[6]==0);
        for(int i=0;i<length;i++)loop.tick(.25f,.5f,l,r);
        loop.state(state);assert(state[6]==1&&state[7]==1&&state[13]==0);
        loop.tick(0,0,l,r);assert(std::fabs(l-.25f)<1e-5&&std::fabs(r-.5f)<1e-5);
        assert(loop.arm(0));for(int i=0;i<length+100;i++)loop.tick(.9f,.9f,l,r);loop.cancel();
        loop.state(state);assert(state[6]==1&&state[7]==1);
        char path[]="/tmp/livemic-harmony-XXXXXX";int fd=mkstemp(path);assert(fd>=0);close(fd);
        assert(loop.saveLayer(0,path));loop.transport(0);loop.clear(0);assert(loop.loadLayer(1,path));unlink(path);
        loop.state(state);assert(state[7]==2);loop.transport(1);loop.tick(0,0,l,r);
        assert(std::fabs(l-.25f)<.001f&&std::fabs(r-.5f)<.001f);
        loop.layer(1,1,0,true);for(int i=0;i<10000;i++)loop.tick(0,0,l,r);assert(std::fabs(l)<.001f&&std::fabs(r)<.001f);
        loop.layer(1,1,0,false);
        for(int slot : {0,2,3}){
            loop.state(state);int revision=int(state[6]);assert(loop.arm(slot));
            for(int i=0;i<length*2+1&&int(state[6])==revision;i++){loop.tick(.1f,.2f,l,r);if(i%100==0)loop.state(state);}
            loop.state(state);assert(int(state[6])==revision+1&&int(state[13])==slot);
        }
        loop.state(state);assert(int(state[7])==15);
        for(int i=0;i<10000;i++)loop.tick(0,0,l,r);
        assert(std::fabs(l-.55f)<.002f&&std::fabs(r-1.1f)<.002f);
        loop.layer(0,1,-1,false);for(int i=0;i<10000;i++)loop.tick(0,0,l,r);
        assert(std::fabs(l-.55f)<.002f&&std::fabs(r-.9f)<.002f);
        loop.transport(0);assert(!loop.prepare(90,0,4));
        loop.transport(1);assert(loop.sampleRate(16000));loop.state(state);assert(state[0]==1);assert(std::fabs(state[2]-4)<1e-6&&int(state[7])==15);
    }
    for(int sampleRate : {44100,48000})for(int bars : {4,8})for(int meter : {0,1,2}){
        vh::Looper loop;assert(loop.sampleRate(sampleRate));assert(loop.prepare(240,meter,bars));
        const int beats=meter==0?4:meter==1?3:2;
        const int bar=int(std::llround(double(sampleRate)*60*beats/240));
        const int frames=bar*bars;
        double state[14]{};loop.state(state);assert(std::fabs(state[2]-double(frames)/sampleRate)<1e-8);
        assert(loop.arm(0));float l=0,r=0,click=0;int onsetCount=0,lastOnset=-sampleRate;
        unsigned long before=allocations.load();
        for(int i=0;i<bar;i++){
            loop.tick(.25f,.5f,l,r,&click);assert(l==0&&r==0);
            if(std::fabs(click)>.002f&&i-lastOnset>sampleRate/10){
                assert(std::abs(i-int(std::llround(onsetCount*sampleRate/4.)))<10);++onsetCount;lastOnset=i;
            }
        }
        assert(onsetCount==beats);loop.state(state);assert(state[5]==0&&state[3]==0);
        for(int i=0;i<frames;i++){loop.tick(.25f,.5f,l,r,&click);assert(click==0);}
        assert(allocations.load()==before);loop.state(state);assert(state[6]==1&&state[7]==1);
        for(int i=0;i<frames+2;i++){loop.tick(0,0,l,r,&click);assert(std::fabs(l-.25f)<1e-6&&std::fabs(r-.5f)<1e-6&&click==0);}
        assert(loop.arm(0));loop.state(state);assert(state[4]==0);
        int untilStart=0;do{loop.tick(.9f,.9f,l,r);loop.state(state);++untilStart;assert(untilStart<=frames);}while(state[5]<0);
        assert(untilStart==frames-1);
        for(int i=0;i<1000;i++)loop.tick(.9f,.9f,l,r);
        loop.cancel();loop.state(state);assert(state[6]==1&&state[7]==1);
        for(int i=0;i<frames;i++){loop.tick(0,0,l,r);assert(std::fabs(l-.25f)<1e-6&&std::fabs(r-.5f)<1e-6);}
        assert(allocations.load()==before);
    }
    {
        vh::Looper loop;assert(loop.sampleRate(48000));assert(loop.prepare(90,2,4));double state[14]{};loop.state(state);
        assert(std::fabs(state[2]-16./3)<1./48000);assert(loop.arm(0));float l,r,click;
        loop.tick(0,0,l,r,&click);loop.state(state);assert(std::fabs(state[3]-2.)<2./48000*90/60);
        for(int i=1;i<64000;i++)loop.tick(0,0,l,r,&click);
        loop.state(state);assert(state[3]==0&&state[5]==0);
    }
    {
        constexpr int scales[5][7]={{0,2,4,5,7,9,11},{0,2,3,5,7,8,10},{0,2,3,5,7,8,11},{0,2,3,5,7,9,10},{0,2,4,5,7,9,10}};
        vh::Harmony harmony;
        for(int key=0;key<12;key++)for(int scale=0;scale<5;scale++){
            int midi=60+key;float freq=440.f*std::pow(2.f,(midi-69)/12.f),l,r;
            harmony.configure(true,key,scale,2,.5f,1,0);harmony.tick(.1f,.1f,freq,.99f,.1f,48000,l,r);
            assert(harmony.inputNote==midi&&harmony.note1==midi+scales[scale][5]-12&&harmony.note2==midi+scales[scale][2]);
        }
    }
    {
        vh::Harmony harmony;harmony.configure(true,0,0,1,1,1,0);
        float l,r;auto f=[](float note){return 440.f*std::pow(2.f,(note-69.f)/12.f);};
        harmony.tick(.1f,.1f,f(60),.99f,.1f,48000,l,r);
        for(int i=0;i<100;i++){
            harmony.configure(true,0,0,1,float(i%10)/10.f,1,float(i%10)/10.f);
            harmony.tick(.1f,.1f,f(60.6f),.99f,.1f,48000,l,r);
            assert(harmony.note1==64&&harmony.note2==67);
        }
        for(int i=0;i<48000;i++){
            float note=60.f+.6f*std::sin(2*vh::pi*5*i/48000.f);
            harmony.tick(.1f,.1f,f(note),.99f,.1f,48000,l,r);
            assert(harmony.note1==64&&harmony.note2==67);
        }
        for(int i=0;i<480;i++){harmony.tick(.1f,.1f,f(62),.99f,.1f,48000,l,r);}
        assert(harmony.note1==65&&harmony.note2==69);
        for(int i=0;i<2400;i++){harmony.tick(0,0,0,0,0,48000,l,r);}
        harmony.configure(true,0,0,1,.4f,1,.3f);
        harmony.tick(0,0,0,0,0,48000,l,r);
        assert(harmony.note1==65);
        for(int i=0;i<4800;i++){harmony.tick(0,0,0,0,0,48000,l,r);}
        assert(harmony.note1==-1);
        for(int i=0;i<480;i++){harmony.tick(.1f,.1f,f(60.6f),.99f,.1f,48000,l,r);}
        assert(harmony.note1==64&&harmony.note2==67);
        harmony.configure(false,0,0,1,1,1,0);harmony.configure(true,0,0,1,1,1,0);
        harmony.tick(.1f,.1f,f(62),.99f,.1f,48000,l,r);assert(harmony.note1==65&&harmony.note2==69);
        vh::Harmony pitchTest;pitchTest.configure(true,0,0,0,1,0,0);
        float inputHz=f(60.6f),desired=inputHz*std::pow(2.f,4.f/12),wrong=inputHz*std::pow(2.f,3.f/12);
        double ar=0,ai=0,br=0,bi=0;
        pitchTest.tick(0,0,f(60),.99f,.1f,48000,l,r);
        for(int i=0;i<96000;i++){
            float x=.2f*std::sin(2*vh::pi*inputHz*i/48000.f);pitchTest.tick(x,x,inputHz,.99f,.1f,48000,l,r);
            if(i>=24000){ar+=l*std::cos(2*vh::pi*desired*i/48000);ai+=l*std::sin(2*vh::pi*desired*i/48000);br+=l*std::cos(2*vh::pi*wrong*i/48000);bi+=l*std::sin(2*vh::pi*wrong*i/48000);}
        }
        assert(std::hypot(ar,ai)>2*std::hypot(br,bi));
    }
    {
        vh::Looper loop;assert(loop.sampleRate(8000));assert(loop.prepare(240,0,4));
        assert(loop.arm(0));float l=0,r=0;for(int i=0;i<8000;i++)loop.tick(0,0,l,r);
        // Deliberately discontinuous loop; playback correction must not alter the recorded PCM.
        for(int i=0;i<32000;i++){float x=i<16000?1.f:(i>=31960&&i%2==0?1.f:-1.f);loop.tick(x,x,l,r);}
        float previous=0;for(int i=0;i<32000;i++){loop.tick(0,0,l,r);assert(std::fabs(l)<=1.000001f&&std::fabs(r)<=1.000001f);previous=l;}
        loop.tick(0,0,l,r);assert(std::fabs(l-previous)<.001f);assert(std::fabs(l-1.f)<.001f);
        char path[]="/tmp/livemic-seam-XXXXXX";int fd=mkstemp(path);assert(fd>=0);close(fd);
        assert(loop.saveLayer(0,path));loop.transport(0);loop.clear(0);assert(loop.loadLayer(0,path));unlink(path);
        loop.transport(1);loop.tick(0,0,l,r);assert(std::fabs(l-1.f)<.001f);
    }
    for(int rate:{44100,48000}){
        vh::Harmony h;float l,r;const int step=rate/100;
        h.configure(true,0,0,0,1,1,0);
        h.tick(.1f,.1f,261.6256f,.99f,.1f,rate,l,r);assert(h.inputNote==60);
        for(int i=1;i<step;i++){h.tick(.1f,.1f,293.6648f,.99f,.1f,rate,l,r);assert(h.inputNote==60);}
        h.tick(.1f,.1f,293.6648f,.99f,.1f,rate,l,r);assert(h.inputNote==62);
        h.configure(false,0,0,0,1,1,0);
        h.tick(.1f,.1f,261.6256f,.99f,.1f,rate,l,r);assert(h.inputNote==60&&h.note1==-1&&h.note2==-1);
    }
    for(int rate:{44100,48000}){
        vh::Harmony h;h.configure(true,0,0,1,1,0,0);
        unsigned long before=allocations.load();
        double re3=0,im3=0,re5=0,im5=0,re0=0,im0=0,energy=0;
        for(int i=0;i<rate*3;i++){
            float x=.2f*std::sin(2*vh::pi*220*i/rate),l,r;
            h.tick(x,x,220,.99,.14,rate,l,r);assert(std::isfinite(l)&&std::isfinite(r));
            if(i>rate){auto accumulate=[&](double hz,double& re,double& im){re+=l*std::cos(2*vh::pi*hz*i/rate);im+=l*std::sin(2*vh::pi*hz*i/rate);};
                accumulate(261.625565,re3,im3);accumulate(329.627557,re5,im5);accumulate(220,re0,im0);energy+=l*l;}
        }
        double a=std::hypot(re3,im3),b=std::hypot(re5,im5),c=std::hypot(re0,im0);
        std::printf("%d Hz: third %.2f fifth %.2f input %.2f\n",rate,a,b,c);
        assert(a>100&&b>100&&a>c&&b>c&&energy>1);assert(allocations.load()==before);
        for(int i=0;i<rate;i++){float l,r;h.tick(0,0,0,0,0,rate,l,r);assert(std::isfinite(l)&&std::isfinite(r));}
        assert(h.inputNote==-1&&h.note1==-1&&h.note2==-1);
        h.configure(false,0,0,0,1,0,0);
        for(int i=0;i<rate;i++){float l,r;h.tick(.1f,.1f,220,.99,.14,rate,l,r);if(i>rate/2)assert(std::fabs(l-.1f)<.001f);}
        h.configure(true,0,0,0,1,0,0);
        for(int i=0;i<rate;i++){float l,r;h.tick(.1f,.1f,220,.84,.14,rate,l,r);if(i>rate/2)assert(std::fabs(l)<.001f);}
        assert(h.note1==-1);
    }
}
