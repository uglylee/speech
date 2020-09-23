package com.baidu.speechmonitor.service.impl;

import com.baidu.speechmonitor.Consts;
import com.baidu.speechmonitor.service.SocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;


@Service
public class SocketServiceImpl implements SocketService {
    private static Logger logger= LoggerFactory.getLogger(SocketServiceImpl.class);
    @Value("${socket.connect_timeout}")
    private int HOST_REQUEST_TIMEOUT;
    @Value("${socket.retry_time}")
    private int RETRY_TIME;;
    @Override
    public boolean isHostConnectable(String host, int port) {
        for(int i=1;i<RETRY_TIME;i++){
            if(checkSocket(host,port,i)){
                return true;
            };
        }
        return false;
    }
    private boolean checkSocket(String host,int port,int retry_time){
        Long start_ts = System.currentTimeMillis(); // 当前时间戳
        Socket socket = new Socket();
        try {
            logger.info("[socket check][start_ts:"+start_ts+"][host:"+host+"][port:"+port+"][step:start][retry_time:"+retry_time+"]");
            socket.connect(new InetSocketAddress(host, port), HOST_REQUEST_TIMEOUT);
        } catch (IOException e) {
            logger.warn("[socket check][start_ts:"+start_ts+"][host:"+host+"][port:"+port+"][step:finish][status:unnormal][retry_time:"+retry_time+"]");
            return false;
        } finally {
            try {
                socket.close();
                logger.info("[socket check][start_ts:"+start_ts+"][host:"+host+"][port:"+port+"][step:close][retry_time:"+retry_time+"]");
            } catch (IOException e) {
                logger.warn(e.toString());
//                e.printStackTrace();
            }
        }
        logger.info("[socket check][start_ts:"+start_ts+"][host:"+host+"][port:"+port+"][step:finish][status:normal][retry_time:"+retry_time+"]");
        return true;
    }
    @Override
    public boolean isHostReachable(String host, Integer timeOut) {
        try {
            return InetAddress.getByName(host).isReachable(timeOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
