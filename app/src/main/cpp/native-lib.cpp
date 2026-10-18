#include <jni.h>
#include <android/log.h>
#include <atomic>
#include <mutex>

#define TAG "GameDockNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

namespace {
std::mutex g_mutex;
bool g_ready = false;
std::atomic<bool> g_visible{false};
std::atomic<int> g_toggles{0};
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_nipuna_gamedock_NativeBridge_nativeInit(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lk(g_mutex);
    g_ready = true; g_toggles = 0; g_visible = false;
    LOGI("native core initialised");
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_nipuna_gamedock_NativeBridge_nativeVersion(JNIEnv *env, jobject) {
    return env->NewStringUTF("1.0.0");
}

JNIEXPORT void JNICALL
Java_com_nipuna_gamedock_NativeBridge_nativeSetOverlayVisible(JNIEnv *, jobject, jboolean v) {
    std::lock_guard<std::mutex> lk(g_mutex);
    if (!g_ready) return;
    bool nv = (v == JNI_TRUE);
    if (g_visible.exchange(nv) != nv) g_toggles++;
}

JNIEXPORT jint JNICALL
Java_com_nipuna_gamedock_NativeBridge_nativeToggleCount(JNIEnv *, jobject) {
    return g_toggles.load();
}

JNIEXPORT void JNICALL
Java_com_nipuna_gamedock_NativeBridge_nativeShutdown(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lk(g_mutex);
    g_ready = false; g_visible = false;
}

}
