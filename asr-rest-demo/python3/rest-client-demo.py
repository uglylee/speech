# -*- coding: utf-8 -*-
import requests,base64,os,json
from enum import Enum, unique

__author__ = 'uglylee'

def file2Base64(file_path):
    """
    :param file_path: 文件路径
    :return: 文件的base64
    """
    with open(file_path, 'rb') as f:
        data = f.read()
        return base64.b64encode(data).decode("utf-8")

@unique
class Format(Enum):
    """
    音频格式,支持:PCM(原始语音流)、WAV(不压缩、pcm 编码)、AMR、SPEEX、ALAW、OPUS
    :return:
    """
    Pcm = "pcm"
    Wav = "wav"
    Amr = "amr"
    Speex = "speex"
    Alaw = "alaw"
    Opus = "opus"

@unique
class Rate(Enum):
    """
    采样率: 8000 / 16000
    :return:
    """
    Rate8k = "8000"
    Rate16k = "16000"

def send_request(params):
    """
    请求rest api接口
    :param params: make_send_params()
    :return: {"capacity":99,"data":[{"confident":100,"word":"这个它让我登录"}],"errcode":0,"errmsg":"success","logid":"396582812394261211","sn":"d4f0f360-314c-4321-b105-8a4500158f81"}
    """
    return requests.post(url=url, headers={'Content-Type': 'application/json'}, data=json.dumps(params))

def make_send_params():
    b64audio = file2Base64(file_path)
    return {
        "pid": pid, ## 用于标识语言及模型选择。对于 RESTAPI 服务依赖此参数进行识别请
        "format":format, ## 标识输入音频压缩格式,目前支持音 频编码格式如下:pcm,wav,amr,speex,opus,alaw
        "rate":rate,## 标识输入音频采样率，需与声学模型 匹配，可选如下8000/16000
        "cuid":cuid, ## 用户唯一标识，用来区分用户，后续 可基于此统计 UV 数据。长度 64 字符 以内。
        "audiolen":str(os.path.getsize(file_path)), ## 输入音频原始字节数
        "b64audio":b64audio ## 原始音频通过 base64 编码后内容， base64 编码时采用单行模式。
        # "callback":"",
                        ##回调 url 只能是 https 或 http 协议，
                        #  url 必须包含协议头例如 http://... 如果省略协议头，
                        # 例如 baidu.com 这 种形式则可能引起报错。
                        # 如请求参数 中包含 callback url，则完成识别后 通过回调 url 将识别结果 POST 方式 回传。
                        #服务接受请求后立刻返回响 应，其中包含 logid、sn 可作为后续 callback 回传结果关联信息。否则调 用侧同步等待识别完成 response 返 回结果。
    }

def main():
    ## 从文件生成请求的json
    try:
        params = make_send_params()
        ## 发送请求至rest api url
        res = send_request(params)
        print(res.text)
    except Exception as e:
        print(str(e))


if __name__ == '__main__':
    # 格式支持:PCM(原始语音流)、WAV(不压缩、pcm 编码)、AMR、SPEEX、ALAW、OPUS
    # 采样率: 8000 / 16000
    # 位深:16 bits
    # 声道: 单声道道
    rate = Rate.Rate8k.value
    format = Format.Wav.value
    pid = "10000" ## 普通话
    cuid = "testcuid"
    file_path = "../data/10s.wav"
    url = "http://10.136.238.22:9001/shortime/speech/recognition"
    main()

