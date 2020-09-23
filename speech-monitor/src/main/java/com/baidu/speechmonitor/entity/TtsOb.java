package com.baidu.speechmonitor.entity;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="tts")
public class TtsOb {

    private String mrcp;
    private String engine;

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getMrcp() {
        return mrcp;
    }
    public void setMrcp(String mrcp) {
        this.mrcp = mrcp;
    }

    public String getMrcpHost() {
        return mrcp.split(":")[0];
    }
    public int getMrcpPort() {
        return Integer.parseInt(mrcp.split(":")[1]);
    }

    public String getEngineHost(){
        return engine.split(":")[0];
    }
    public int getEnginePort(){
        return Integer.parseInt(engine.split(":")[1]);
    }

}
