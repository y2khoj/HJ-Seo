#include <cassert>
#include <cmath>
#include <cstdint>
#include <fstream>
#include <iostream>
#include <memory>
#include <vector>
#include "../app/src/main/cpp/composer_engine.h"
static void u16(std::ostream& o,uint16_t x){o.put(x&255);o.put(x>>8);}
static void u32(std::ostream& o,uint32_t x){u16(o,x&65535);u16(o,x>>16);}
int main(int argc,char** argv){
 if(argc!=4){std::cerr<<"usage: arrangement_render soundfont plan.txt output.wav\n";return 2;}
 std::ifstream in(argv[2]);double beats,bar,bpm;if(!(in>>beats>>bar>>bpm))return 3;
 std::vector<lv::Event> events;lv::Event e;while(in>>e.beat>>e.type>>e.ch>>e.note>>e.velocity>>e.program>>e.bank>>e.pan>>e.gain)events.push_back(e);
 auto engine=std::make_unique<lv::ComposerEngine>();if(!engine->load(argv[1]))return 4;
 constexpr int rate=44100;engine->sampleRate(rate);if(!engine->publish(events,beats,bar,bpm,0,0))return 5;engine->transport(1);
 uint32_t frames=uint32_t(beats*60/bpm*rate);std::ofstream out(argv[3],std::ios::binary);out.write("RIFF",4);u32(out,36+frames*4);out.write("WAVEfmt ",8);u32(out,16);u16(out,1);u16(out,2);u32(out,rate);u32(out,rate*4);u16(out,4);u16(out,16);out.write("data",4);u32(out,frames*4);
 float music[512],voice[512],count[256];double energy=0;for(uint32_t pos=0;pos<frames;){int n=std::min<uint32_t>(256,frames-pos);engine->render(music,voice,count,n);for(int i=0;i<n*2;i++){if(!std::isfinite(music[i]))return 6;energy+=music[i]*music[i];int value=std::lround(std::clamp(music[i],-1.f,1.f)*32767);u16(out,uint16_t(int16_t(value)));}pos+=n;}
 if(energy<1)return 7;std::cout<<argv[3]<<": "<<frames<<" frames, energy="<<energy<<"\n";
}
