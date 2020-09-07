from django.urls import path

from . import views

urlpatterns = [

    path('asr/getResult',views.get_result,name='getResult'),
    path('asr/', views.asrPage,name='asrPage'),
    path('test_websocket', views.test_websocket, name='test_websocket'),

]
