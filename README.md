# Netty_Proxy

- 当前2.0.0版本更新如下
  - 修复之前开启一天左右会出现**断连**的情况
  - 增加客户端断开之后的**重连**机制
  - 支持客户端的**一对多**，一个客户端可以绑定多个代理客户端
  - 优化**资源关闭**机制
  - 弃用jdk的log框架，引入slf4j

## Server

启动命令:

```shell
java -jar server.jar -port 9527 -password 123lmy
```

或者简写:

```shell
java -jar server.jar -p 9527 -pwd 123lmy
```

- p     &emsp;  &emsp;  &emsp;     服务端端口
- pwd  &emsp;  &emsp; 服务端token校验

## Client

### 模式说明

- 一对一

  一个客户端对应一个代理客户端，通过指定相关参数启动，启动方式如下：

  ```shell
  java -jar client.jar -host 服务端host -port 服务端端口 -password 连接服务端的密码 -proxy_host 被代理的服务host（通常在当前客户端环境localhost） -proxy_port 被代理服务的端口 -remote_port 需要外网服务端暴露访问被代理服务的端口
  ```

- 一对多

  一个客户端对应多个代理客户端，通过指定配置文件(最好指定绝对路径)的方式启动，启动方式如下

  ```shell
  java -jar -profile E:\Netty_Proxy\client.pro
  ```

  若是都在本地测试，则配置文件格式如下

  ```properties
  # 外网服务器地址
  host=localhost
  # 外网服务器端口
  port=9000
  # 外网服务器所需要的密码
  password=123lmy
  
  # 需要被代理的服务器地址，支持 [host1,host2,host3] 和 host,host2,host3 两种格式
  proxyHost=localhost,localhost
  # 需要被代理的服务器端口，格式如上
  proxyPort=8899,9988
  
  # 访问内网被代理服务所暴露的外网端口，格式如上
  remotePort=11000,12000
  ```

### 代理http服务:

启动命令:

```shell
java -jar client.jar -host 47.96.158.922 -port 9527 -password 123lmy -proxy_host localhost -proxy_port 8080 -remote_port 12135
```

或者简写: 

```shell
java -jar client.jar -h 47.96.158.922 -p 9527 -pwd 123lmy -proxy_h localhost -proxy_p 8080 -remote_p 12135
```

- h     &emsp;  &emsp;  &emsp;     服务端ip地址，可以是阿里云
- p &emsp;  &emsp;  &emsp;     服务端端口，如果是阿里云需要带控制台暴露该端口
- pwd  &emsp;  &emsp; 服务端token校验
- proxy_h &emsp;内网被代理的服务ip，因为客户端在内网，所以一般为localhost
- proxy_p &emsp;内网被代理的服务端口
- remote_p&ensp;需要公网，即服务端暴露访问内网应用的端口

若一个客户端需要代理多个服务则使用**一对多模式**

```shell
java -jar -profile  配置文件绝对路径
```

配置如下

```properties
# 外网服务器地址
host=47.96.158.922
# 外网服务器端口
port=9527
# 外网服务器所需要的密码
password=123lmy

# 需要被代理的服务器地址，支持 [host1,host2,host3] 和 host,host2,host3 两种格式
proxyHost=localhost,localhost
# 需要被代理的服务器端口，格式如上
proxyPort=8899,9988

# 访问内网被代理服务所暴露的外网端口，格式如上
remotePort=11000,12000
```

当前模式则使用客户端开启了两个内网服务的代理，分别是 localhost:8899 和 localhost:9988分别对应外网暴露的 11000和12000两个端口。

### 代理ssh服务:

```shell
java -jar client.jar -h 47.96.158.922 -p 9527 -password 123lmy -proxy_h localhost -proxy_p 22 -remote_p 12222
```

- proxy_p 这是内网ssh连接端口，一般默认是22
- remote_p 服务端暴露访问ssh的端口

```java
如使用ssh连接工具输入以下即可连接成功:
	ip: 47.96.158.922
	port:12222
	username:xxx
	password:xxx
```

## 本地测试

### server:

```shell
 java -jar server.jar -port 9000 -password 123lmy
```

### client:

```shell
 java -jar client.jar -profile E:\Netty_Proxy\client.pro
```

配置文件如下

```properties
# 外网服务器地址
host=localhost
# 外网服务器端口
port=9000
# 外网服务器所需要的密码
password=123lmy

# 需要被代理的服务器地址
proxyHost=localhost,localhost
# 需要被代理的服务器端口
proxyPort=8899,9988

# 访问内网被代理服务所暴露的外网端口
remotePort=11000,12000
```

---

### 测试

- 本地启动两个web服务，端口分别为8899,9988

- 分别访问 **localhost:11000** 和 **localhost:12000**
- 若有正常结果返回，则证明代理成功

## 附

#### Linux启动脚本例子
```shell script
服务端 nohup java -jar server-2.0.0.jar -port 9527 -password 123lmy >>server.log 2>&1  &
客户端 nohup java -jar client-2.0.0.jar -h 服务端地址 -p 9527 -password 123lmy -proxy_h localhost -proxy_p 8080 -remote_p 11000 >>client.log 2>&1  &
```

#### 压力测试

- 本地测试服务端，客户端
- 配置jemeter压测参数，线程数从10-1000，均为1s内发送完成
- 服务端内存占用范围（18-80m），客户端（40-200m）

#### 后期优化

- netty各端线程数控制，尽量共享EventLoopGroup
- 资源控制

#### 其他(Jmeter生成性能测试报告)
- 使用创建好的jmx文件
- 进入jmeter bin目录
- 执行
```cmd
jmeter -n -t F:\JavaProject\Netty_Proxy\Netty_Proxy.jmx  -l test.jtl -e -o ./netty_proxy
```
- 最终会使用Netty_Proxy.jmx生成test.jtl源文件，再根据源文件在./netty_proxy目录下生成可视化的测试报告，打开index.html即可


---
- jmeter注意事项
- 出现 org.apache.http.NoHttpResponseException 一般是jmeter问题
    - 需要更改jmeter参数 bin下面的配置文件
    ```xml
      # 10s 
      httpclient4.idletimeout=10000
      httpclient4.retrycount=3
    ``` 
    - 循环4次 每次50个请求  在客户端会发现激活的客户端大于200，可能是由于服务器端的负载过高以及网络带宽占满等因素导致响应延迟加长，
    而Jmeter默认没有timout等待，导致出错。解决方案:
    
  - 务必勾选长连接
  
  - 压测可能会出现句柄占用耗尽，可以参考突破句柄限制
