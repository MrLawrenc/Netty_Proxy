# Netty_Proxy
基于netty实现的http请求内网穿透

### client
-H localhost -p 9527 -password 123lmy -proxy_h localhost -proxy_p 8080 -remote_p 10000

### server
-p 9000 -password 123lmy

### 心跳机制说明

由于客户端和服务端共用同一个超时机制，如果读写超时同时存在，就必须设置读超时大于写超时。

