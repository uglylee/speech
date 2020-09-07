package com.baidu.speech.jserverttssdk;

public class JBDSpeechTTSSDK {

    private JBDSpeechTTSCallback callBack;

    public static native String tTSGetVersion();

    public static native void tTSSetGlobalConfig(int timeout, int max_retry);

    public static String getVersion() {
        return tTSGetVersion();
    }

    public static void setGlobalConfig(int timeout, int max_retry) {
        tTSSetGlobalConfig(timeout, max_retry);
    }

    public native String tTSStartRequest(String url);

    public String startRequest(String url, JBDSpeechTTSCallback callback) {
        callBack = callback;
        return tTSStartRequest(url);
    }

    public void onData(String sn, byte[] audio, int length, boolean end) {
        callBack.onData(sn, audio, length, end);
    }

    public void onError(String sn, int errcode, String err_desc) {
        callBack.onError(sn, errcode, err_desc);
    }
}
