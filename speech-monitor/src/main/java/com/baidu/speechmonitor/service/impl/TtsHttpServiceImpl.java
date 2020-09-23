package com.baidu.speechmonitor.service.impl;

import com.baidu.speechmonitor.Consts;
import com.baidu.speechmonitor.service.TtsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class TtsHttpServiceImpl implements TtsService {
    private static Logger logger= LoggerFactory.getLogger(TtsHttpServiceImpl.class);

    @Value("${tts.request}")
    private String TTS_HTTP_URL;
    @Value("${tts.read_timeout}")
    private int TTS_HTTP_READ_TIMEOUT;
    @Value("${tts.connect_timeout}")
    private int TTS_HTTP_CONNECT_TIMEOUT;
    @Value("${tts.text_length}")
    private int TTS_HTTP_TEXT_LENGTH;
    @Value("${tts.retry_time}")
    private Integer RETRY_TIME;

    @Override
    public boolean httpCheck() {
        for(int i=1;i<RETRY_TIME;i++){
            if(checkHttpStatus(i)){
                return true;
            };
        }
        return false;
    }
    public boolean checkHttpStatus(int retry_time){
        Long start_ts = System.currentTimeMillis(); // 当前时间戳
        try {
            logger.info("[tts_http check][start_ts:"+start_ts+"][url:"+TTS_HTTP_URL+"][step:start][retry_time:"+retry_time+"]");
            URL url = new URL(TTS_HTTP_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setDoOutput(true); // 设置该连接是可以输出的
            connection.setConnectTimeout(TTS_HTTP_CONNECT_TIMEOUT);
            connection.setReadTimeout(TTS_HTTP_READ_TIMEOUT);
            connection.setRequestMethod("GET"); // 设置请求方式
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            String line = null;
            StringBuilder result = new StringBuilder();
            while ((line = br.readLine()) != null) { // 读取数据
                result.append(line + "\n");
            }
            connection.disconnect();
            if(result.length() < TTS_HTTP_TEXT_LENGTH){
                logger.warn("[tts_http check][start_ts:"+start_ts+"][url:"+TTS_HTTP_URL+"][step:finish][status:unnormal][retry_time:"+retry_time+"][reason:result < tts.text.length]");
                return false;
            }
        } catch (Exception e) {
            logger.warn("[tts_http check][start_ts:"+start_ts+"][url:"+TTS_HTTP_URL+"][step:finish][status:unnormal][retry_time:"+retry_time+"][reason:connect_timeout]");
//            e.printStackTrace();
            return false;
        }
        logger.info("[tts_http check][start_ts:"+start_ts+"][url:"+TTS_HTTP_URL+"][step:finish][status:normal][retry_time:"+retry_time+"]");
        return  true;

    }

}
