from baidu_acu_asr import header_manipulator_client_interceptor, audio_streaming_pb2_grpc,audio_streaming_pb2
from django.shortcuts import render
import base64
import grpc
from dwebsocket.decorators import accept_websocket
from baidu_acu_asr.asr_client import AsrClient
from baidu_acu_asr.asr_product import AsrProduct
import time
import logging
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
local_cache = {}
read_bytes_len = 3000
read_cache_sleep_time = 0.01
host = url + ":" + port

client = AsrClient(url, port, product_id, enable_flush_data,
                   log_level=log_level,
                   product_id=product_id,
                   enable_chunk=enable_chunk,
                   sample_rate=sample_rate,
                   user_name=user_name,
                   password=password)

def asrPage(request):
    """
    返回demo页面
    :param params: request
    :return: demo页面
    """
    return render(request,"asr/ws2.html")

@accept_websocket
def test_websocket(request):
    """
    从js将流持续加到本地cache
    :param params: request
    :return:
    """
    if request.is_websocket():
        with open(save_file, 'wb') as f:
            for message in request.websocket:
                try:
                    if message =="start".encode():
                        local_cache["test"] = bytes()
                        continue
                    b = local_cache.get("test")
                    local_cache["test"] = b + message
                    if save_radio:
                        f.write(message)
                except Exception as e:
                    logging.error("error:",e)

@accept_websocket
def get_result(request):
    """
    持续获得百度语音流结果发到页面
    :param params: request
    :return:
    """
    responses = test_local_cache()
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

def test_local_cache():
    """
    从百度语音流获得结果
    :param params: make_stream_from_local_cache()
    :return:
    """
    header_adder_interceptor = header_manipulator_client_interceptor.header_adder_interceptor(
        'audio_meta', base64.b64encode(client.request.SerializeToString()))
    with grpc.insecure_channel(target=host, options=[('grpc.keepalive_timeout_ms', 1000000), ]) as channel:
        intercept_channel = grpc.intercept_channel(channel,
                                                   header_adder_interceptor)
        stub = audio_streaming_pb2_grpc.AsrServiceStub(intercept_channel)
        responses = stub.send(make_stream_from_local_cache(), timeout=100000)
        for response in responses:
            yield response

def make_stream_from_local_cache():
    """
    从本地cache持续生成百度request
    :param params:
    :return:
    """
    i = 0
    u = read_bytes_len
    res = local_cache.get("test")[i:u]
    while True:
        if len(res) >= read_bytes_len:
            i +=1
            yield audio_streaming_pb2.AudioFragmentRequest(audio_data=res)
        res = local_cache.get("test")[i*u:u*(i+1)]
        time.sleep(read_cache_sleep_time)




#
#
# from django.core.cache import cache #引入缓存模块
#
# @accept_websocket
# def test_websocket(request):
#     """
#     从js将流持续加到redis
#     :param params: request
#     :return:
#     """
#     with open(save_file, 'wb') as f:
#         if request.is_websocket():  # 如果请求是websocket请求：
#             for message in request.websocket:
#                 try:
#                     if message =="start".encode():
#                         cache.set("test",bytes)
#                         continue
#                     b = cache.get("test")
#                     cache.set("test",b + message)
#                     if save_radio:
#                         f.write(message)
#                 except Exception as e:
#                     logging.error("error:",e)
#
# @accept_websocket
# def get_result(request):
#     """
#     持续获得百度语音流结果发到页面
#     :param params: request
#     :return:
#     """
#     responses = test_redis_cache()
#     try:
#         for response in responses:
#             if response.type == baidu_acu_asr.audio_streaming_pb2.FRAGMENT_DATA:
#                 request.websocket.send(response.audio_fragment.result.encode())
#                 logging.info("%s\t%s\t%s\t%s",
#                              response.audio_fragment.start_time,
#                              response.audio_fragment.end_time,
#                              response.audio_fragment.result,
#                              response.audio_fragment.serial_num)
#             else:
#                 logging.warning("type is: %d", response.type)
#     except Exception as e:
#         logging.error("error",e)
#         time.sleep(0.5)
#
# def test_redis_cache():
#     """
#     从百度语音流获得结果
#     :param params: make_stream_from_redis_cache()
#     :return:
#     """
#     header_adder_interceptor = header_manipulator_client_interceptor.header_adder_interceptor(
#         'audio_meta', base64.b64encode(client.request.SerializeToString()))
#     with grpc.insecure_channel(target=host, options=[('grpc.keepalive_timeout_ms', 1000000), ]) as channel:
#         intercept_channel = grpc.intercept_channel(channel,
#                                                    header_adder_interceptor)
#         stub = audio_streaming_pb2_grpc.AsrServiceStub(intercept_channel)
#         ### 获得文件16k_test.pcm的结果,AsrProduct.INPUT_METHOD
#         #### responses = stub.send(client.generate_file_stream("16k_test.pcm"), timeout=100000)
#         ### 从cache去获得流
#         responses = stub.send(make_stream_from_redis_cache(), timeout=100000)
#         for response in responses:
#             yield response
#
# def make_stream_from_redis_cache():
#     """
#     从redis持续生成百度request
#     :param params:
#     :return:
#     """
#     i = 0
#     u = read_bytes_len
#     res = cache.get("test")[i:u]
#     while True:
#         if len(res) >= read_bytes_len:
#             i +=1
#             yield audio_streaming_pb2.AudioFragmentRequest(audio_data=res)
#         res = cache.get("test")[i*u:u*(i+1)]
#         time.sleep(read_cache_sleep_time)
#
