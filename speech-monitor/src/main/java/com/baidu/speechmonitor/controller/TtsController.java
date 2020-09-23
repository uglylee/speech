package com.baidu.speechmonitor.controller;

import com.baidu.speechmonitor.common.Result;
import com.baidu.speechmonitor.entity.AsrOb;
import com.baidu.speechmonitor.entity.TtsOb;
import com.baidu.speechmonitor.service.TtsService;
import com.baidu.speechmonitor.service.impl.SocketServiceImpl;
import com.baidu.speechmonitor.service.impl.TtsHttpServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.logging.Logger;

@Controller
@RequestMapping("/tts")
public class TtsController {

    @Autowired
    private SocketServiceImpl socketServiceImpl;
    @Autowired
    private TtsHttpServiceImpl ttsHttpService;


    @Autowired
    private TtsOb ttsOb;
    private static Logger logger = Logger
            .getLogger("controller.TtsController");

    @GetMapping(value = {"/mrcp"})
    @ResponseBody
    public String getMrcpStatus() {
        return Result.checkStatus(
                socketServiceImpl.isHostConnectable(ttsOb.getMrcpHost(),ttsOb.getMrcpPort())&
                        socketServiceImpl.isHostConnectable(ttsOb.getEngineHost(),ttsOb.getEnginePort()));
    }

    @GetMapping(value = {"/http"})
    @ResponseBody
    public String getHttpStatus(){
        return Result.checkStatus(
                        ttsHttpService.httpCheck()
        );
    }

}
