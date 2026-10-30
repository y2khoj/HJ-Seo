#pragma once
#include <algorithm>
#include <atomic>
#include <cmath>

namespace lm {
// UI writes a desired time; only the audio callback moves the playback cursor.
class LocalSeek {
    std::atomic<double> pending{-1};
public:
    bool request(double seconds,double duration){
        if(!std::isfinite(seconds)||!std::isfinite(duration)||duration<=0)return false;
        pending.store(std::clamp(seconds,0.0,duration),std::memory_order_release);
        return true;
    }
    double peek() const {return pending.load(std::memory_order_acquire);}
    double consume(){return pending.exchange(-1,std::memory_order_acq_rel);}
    void clear(){pending.store(-1,std::memory_order_release);}
};
}
