#include "../app/src/main/cpp/record_alignment.h"
#include <cassert>
#include <cmath>
int main(){for(int rate:{44100,48000})for(int ms:{-500,-250,0,250,500}){lm::RecordAlignment a;a.init(rate,ms);int vh=-1,mh=-1;for(int i=0;i<rate+ a.tail();i++){float vl=i==0?1:0,vr=vl,ml=vl,mr=vl;a.tick(vl,vr,ml,mr);assert(vl==vr&&ml==mr);if(vl>0)vh=i;if(ml>0)mh=i;}assert(vh==std::max(0,ms)*rate/1000);assert(mh==std::max(0,-ms)*rate/1000);assert(vh-mh==ms*rate/1000);}}
