import struct

from baidu_acu_asr import header_manipulator_client_interceptor, audio_streaming_pb2_grpc,audio_streaming_pb2
from django.http import HttpResponse
from django.shortcuts import render
import base64
import grpc
from dwebsocket.decorators import accept_websocket, require_websocket
from baidu_acu_asr.asr_client import AsrClient
from baidu_acu_asr.asr_product import AsrProduct
import time
import logging
# from django_redis import get_redis_connection
# cache = get_redis_connection("default")
import baidu_acu_asr.audio_streaming_pb2



url="10.153.205.20"
port="8051"
user_name="zhengxin1890"
password="zhengxin1890"

save_radio = True
save_file = "test.pcm"
log_level = 0
send_per_seconds = 0.16
sleep_ratio = 0.7
enable_chunk=True
product_id = AsrProduct.CUSTOMER_SERVICE_STOCK
enable_flush_data = True

sample_rate = 8000
java_cache = {}
read_bytes_len = 3000
read_cache_sleep_time = 0.01
host = url + ":" + port
client = AsrClient(url, port, AsrProduct.CUSTOMER_SERVICE_STOCK, enable_flush_data,
                   log_level=log_level,
                   product_id=product_id,
                   enable_chunk=True,
                   # product= AsrProduct.CUSTOMER_SERVICE_FINANCE,
                   sample_rate=8000,
                   user_name=user_name,
                   password=password)

## demo页面
def asrPage(request):
    return render(request,"asr/ws2.html")

## websocket入口
@accept_websocket
def test_websocket(request):
    with open(save_file, 'wb') as f:
        if request.is_websocket():  # 如果请求是websocket请求：
            for message in request.websocket:
                try:
                    if message =="start".encode():
                        java_cache["test"] = bytes()
                        continue
                    b = java_cache.get("test")
                    java_cache["test"] = b + message
                    if save_radio and message != "start".encode():
                        f.write(message)
                except Exception as e:
                    logging.error("error:",e)

## 获得结果的入口
@accept_websocket
def get_result(request):
    responses = test_java_cache()
    try:
        for response in responses:
            if response.type == baidu_acu_asr.audio_streaming_pb2.FRAGMENT_DATA:
                request.websocket.send(response.audio_fragment.result.encode())
                logging.info("%s\t%s\t%s\t%s",
                             response.audio_fragment.start_time,
                             response.audio_fragment.end_time,
                             response.audio_fragment.result,
                             response.audio_fragment.serial_num)
            else:
                logging.warning("type is: %d", response.type)
    except Exception as e:
        logging.error("error",e)
        time.sleep(0.5)
## java_cache
def test_java_cache():
    header_adder_interceptor = header_manipulator_client_interceptor.header_adder_interceptor(
        'audio_meta', base64.b64encode(client.request.SerializeToString()))
    with grpc.insecure_channel(target=host, options=[('grpc.keepalive_timeout_ms', 1000000), ]) as channel:
        intercept_channel = grpc.intercept_channel(channel,
                                                   header_adder_interceptor)
        stub = audio_streaming_pb2_grpc.AsrServiceStub(intercept_channel)
        ### 获得文件16k_test.pcm的结果,AsrProduct.INPUT_METHOD
        #### responses = stub.send(client.generate_file_stream("16k_test.pcm"), timeout=100000)
        ### 从cache去获得流
        responses = stub.send(make_stream_from_java_cache(), timeout=100000)
        for response in responses:
            yield response

## 持续去读cache生成流的请求
def make_stream_from_java_cache():
    i = 0
    u = read_bytes_len
    res = java_cache.get("test")[i:u]
    while True:
        if len(res) >=read_bytes_len:
            i +=1
            yield audio_streaming_pb2.AudioFragmentRequest(audio_data=res)
        res = java_cache.get("test")[i*u:u*(i+1)]
        time.sleep(read_cache_sleep_time)

#
#
# ####### 下面使用redis方式
# ## 获得redis中流的结果
# def get_redis_result():
#     header_adder_interceptor = header_manipulator_client_interceptor.header_adder_interceptor(
#         'audio_meta', base64.b64encode(client.request.SerializeToString()))
#     with grpc.insecure_channel(target=host, options=[('grpc.keepalive_timeout_ms', 1000000), ]) as channel:
#         intercept_channel = grpc.intercept_channel(channel,
#                                                    header_adder_interceptor)
#         stub = audio_streaming_pb2_grpc.AsrServiceStub(intercept_channel)
#         responses = stub.send(make_stream_from_redis(), timeout=100000)
#         print(responses)
#         for response in responses:
#             yield response
#
#
# ## 持续去读redis生成流的请求
# def make_stream_from_redis():
#     i = 0
#     u = 2000
#     res = cache.get("test")[i:u]
#     while True:
#         if len(res) >0:
#             i +=1
#         #     print("当前的i值：%s，当前的res长度： %s" %(i,len(res)))
#             yield audio_streaming_pb2.AudioFragmentRequest(audio_data=res)
#             res = cache.get("test")[i*u:u*(i+1)]
#             time.sleep(0.01)
#
#
# # 存入redis的websocket入口
# @accept_websocket
# def redis_websocket(request):
#     print("demo")
#     with open('test.pcm', 'wb') as f:
#         if request.is_websocket():  # 如果请求是websocket请求：
#             header_adder_interceptor = header_manipulator_client_interceptor.header_adder_interceptor(
#                 'audio_meta', base64.b64encode(client.request.SerializeToString()))
#             with grpc.insecure_channel(host) as channel:
#                 for message in request.websocket:
#                     try:
#                         if message =="start".encode():
#                             message=bytes()
#                             cache.set("test",bytes())
#                         b = cache.get("test")
#                         if b:
#                             cache.set("test",b+message)
#                         else:
#                             cache.set("test",message)
#                         if save_radio and message != "start".encode():
#                             f.write(message)
#
#                     except Exception as e:
#                         logging.error("error:",e)
#
#
#
