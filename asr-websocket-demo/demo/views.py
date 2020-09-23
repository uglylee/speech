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
from django.conf import settings
import json


# url = settings.URL
# port = settings.PORT
user_name = settings.USER_NAME
password= settings.PASSWORD
save_radio = settings.SAVE_RADIO
save_file = settings.SAVE_FILE
log_level = settings.LOG_LEVEL
send_per_seconds = settings.SEND_PER_SECONDS
sleep_ratio = settings.SLEEP_RATIO
enable_chunk = settings.ENABLE_TRUNK
# product_id = AsrProduct.CUSTOMER_SERVICE_STOCK
enable_flush_data = settings.ENABLE_FLUSH_DATA
sample_rate = settings.SAMPLE_RATE
read_bytes_len = settings.READ_BYTES_LEN
read_cache_sleep_time = settings.READ_CACHE_SLEEP_TIME

# host = url + ":" + port
content = {}
local_cache = {}
def getProduct(product_id):
    product = {
        "1903": AsrProduct.CUSTOMER_SERVICE,
        "1904": AsrProduct.CUSTOMER_SERVICE_TOUR,
        "1905": AsrProduct.CUSTOMER_SERVICE_STOCK,
        "1906": AsrProduct.CUSTOMER_SERVICE_FINANCE,
        "1907": AsrProduct.CUSTOMER_SERVICE_ENERGY,
        "888": AsrProduct.INPUT_METHOD,
        "1888": AsrProduct.FAR_FIELD,
        "1889": AsrProduct.FAR_FIELD_ROBOT,
        "1": AsrProduct.CHONGQING_FAYUAN,
        "1912": AsrProduct.SPEECH_SERVICE
    }
    return product[product_id]
product_id = getProduct(settings.PRODUCT_ID)

def getBoolean(key):
    return True if ("true" in key) or ("True" in key) else False


def getClient(info):
    return AsrClient(
        info.get("url"),info.get("port"),getProduct(info.get("product_id",product_id)), getBoolean(info.get("enable_flush_data")),
                       log_level=log_level,
                       product_id=getProduct(info.get("product_id")),
                       enable_chunk=getBoolean(info.get("enable_chunk")),
                       sample_rate=info.get("sample_rate"),
                       user_name=info.get("user_name"),
                       password=info.get("password"))

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
    user = None
    info = None
    if request.is_websocket():
        pcm_file = None

        for message in request.websocket:
            try:
                if message!= None and message.startswith("start".encode()):
                    info = json.loads(message.decode().split("*")[1])
                    user = info.get("id")
                    local_cache[user] = bytes()
                    if getBoolean(info.get("save_file")):
                        info["enable_flush_data"] = "false"
                    local_cache[user+"_client"] = getClient(info)
                    if getBoolean(info.get("save_radio")):
                        pcm_file_name = "audio/" + user + "_" + info["timestamp"] + ".pcm"
                        pcm_file = open(pcm_file_name, 'wb')
                    continue
                b = local_cache.get(user)
                local_cache[user] = b + message
                if getBoolean(info.get("save_radio")):
                    pcm_file.write(message)
            except Exception as e:
                logging.error("error: " + str(e))
                pcm_file.close()

        pcm_file.close()

@accept_websocket
def get_result(request):
    """
    持续获得百度语音流结果发到页面
    :param params: request
    :return:
    """
    info = None
    if request.is_websocket():
        for message in request.websocket:
            info = json.loads(message.decode())
            break

    responses = test_local_cache(info)
    try:

        for response in responses:
            if response.type == baidu_acu_asr.audio_streaming_pb2.FRAGMENT_DATA:
                request.websocket.send(response.audio_fragment.result.encode())
                logging.info("%s\t%s\t%s\t%s",
                             response.audio_fragment.start_time,
                             response.audio_fragment.end_time,
                             response.audio_fragment.result,
                             response.audio_fragment.serial_num)
                if getBoolean(info.get("save_file")):
                    result_file_name = "res/" + info["id"] + "_" + info["timestamp"] + ".txt"
                    with open(result_file_name, "a+") as result_file:

                        result_file.write(response.audio_fragment.result + "\n")
            else:
                logging.warning("type is: %d", response.type)

    except Exception as e:
        logging.error("error",e)
        time.sleep(0.5)


def test_local_cache(info):
    """
    从百度语音流获得结果
    :param params: make_stream_from_local_cache()
    :return:
    """
    client = local_cache[info.get("id") + "_client"]

    header_adder_interceptor = header_manipulator_client_interceptor.header_adder_interceptor(
        'audio_meta', base64.b64encode(client.request.SerializeToString()))

    with grpc.insecure_channel(target=info.get("url") + ":" + info.get("port"), options=[('grpc.keepalive_timeout_ms', 1000000), ]) as channel:
        intercept_channel = grpc.intercept_channel(channel,
                                                   header_adder_interceptor)
        stub = audio_streaming_pb2_grpc.AsrServiceStub(intercept_channel)
        responses = stub.send(make_stream_from_local_cache(info.get("id")), timeout=100000)
        for response in responses:
            yield response




def make_stream_from_local_cache(user):
    """
    从本地cache持续生成百度request
    :param params:
    :return:
    """
    i = 0
    u = read_bytes_len
    res = local_cache.get(user)[i:u]
    while True:
        if len(res) >= read_bytes_len:
            i +=1
            yield audio_streaming_pb2.AudioFragmentRequest(audio_data=res)
        res = local_cache.get(user)[i*u:u*(i+1)]
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
