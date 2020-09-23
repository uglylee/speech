package com.baidu.speechmonitor.service.impl;


import com.baidu.acu.pie.client.AsrClient;
import com.baidu.acu.pie.client.AsrClientFactory;
import com.baidu.acu.pie.model.AsrConfig;
import com.baidu.acu.pie.model.AsrProduct;
import com.baidu.speechmonitor.service.AsrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.joda.time.DateTime;

import com.baidu.acu.pie.client.Consumer;
import com.baidu.acu.pie.exception.AsrException;

import com.baidu.acu.pie.model.RecognitionResult;
import com.baidu.acu.pie.model.StreamContext;
@Service
public class AsrServiceImpl implements AsrService {


    private static Logger logger= LoggerFactory.getLogger(AsrServiceImpl.class);
    private String appName = "monitor";
    @Value("${asr.streaming.host}")
    private String ip ;          // asr服务的ip地址
    @Value("${asr.streaming.port}")
    private Integer port;     // asr服务的端口
    private AsrProduct pid = AsrProduct.CUSTOMER_SERVICE;     // asr模型编号(不同的模型在不同的场景下asr识别的最终结果可能会存在很大差异)
    private String userName = "admin";    // 用户名, 请联系百度相关人员进行申请
    private String passWord = "your_password";    // 密码, 请联系百度相关人员进行申请
    @Value("${asr.audio_res}")
    private String ASR_AUDIO_RES;
    @Value("${asr.audio_file}")
    private String ASR_AUDIOPATH; // 音频文件路径
    @Value("${asr.retry_time}")
    private Integer RETRY_TIME;
    @Override
    public boolean streamingCheck(){
        for(int i=1;i<=RETRY_TIME;i++){
            if(asyncRecognizeWithStream(createAsrClient(),i)){
                return true;
            };
        }
        return false;
    }

    private AsrClient createAsrClient() {
        // 创建调用asr服务的客户端
        // asrConfig构造后就不可修改
        AsrConfig asrConfig = AsrConfig.builder()
                .appName(appName)
                .serverIp(ip)
                .serverPort(port)
                .product(pid)
                .userName(userName)
                .password(passWord)
                .build();
        return AsrClientFactory.buildClient(asrConfig);
    }
    static boolean finish = false;

    private  boolean asyncRecognizeWithStream(AsrClient asrClient,int retry_time) {
        Long start_ts = System.currentTimeMillis(); // 当前时间戳
        logger.info("[streaming check][start_ts:"+start_ts+"][host:"+ip+"][port:"+port+"][step:start][retry_time:"+retry_time+"]");
        final AtomicReference<DateTime> beginSend = new AtomicReference<DateTime>();
        final StreamContext streamContext = asrClient.asyncRecognize(new Consumer<RecognitionResult>() {
            String res;
            public void accept(RecognitionResult recognitionResult) {
                res = recognitionResult.getResult();
                for(int i=0;i<ASR_AUDIO_RES.length();i++){
                    if (res.contains(ASR_AUDIO_RES.charAt(i) + "")) {
                        finish = true;
                        break;
                    }
                }
            }
        });
        // 异常回调
        streamContext.enableCallback(new Consumer<AsrException>() {
            public void accept(AsrException e) {
                logger.error("Exception recognition for asr ：", e);
            }
        });

        try {
            finish = false;
            // 这里从文件中得到一个输入流InputStream，实际场景下，也可以从麦克风或者其他音频源来得到InputStream
            final FileInputStream audioStream = new FileInputStream(ASR_AUDIOPATH);
            // 实时音频流的情况下，8k音频用320， 16k音频用640
            final byte[] data = new byte[asrClient.getFragmentSize()];
            // 创建延时精确的定时任务
            ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
            final CountDownLatch sendFinish = new CountDownLatch(1);
            // 控制台打印每次发包大小
            System.out.println(new DateTime().toString() + "\t" + Thread.currentThread().getId() +
                    " start to send with package size=" + asrClient.getFragmentSize());
            // 设置发送开始时间
            beginSend.set(DateTime.now());
            // 开始执行定时任务
            executor.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    try {
                        int count = 0;
                        // 判断音频有没有发送和处理完成
                        if ((count = audioStream.read(data)) != -1 && !streamContext.getFinishLatch().finished() && !finish) {
                            // 发送音频数据包
                            streamContext.send(data);
                        } else {
                            // 音频处理完成，置0标记，结束所有线程任务
                            sendFinish.countDown();
                        }
                    } catch (AsrException | IOException e) {
                        e.printStackTrace();
                        // 异常时，置0标记，结束所有线程任务
                        sendFinish.countDown();
                    }
                }
            }, 0, 20, TimeUnit.MILLISECONDS); // 0:第一次发包延时； 20:每次任务间隔时间; 单位：ms
            // 阻塞主线程，直到CountDownLatch的值为0时停止阻塞
            sendFinish.await();
//            System.out.println(new DateTime().toString() + "\t" + Thread.currentThread().getId() + " send finish");
            // 结束定时任务
            executor.shutdown();
            streamContext.complete();
            // 等待最后输入的音频流识别的结果返回完毕（如果略掉这行代码会造成音频识别不完整!）
            streamContext.await();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            asrClient.shutdown();
        }
        if(finish){
            logger.info("[streaming check][start_ts:"+start_ts+"][host:"+ip+"][port:"+port+"][step:finish][retry_time:"+retry_time+"][status:normal]");
        }else{
            logger.warn("[streaming check][start_ts:"+start_ts+"][host:"+ip+"][port:"+port+"][step:finish][retry_time:"+retry_time+"][status:unnormal]");
        }
        return finish;
    }


}
