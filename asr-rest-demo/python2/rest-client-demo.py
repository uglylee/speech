# -*- coding: utf-8 -*-
import base64,os,json,urllib2

__author__ = 'uglylee'

def file2Base64(file_path):
    with open(file_path, 'rb') as f:
        data = f.read()
        return base64.b64encode(data).decode("utf-8")

def send_urllib(params):
    opener = urllib2.build_opener()
    rs = opener.open(url,json.dumps(params)).read()
    return rs

def make_send_params():
    b64audio = file2Base64(file_path)
    return {
        "pid": pid,
        "format":format,
        "rate":rate,
        "cuid":cuid,
        "audiolen":str(os.path.getsize(file_path)),
        "b64audio":b64audio,
        "callback":callback
    }

def main():
    try:
        params = make_send_params()
        res = send_urllib(params)
        print res
    except Exception as e:
        print str(e)


if __name__ == '__main__':
    rate = "8000"
    format = "wav"
    pid = "10000" ## 普通话
    cuid = "testcuid"
    file_path = "../data/10s.wav"
    callback = "http://127.0.0.1:50001/res"
    url = "http://10.136.238.22:9001/shortime/speech/recognition"
    main()
