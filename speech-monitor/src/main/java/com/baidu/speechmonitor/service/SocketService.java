package com.baidu.speechmonitor.service;

/**
 * @author uglylee
 */
public interface SocketService {


    public  boolean isHostConnectable(String host, int port);

    public  boolean isHostReachable(String host, Integer timeOut);


}
