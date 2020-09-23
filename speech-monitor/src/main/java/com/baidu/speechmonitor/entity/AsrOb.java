package com.baidu.speechmonitor.entity;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="asr")
public class AsrOb {

    private String mrcp;
    private String streaming;
    private String engine;

    public String getEngine() {return engine;}
    public void setEngine(String engine) {this.engine = engine;}
    public String getMrcp() {
        return mrcp;
    }
    public void setMrcp(String mrcp) {
        this.mrcp = mrcp;
    }
    public String getStreaming() {
        return streaming;
    }
    public void setStreaming(String streaming) {
        this.streaming = streaming;
    }

    public String getMrcpHost() {
        return mrcp.split(":")[0];
    }
    public int getMrcpPort() {
        return Integer.parseInt(mrcp.split(":")[1]);
    }
    public String getStreamingHost() {
        return streaming.split(":")[0];
    }
    public int getStreamingPort() {
        return Integer.parseInt(streaming.split(":")[1]);
    }
    public String getEngineHost(){return engine.split(":")[0];}
    public int getEnginePort(){
        return Integer.parseInt(engine.split(":")[1]);
    }

}
