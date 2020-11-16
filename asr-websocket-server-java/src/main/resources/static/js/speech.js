var begin = document.getElementById('intercomBegin');
var end = document.getElementById('intercomEnd');
var box =  document.getElementById('box');
var ws = null; //实现WebSocket
var record = null; //多媒体对象，用来处理音频
var ws1 = null;
var id=null;
var timestamp=null;

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
                        // setTimeout(function () {
                        //     //内容30ms后执行
                        // }, 10);
                        ws.send(tmparr);
                        if (arr.byteLength - i - 1 >= 1024) {
                            tmparr = new Int8Array(1024);
                        } else {
                            tmparr = new Int8Array(arr.byteLength - i - 1);
                        }
                        j = 0;
                    }
                    if ((i + 1 == arr.byteLength) && ((i + 1) % 1024) != 0) {
                        // setTimeout(function () {
                        //     //内容30ms后执行
                        // }, 10);
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


/*
* WebSocket
*/



function useWebSocket() {
    id = guid();
    ws = new WebSocket("ws://localhost:8080/wbserver/"+id+"");
    ws.binaryType = 'arraybuffer'; //传输的是 ArrayBuffer 类型的数据
    ws.onopen = function() {
        console.log('握手成功');
        if (ws.readyState == 1) { //ws进入连接状态，则每隔毫秒发送一包数据
            record.start();
        }
    };

    ws.onmessage = function(msg) {
        console.log(msg);
        box.innerHTML =  msg.data  +"<br>"  + box.innerHTML+"<br>"

    };

    ws.onerror = function(err) {
        console.info(err)
    };
    ws.onclose = function (ev) {
        console.log("close")
    }
}


/*
* 开始对讲
*/
begin.onclick = function() {
    navigator.getUserMedia = navigator.mediaDevices.getUserMedia;
    // navigator.getUserMedia = navigator.getUserMedia || navigator.webkitGetUserMedia;

    if (navigator.mediaDevices.getUserMedia) {
        const constraints = { audio: true };
        navigator.mediaDevices.getUserMedia(constraints).then(
            stream => {
            init(new Recorder(stream));
        console.log('开始对讲');
        useWebSocket();
        console.log("授权成功！");
    },
        () => {
            console.error("授权失败！");
        }
    );
    } else {
        console.error("浏览器不支持 getUserMedia");
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

};
