package com.baidu.speech.jserverttssdk;

public interface JBDSpeechTTSCallback {

    /**
     * 对返回的音频数据进行处理
     * <p>
     * sn: 请求序列号
     * audio: 返回的音频数据片段
     * length: 返回的音频长度
     * end: 音频数据是否全部返回
     **/
    public void onData(String sn, byte[] audio, int length, boolean end);
    /**
     * 错误返回的处理
     * <p>
     * sn: 请求序列号
     * errcode: 错误编码
     * err_desc: 错误描述
     **/
    public void onError(String sn, int errcode, String err_desc);
}
