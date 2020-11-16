package com.example.wsdemo.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AsrController {

    @GetMapping("/")
    public String index(){
        return "index";
    }

//    @GetMapping("page")
//    public ModelAndView page(){
//        return new ModelAndView("websocket");
//    }

//    @RequestMapping("/push/{toUserId}")
//    public String pushToWeb(String message, @PathVariable String toUserId) throws IOException {
//        WebSocketServer.sendInfo(message,toUserId);
//        return ResponseEntity.ok("MSG SEND SUCCESS");
//    }

}
