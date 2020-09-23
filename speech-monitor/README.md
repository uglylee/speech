# SPEECH-MONITOR

## BUILD AND START
```shell script
mvn clean package
java -jar target/xxx.jar
```
## REQUEST
### ASR
```
// streaming-server
http://ip:8080/monitor/asr/streaming

// mrcp-asr
http://ip:8080/monitor/asr/mrcp
```

### TTS
```shell script
// http
http://ip:8080/monitor/tts/http

// mrcp
http://ip:8080/monitor/tts/mrcp
```
