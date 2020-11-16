package com.example.wsdemo.Service;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import com.alibaba.fastjson.JSONObject;
import com.baidu.acu.pie.client.AsrClient;
import com.baidu.acu.pie.client.AsrClientFactory;
import com.baidu.acu.pie.client.Consumer;
import com.baidu.acu.pie.exception.AsrException;
import com.baidu.acu.pie.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * @author v_zhaoli04@baidu.com
 */

@ServerEndpoint("/wbserver/{userId}")
@Component
//@ConfigurationProperties(prefix = "asr")
public class WebSocketServer {

    private String appName = "ws-demo";
    private String ip = "10.185.181.13";
    private Integer port = 8052;
    private String username = "admin";   // 用户名, 请联系百度相关人员进行申请
    private String password = "1234567890";  // 密码, 请联系百度相关人员进行申请
    private AsrProduct pid = AsrProduct.CUSTOMER_SERVICE;

    /**静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。*/
    private static int onlineCount = 0;
    /**concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。*/
    private static ConcurrentHashMap<String,WebSocketServer> webSocketMap = new ConcurrentHashMap<>();
    /**与某个客户端的连接会话，需要通过它来给客户端发送数据*/
    private Session session;
    /**接收userId*/
    private String userId="";
    private StreamContext streamContext;
    private RequestMetaData requestMetaData;
    private AsrClient asrClient;
    private static Logger logger = LoggerFactory.getLogger(WebSocketServer.class);

    public WebSocketServer() throws FileNotFoundException {
    }

    /**
     * 连接建立成功调用的方法*/
    @OnOpen
    public void onOpen(Session session,@PathParam("userId") String userId) {
        this.session = session;
        this.userId=userId;
        System.out.println(username);
        this.requestMetaData = new RequestMetaData(true,1,1,0.005,10,"");
        this.asrClient = createAsrClient();
        this.streamContext=asrClient.asyncRecognize(new Consumer<RecognitionResult>() {
            public void accept(RecognitionResult recognitionResult) {
                try {
                    logger.info(recognitionResult.toString());
                    sendMessage(recognitionResult.getResult());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, requestMetaData);
        streamContext.enableCallback(new Consumer<AsrException>() {
            public void accept(AsrException e) {
                logger.error("Exception recognition for asr ： ", e);
            }
        });
        if(webSocketMap.containsKey(userId)){
            webSocketMap.remove(userId);
            webSocketMap.put(userId,this);
            //加入set中
        }else{
            webSocketMap.put(userId,this);
            //加入set中
            addOnlineCount();
            //在线数加1
        }
        logger.info("用户连接:"+userId+",当前在线人数为:" + getOnlineCount());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() throws IOException {
        this.streamContext.complete();
        if(webSocketMap.containsKey(userId)){
            webSocketMap.remove(userId);
            //从set中删除
            subOnlineCount();
        }

        logger.info("用户退出:"+userId+",当前在线人数为:" + getOnlineCount());
    }
    private static AsrProduct getAsrProduct(String pid) {
        for (AsrProduct asrProduct : AsrProduct.values()) {
            if (asrProduct.getCode().equals(pid)) {
                return asrProduct;
            }
        }
        return null;
    }

    private  AsrClient createAsrClient() {
        // 创建调用asr服务的客户端
        // asrConfig构造后就不可修改
        AsrConfig asrConfig = AsrConfig.builder()
                .appName(appName)
                .serverIp(ip)
                .serverPort(port)
                .product(pid)
                .userName(username)
                .password(password)
                .build();
        return AsrClientFactory.buildClient(asrConfig);
    }

    @OnMessage
    public void onMessage(byte[] message) throws IOException, InterruptedException {
        try {
            if(!streamContext.getFinishLatch().finished()){
                streamContext.send(message);
                Thread.sleep(10);
            }else{
                System.out.println("end");
            }
        } catch (AsrException e) {
            e.printStackTrace();
        }
    }


    /**
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        logger.error("用户错误:"+this.userId+",原因:"+error.getMessage());
        error.printStackTrace();
    }
    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }


    /**
     * 发送自定义消息
     * */
    public static void sendInfo(String message,@PathParam("userId") String userId) throws IOException {
//        log.info("发送消息到:"+userId+"，报文:"+message);
        System.out.println(3);
        if (webSocketMap.containsKey(userId)){
//        if(StringUtils.isNotBlank(userId)&&webSocketMap.containsKey(userId)){
            webSocketMap.get(userId).sendMessage(message);
        }else{
            logger.error("用户"+userId+",不在线！");
        }
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
    }
}
