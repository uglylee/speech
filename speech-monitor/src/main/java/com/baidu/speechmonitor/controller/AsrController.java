package com.baidu.speechmonitor.controller;

import com.baidu.speechmonitor.Consts;
import com.baidu.speechmonitor.common.Result;
import com.baidu.speechmonitor.entity.AsrOb;
import com.baidu.speechmonitor.service.impl.AsrServiceImpl;
import com.baidu.speechmonitor.service.impl.SocketServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

@Controller
@RequestMapping("/asr")
public class AsrController {

    @Autowired
    private SocketServiceImpl socketServiceImpl;
    @Autowired
    private AsrServiceImpl asrServiceImpl;

    @Autowired
    private AsrOb asrOb;
    private static Logger logger = Logger
            .getLogger("controller.AsrController");
    @Value("${asr.streaming.host}")
    private String streaming_ip ;          // asr服务的ip地址
    @Value("${asr.streaming.port}")
    private Integer streaming_port;     // asr服务的端口

    @GetMapping(value = {"/mrcp"})
    @ResponseBody
    public String getMrcpStatus() {
        return Result.checkStatus(
                socketServiceImpl.isHostConnectable(asrOb.getMrcpHost(),asrOb.getMrcpPort())&
                        socketServiceImpl.isHostConnectable(asrOb.getEngineHost(),asrOb.getEnginePort())
        );
    }
    @GetMapping(value = {"/streaming"})
    @ResponseBody
    public String getStreamingStatus() throws InterruptedException {

        return Result.checkStatus(
                socketServiceImpl.isHostConnectable(streaming_ip,streaming_port)&
                        asrServiceImpl.streamingCheck());

    }

}
