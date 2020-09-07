package com.baidu.speech.jserverttssdk;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * 接收TTS返回的实际逻辑
 * 注意：只能在 linux下运行，运行时请加入 -Djava.library.path=/directory/of/libSpeechTTSJni.so/，
 * 如果还是出现找不到so库的情况，可以将so库的路径加入到LD_LIBRARY_PATH环境变量中，
 * `export LD_LIBRARY_PATH=/directory/of/libSpeechTTSJni.so/:$LD_LIBRARY_PATH`
 *
 * @Author xiashuai
 * @Date 2019/11/1 2:28 下午
 **/
public class JBDSpeechTTSSDKDemo implements JBDSpeechTTSCallback {

    private static OutputStream fos = null;

    public JBDSpeechTTSSDKDemo() throws FileNotFoundException {
        fos = new FileOutputStream("audio.mp3");
    }

    public static void main(String[] args) {

        System.loadLibrary("SpeechTTSJni");

        try {
            JBDSpeechTTSSDK tts_sdk = new JBDSpeechTTSSDK();
            JBDSpeechTTSSDKDemo callback = new JBDSpeechTTSSDKDemo();
            // JBDSpeechTTSSDK.SetGlobalConfig(3000, 3);
            String text = "这是一个测试的语音合成音频输出文件";
  tts_sdk.startRequest("http://14.215.177.147:80/text2audio?lan=zh&cuid=XXX&ctp=10&pdt=993&per=5117" +
             "&spd=5&pit=5&tex="+text+"",
                  callback);
            Thread.sleep(20000);
        } catch (Exception e) {
        }
    }

    public void onData(String sn, byte[] audio, int length, boolean end) {
        // TODO 音频数据处理部分
        try {
            fos.write(audio);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("receive tts OnData sn m : " + sn + " aduio len: " + length + " is end: " + end);
    }

    public void onError(String sn, int errcode, String err_desc) {
        System.out.println("receive tts OnError sn m : " + sn + " err_desc: " + err_desc);
    }
}
