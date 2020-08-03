# 如何调用百度python sdk
## 一.导入百度asr包

```python
from baidu_acu_asr.asr_client import AsrClient
```
## 二.js持续从网页获得语音流发送给python
```javascript
demo/static/demo/js/speech.js
```
## 三.python从页面持续的获得流加到缓存
```python
@accept_websocket
def test_websocket(request):
    with open('test.pcm', 'wb') as f:
        if request.is_websocket():  # 如果请求是websocket请求：
            header_adder_interceptor = header_manipulator_client_interceptor.header_adder_interceptor(
                'audio_meta', base64.b64encode(client.request.SerializeToString()))
            with grpc.insecure_channel(host) as channel:
                for message in request.websocket:
                    try:
                        ## 这里将页面传来的流不断的加到缓存或redis，等待后续去持续的读
                    except Exception as e:
                        logging.error("error:",e)
```

```python
client = AsrClient(url, port, AsrProduct.CUSTOMER_SERVICE_STOCK, enable_flush_data,
                   log_level=log_level,
                   product_id=product_id,
                   enable_chunk=True,
                   # product= AsrProduct.CUSTOMER_SERVICE_FINANCE,
                   sample_rate=8000,
                   user_name=user_name,
                   password=password)

```
## 四.python持续读取缓存去生成百度语音流
```python
def make_stream_from_cache():
    ## 这里可以从redis或者cache读取bytes去不断的生成流

```
## 五.python持续得从百度语音流获得结果
```python
def get():
    header_adder_interceptor = header_manipulator_client_interceptor.header_adder_interceptor(
        'audio_meta', base64.b64encode(client.request.SerializeToString()))
    with grpc.insecure_channel(target=host, options=[('grpc.keepalive_timeout_ms', 1000000), ]) as channel:
        intercept_channel = grpc.intercept_channel(channel,
                                                   header_adder_interceptor)
        stub = audio_streaming_pb2_grpc.AsrServiceStub(intercept_channel)
        ### 从cache去获得流
        responses = stub.send(make_stream_from_java_cache(), timeout=100000)
        for response in responses:
            yield response
```


# 安装启动demo项目
## 安装
pip3 install -r requirements.txt 

## 启动
pthon3 manage.py runserver 0.0.0.0:8000


## 打开网页
http://127.0.0.1:8000/demo/asr
### 点击开始讲话后说话即可


### 返回的结果不太准确时，怀疑需要调节AsrProduct，和demo/views.py中的
##### read_bytes_len = 3000
##### read_cache_sleep_time = 0.01
来调节
