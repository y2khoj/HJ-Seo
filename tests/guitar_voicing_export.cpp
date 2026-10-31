#include <iostream>
#include "../app/src/main/cpp/composer_engine.h"
int main(){for(int root=60;root<72;root++)for(int quality=0;quality<11;quality++){int tones[6];int count=lv::guitarVoicing(root,quality,tones);std::cout<<root<<' '<<quality;for(int i=0;i<count;i++)std::cout<<' '<<tones[i];std::cout<<'\n';}}
