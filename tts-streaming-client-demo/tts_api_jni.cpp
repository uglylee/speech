
/*
 * guoqihang@baidu.com
 */

#include <jni.h>
#include <string.h>
#include <string>

#include "server_tts_api.h"
#include "com_baidu_speech_jserverttssdk_JBDSpeechTTSSDK.h"

class TTSRequestJni : public server_tts_api::ServerTTSCallback {
public:
    TTSRequestJni(JNIEnv * env, jobject &obj) {

        env->GetJavaVM(&_jvm);
        _object = env->NewGlobalRef(obj);
        //printf("object %p, %p\n", _object, env);
    }

    ~TTSRequestJni() {
    }

    JavaVM* get_jvm() const {
        return _jvm;
    }

    const jobject * get_jobject() const {
        return &_object;
    }

    void delete_jobject(JNIEnv* env) {
        env->DeleteGlobalRef(_object);
    }

    void on_data(std::string sn, const void* audio_data, size_t audio_len, bool req_fin) {

        //return;
        JNIEnv *env = NULL;
        int state = this->get_jvm()->AttachCurrentThread((void**)&env, NULL);
        //printf("on_data state: %d, object %p, %p\n", state, _object, env);

        jclass cls = env->GetObjectClass(_object);
        if (!cls) {
            //LOG("on_data_received failed find class");
            return;
        }

        // get method

        jmethodID java_func = env->GetMethodID(cls, "onData", "(Ljava/lang/String;[BIZ)V");
        if (java_func) {
            //printf("call OnMessage jni len %d, fin %d \n", audio_len, req_fin);
            jbyteArray bytes = env->NewByteArray(audio_len);
            env->SetByteArrayRegion(bytes, 0, audio_len, (jbyte*)audio_data);
            jstring j_sn = env->NewStringUTF(sn.c_str());
            jboolean j_fin = req_fin ? JNI_TRUE : JNI_FALSE;
            env->CallVoidMethod(*(this->get_jobject()), java_func, j_sn, bytes, audio_len, j_fin);
            env->DeleteLocalRef(bytes);
            env->DeleteLocalRef(j_sn);
        }
        env->DeleteLocalRef(cls);
        if (req_fin) {
            delete_jobject(env);
        }
        this->get_jvm()->DetachCurrentThread();

        if (req_fin) {
            delete this;
        }
    }

    void on_error(std::string sn, int err_code, const std::string& err_desc) {
        JNIEnv *env = NULL;
        this->get_jvm()->AttachCurrentThread((void**)&env, NULL);

        jclass cls = env->GetObjectClass(*(this->get_jobject()));
        if (!cls) {
            //LOG("on_data_received failed find class");
            return;
        }

        // get method

        jmethodID java_func = env->GetMethodID(cls, "onError", "(Ljava/lang/String;ILjava/lang/String;)V");
        if (java_func) {
            //printf("call OnMessage jni \n");
            jstring j_sn = env->NewStringUTF(sn.c_str());
            jstring j_err = env->NewStringUTF(err_desc.c_str());
            env->CallVoidMethod(*(this->get_jobject()), java_func, j_sn, (jint)err_code, j_err);
            env->DeleteLocalRef(j_sn);
            env->DeleteLocalRef(j_err);
        }
        env->DeleteLocalRef(cls);
        delete_jobject(env);
        this->get_jvm()->DetachCurrentThread();

        delete this;
    }

private:
    JavaVM *_jvm;
    jobject _object;
};

JNIEXPORT jstring JNICALL Java_com_baidu_speech_jserverttssdk_JBDSpeechTTSSDK_tTSStartRequest(JNIEnv * env, jobject obj, jstring url)
{

    const char* url_native =  NULL;
    server_tts_api::disable_bthread();
    if (url) {
        url_native = env->GetStringUTFChars(url, 0);
        std::string url_string = url_native;
        TTSRequestJni *jni_handler = new TTSRequestJni(env, obj);
        std::string sn = server_tts_api::start_new_request(url_string, jni_handler);
        env->ReleaseStringUTFChars(url, url_native);
        return  env->NewStringUTF(sn.c_str());
    }
    return NULL;
}

JNIEXPORT jstring JNICALL Java_com_baidu_speech_jserverttssdk_JBDSpeechTTSSDK_tTSGetVersion(JNIEnv * env, jclass cls)
{
    return  env->NewStringUTF(server_tts_api::version().c_str());
}

JNIEXPORT void JNICALL Java_com_baidu_speech_jserverttssdk_JBDSpeechTTSSDK_tTSSetGlobalConfig(JNIEnv * env, jclass cls, jint timeout, jint max_retry)
{
    server_tts_api::set_global_config(timeout, max_retry);
}
