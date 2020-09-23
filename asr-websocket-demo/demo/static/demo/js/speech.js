var begin = document.getElementById('intercomBegin');
var end = document.getElementById('intercomEnd');

var ws = null; //实现WebSocket
var record = null; //多媒体对象，用来处理音频
var ws1 = null;
var id=null;
        //
var timestamp=null;

var url = null;
var port = null;
var user_name = null;
var password = null;
var save_radio = null;
var save_file = null;
var send_per_seconds = null;
var sleep_ratio = null;
var enable_chunk = null;
var enable_flush_data = null;
var sample_rate = null;
var read_bytes_len = null;
var read_cache_sleep_time =null;
function init(rec) {
    record = rec;
}

//录音对象
var Recorder = function(stream) {
    var sampleBits = 16; //输出采样数位 8, 16
    var sampleRate = 8000; //输出采样率
    var context = new AudioContext();
    var audioInput = context.createMediaStreamSource(stream);
    var recorder = context.createScriptProcessor(4096, 1, 1);
    var audioData = {
        size: 0, //录音文件长度
        buffer: [], //录音缓存
        inputSampleRate: 48000, //输入采样率
        inputSampleBits: 16, //输入采样数位 8, 16
        outputSampleRate: sampleRate, //输出采样数位
        oututSampleBits: sampleBits, //输出采样率
        clear: function() {
            this.buffer = [];
            this.size = 0;
        },
        input: function(data) {
            this.buffer.push(new Float32Array(data));
            this.size += data.length;
        },
        compress: function() { //合并压缩
            //合并
            var data = new Float32Array(this.size);
            var offset = 0;
            for (var i = 0; i < this.buffer.length; i++) {
                data.set(this.buffer[i], offset);
                offset += this.buffer[i].length;
            }
            //压缩
            var compression = parseInt(this.inputSampleRate / this.outputSampleRate);
            var length = data.length / compression;
            var result = new Float32Array(length);
            var index = 0,
            j = 0;
            while (index < length) {
                result[index] = data[j];
                j += compression;
                index++;
            }
            return result;
        },
        encodePCM: function() { //这里不对采集到的数据进行其他格式处理，如有需要均交给服务器端处理。
            var sampleRate = Math.min(this.inputSampleRate, this.outputSampleRate);
            var sampleBits = Math.min(this.inputSampleBits, this.oututSampleBits);
            var bytes = this.compress();
            var dataLength = bytes.length * (sampleBits / 8);
            var buffer = new ArrayBuffer(dataLength);
            var data = new DataView(buffer);
            var offset = 0;
            for (var i = 0; i < bytes.length; i++, offset += 2) {
            var s = Math.max(-1, Math.min(1, bytes[i]));
                data.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7FFF, true);
            }
            return new Blob([data]);
        }
    };

    var sendData = function() { //对以获取的数据进行处理(分包)
        var reader = new FileReader();
        reader.onload = e => {
            var outbuffer = e.target.result;
            var arr = new Int8Array(outbuffer);
            if (arr.length > 0) {
                var tmparr = new Int8Array(1024);
                var j = 0;
                for (var i = 0; i < arr.byteLength; i++) {
                    tmparr[j++] = arr[i];
                    if (((i + 1) % 1024) == 0) {
                        ws.send(tmparr);
                        if (arr.byteLength - i - 1 >= 1024) {
                            tmparr = new Int8Array(1024);
                        } else {
                            tmparr = new Int8Array(arr.byteLength - i - 1);
                        }
                        j = 0;
                    }
                    if ((i + 1 == arr.byteLength) && ((i + 1) % 1024) != 0) {
                        ws.send(tmparr);
                    }
                }
            }
        };
        reader.readAsArrayBuffer(audioData.encodePCM());
        audioData.clear();//每次发送完成则清理掉旧数据
    };

    this.start = function() {
        audioInput.connect(recorder);
        recorder.connect(context.destination);
    }

    this.stop = function() {
        recorder.disconnect();
    }

    this.getBlob = function() {
        return audioData.encodePCM();
    }

    this.clear = function() {
        audioData.clear();
    }

    recorder.onaudioprocess = function(e) {
        var inputBuffer = e.inputBuffer.getChannelData(0);
        audioData.input(inputBuffer);
        sendData();
    }
}
  function guid() {
        return Number(Math.random().toString().substr(3, 3) + Date.now()).toString(36);
    }
function getAudioInfo(){

    url = document.getElementById("url").value;
    port = document.getElementById("port").value;
    user_name = document.getElementById("user_name").value;
    password = document.getElementById("password").value;
    product_id = document.getElementById("product_id").value;
    save_radio = document.getElementById("save_radio").value;
    save_file = document.getElementById("save_file").value;
    send_per_seconds = document.getElementById("send_per_seconds").value;
    sleep_ratio = document.getElementById("sleep_ratio").value;
    enable_chunk = document.getElementById("enable_chunk").value;
    enable_flush_data = document.getElementById("enable_flush_data").value;
    sample_rate = document.getElementById("sample_rate").value;
    read_bytes_len = document.getElementById("read_bytes_len").value;
    read_cache_sleep_time = document.getElementById("read_cache_sleep_time").value;
    var dic = {};
    dic["id"] = id;
    dic["url"] = url;
    dic["port"] = port;
    dic["user_name"] = user_name;
    dic["password"] = password;
    dic["product_id"] = product_id;
    dic["save_radio"] = save_radio;
    dic["save_file"] = save_file;
    dic["send_per_seconds"] = send_per_seconds;
    dic["sleep_ratio"] = sleep_ratio;
    dic["enable_chunk"] = enable_chunk;
    dic["enable_flush_data"] = enable_flush_data;
    dic["sample_rate"] = sample_rate;
    dic["read_bytes_len"] = read_bytes_len;
    dic["read_cache_sleep_time"] = read_cache_sleep_time;
    dic["timestamp"] = timestamp;
    console.log(timestamp)
    return JSON.stringify(dic);

}

/*
* WebSocket
*/
function useWebSocket() {

    ws = new WebSocket("ws://localhost:8000/demo/test_websocket");
    ws.binaryType = 'arraybuffer'; //传输的是 ArrayBuffer 类型的数据
    ws.onopen = function() {
        console.log('握手成功');
        if (ws.readyState == 1) { //ws进入连接状态，则每隔500毫秒发送一包数据
            record.start();
        }
        id = guid()
        console.log(id)
        timestamp=new Date().getTime().toString();
        // alert(typeof(timestamp))

        ws.send("start*"+getAudioInfo()+"");
        getWebSocketResult()
    };

    ws.onmessage = function(msg) {
        console.info(msg)
    };

    ws.onerror = function(err) {
        console.info(err)
    }
}


function getWebSocketResult() {
    ws1 = new WebSocket("ws://localhost:8000/demo/asr/getResult");
    ws1.binaryType = 'arraybuffer'; //传输的是 ArrayBuffer 类型的数据
    ws1.onopen = function() {
        console.log('握手成功1');
        console.log(id)

        ws1.send(getAudioInfo());
    };

    ws1.onmessage = function(msg) {
        console.info(msg.data);
        var content = document.getElementById("content");
        // content.innerHTML += msg.data + "<br>"
        content.innerHTML =  msg.data  +"<br>"  + content.innerHTML+"<br>"

    };

    ws1.onerror = function(err) {
        console.info(err)
    }
}

/*
* 开始对讲
*/
begin.onclick = function() {
    navigator.getUserMedia = navigator.getUserMedia || navigator.webkitGetUserMedia;
    if (!navigator.getUserMedia) {
        alert('浏览器不支持音频输入');
    } else {
        navigator.getUserMedia({
        audio: true
    },
    function(mediaStream) {
        init(new Recorder(mediaStream));
        console.log('开始对讲');
        useWebSocket();
    },
    function(error) {
        console.log(error);
        switch (error.message || error.name) {
            case 'PERMISSION_DENIED':
            case 'PermissionDeniedError':
                console.info('用户拒绝提供信息。');
                break;
            case 'NOT_SUPPORTED_ERROR':
            case 'NotSupportedError':
                console.info('浏览器不支持硬件设备。');
                break;
            case 'MANDATORY_UNSATISFIED_ERROR':
            case 'MandatoryUnsatisfiedError':
                console.info('无法发现指定的硬件设备。');
                break;
                default:
                console.info('无法打开麦克风。异常信息:' + (error.code || error.name));
                break;
                }
            }
        )
    }
}

/*
* 关闭对讲
*/
end.onclick = function() {
    if (ws) {
        ws.close();
        record.stop();
        console.log('关闭对讲以及WebSocket');
    }
    if(ws1) {
        ws1.close();
    }
}
