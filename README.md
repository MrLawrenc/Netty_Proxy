# Netty_Proxy
基于netty实现的http请求内网穿透

## 本机测试:
### client
-h localhost -p 9527 -password 123lmy -proxy_h localhost -proxy_p 8080 -remote_p 10000

### server
-p 9000 -password 123lmy

在本机访问本地的web应用:localhost:10000

## 公网测试:
### client
-h 公网ip -p 公网服务端端口 -password 123lmy -proxy_h localhost -proxy_p 代理的本地服务端口 -remote_p 公网开放访问本地服务的端口 
### server
-p 公网服务端端口 -password 123lmy

任意主机访问内网的(即安装客户端的主机)web应用:  公网ip:公网开放访问本地服务的端口  即可访问


### 心跳机制说明

由于客户端和服务端共用同一个超时机制，如果读写超时同时存在，就必须设置读超时大于写超时。


