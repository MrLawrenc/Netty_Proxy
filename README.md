





# Netty_Proxy

- 当前2.1.3版本更新如下
  - 主要处理client和server的oom异常

## 服务端和客户端启动配置

### Server

启动命令:

```shell
java -jar server.jar -port 9527 -password 123456
```

或者简写:

```shell
java -jar server.jar -p 9527 -pwd 123456
```

- p     &emsp;  &emsp;  &emsp;     服务端端口
- pwd  &emsp;               连接服务端token

### Client

##### 模式说明

- 一对一（不建议使用）

  一个客户端对应一个代理客户端，通过指定相关参数启动，启动方式如下：

  ```shell
  java -jar client.jar -host 服务端host -port 服务端端口 -password 连接服务端的密码 -proxy_host 被代理的服务host（通常在当前客户端环境localhost） -proxy_port 被代理服务的端口 -remote_port 需要外网服务端暴露访问被代理服务的端口
  ```

  - h     &emsp;  &emsp;  &emsp;     服务端ip地址，可以是阿里云
  - p                         服务端端口，如果是阿里云需要在控制台开放该端口给代理客户端连接
  - pwd                    服务端token校验
  - proxy_h             内网被代理的服务ip，因为客户端在内网，所以一般为localhost
  - proxy_p             内网被代理的服务端口
  - remote_p          需要公网，即服务端暴露访问内网应用的端口

- 一对多（建议使用）

  一个客户端对应多个代理客户端，通过指定配置文件(最好指定绝对路径)的方式启动，启动方式如下

  ```shell
  java -jar -profile E:\Netty_Proxy\client.pro
  ```

  若是都在本地测试，则配置文件格式如下

  ```properties
  #################################以下为连接基础配置#################################
  # 外网代理服务器地址
  host=localhost
  # 外网代理服务器启动端口
  port=9000
  # 连接代理服务器所需要的密码
  password=123456
  #################################以下为代理客户端配置#################################
  # 需要被代理的服务器地址，通常和代理客户端处在同一台机器，即localhost，支持 [host1,host2,host3] 和 host,host2,host3 两种格式
  proxyHost=localhost,localhost
  # 需要被代理的服务端口，格式如上
  proxyPort=8899,9988
  
  
  # 需要代理服务端暴露的外网端口，可以通过该端口访问到被代理的内网服务，格式如上
  remotePort=11000,12000
  ```

## 代理http服务示例

#### 代理服务端启动

```shell
java -jar server.jar -port 9527 -password 123456
```

#### 代理客户端启动

```shell
java -jar -profile  /data/client.pro
```

/data/client.pro配置如下(目前只支持绝对路径)

```properties
# 代理服务器配置
host=47.96.158.922
port=9527
password=123456
# 假设被代理的是两个本地http服务
proxyHost=localhost,localhost
proxyPort=8899,9988
# 代理服务器开放访问本地http服务的端口
remotePort=11000,12000
```

启动代理客户端之后分别在端口8899和9988启动两个需要被代理的Http服务，之后通过**47.96.158.922:11000**或者**47.96.158.922:12000**即可访问对于的被代理Http服务。

## 代理ssh服务示例

#### 代理服务端启动

```shell
java -jar server.jar -port 9527 -password 123456
```

#### 代理客户端端启动

```shell
java -jar client.jar -h 47.96.158.922 -p 9527 -password 123456 -proxy_h localhost -proxy_p 22 -remote_p 12222
```

- proxy_p 这是内网ssh连接端口，一般默认是22
- remote_p 服务端暴露访问ssh的端口

输入以下信息连接被代理的ssh服务测试

```cmd
ip: 47.96.158.922
port:12222
username:xxx
password:xxx
```

## 本地测试示例配置

### server:

```shell
 java -jar server.jar -port 9000 -password 123456
```

### client:

```shell
 java -jar client.jar -profile E:\Netty_Proxy\client.pro
```

如使用IDEA启动，则在Program arguments加入**-profile F:\JavaProject\Netty_Proxy\client.pro**启动参数即可

配置文件如下（项目工程根目录有本地示例配置）

```properties
host=localhost
port=9000
password=123456

proxyHost=localhost,localhost
proxyPort=8899,9988

remotePort=11000,12000
```

---

### 测试

- 本地启动两个web服务，端口分别为8899,9988

- 分别访问 **localhost:11000** 和 **localhost:12000**
- 若有正常结果返回，则证明代理成功

## 附

#### 多服务代理客户端配置样例

这是本人使用的一份**代理客户端配置**，主要代理内网笔记本的一些ssh服务和基础服务

```shell
# 外网服务器地址
host=47.96.158.250
# 外网服务器端口
port=7731
# 外网服务器所需要的密码
password=123123

# 需要被代理的服务器地址
proxyHost=localhost,localhost,localhost,localhost
# 需要被代理的服务器端口(目前代理了ssh、mysql、redis、mongodb)
proxyPort=22,3306,6379,27017

# 访问内网被代理服务所暴露的外网端口
remotePort=12222,13306,16379,27017
```

#### Linux启动脚本例子

- 以制作server端的service服务脚本为例（推荐）

  - 首先切换到root创建并编辑文件(文件名即是服务名，自己取)

    ```cmd
    vim  /etc/systemd/system/netty-server.service
    ```

  - 复制以下内容到netty-server.service，并保存退出。

    ```shell
    Description=Netty Service
    Documentation=https://github.com/MrLawrenc/Netty_Proxy
    After=network-online.target
    Wants=network-online.target
    
    [Service]
    Type=simple
    ExecStart=/home/plugins/jdk-11.0.4/bin/java -server -Xms256m -Xmx256m -jar /etc/systemd/system/server-2.1.3.jar -port 9599 -password 123456
    ExecStop=/bin/kill -s QUIT $MAINPID
    Restart=always
    StandOutput=syslog
    
    StandError=inherit
    
    [Install]
    WantedBy=multi-user.target
    ```

    注意 `/home/plugins/jdk-11.0.4/bin/java`为java可执行命令的全路径（java路径或者在`/usr/bin/java`），不知道在哪儿的可以使用`which java`命令查看

    ![which](https://lmy25.wang/upload/2020/10/which-36b91787a6d54e16b0127f1f703ff16a.png)

    `/etc/systemd/system/server-2.1.3.jar`是自己jar的全路径，systemd 中的所有路径均要写为绝对路径，另外，`~` 在 systemd 中也是无法被识别的，所以你不能写成类似 `~/server-1.2.3.jar` 这种路径。

    检验是否修改正确可以把 ExecStart 中的命令拿出来执行一遍。

    StandOutput配置日志输出，可以自行调整

  - 之后启动服务，查看状态，停止

    ```shell
    service netty-server start
    service netty-server status -l
    service netty-server stop
    ```

  - 其他

    1. 有更改*.service文件需要使用`systemctl daemon-reload`重新刷新之后再启动服务
    2. journalctl -u 服务名  可以查看详细service服务日志

- 简单sh启动脚本（不推荐）

  服务端server.sh

  ```shell script
  nohup java -jar server-2.1.1.jar -port 9527 -password 123456 >>server.log 2>&1  &
  ```

  客户端client.sh

  ```shell
  nohup java -jar client-2.1.1.jar -h 服务端地址 -p 9527 -password 123456 -proxy_h localhost -proxy_p 8080 -remote_p 11000 >>client.log 2>&1  &
  ```

  之后赋予权限

  ```shell
  chmod  777 server.sh
  chmod  777 client.sh
  ```

  启动

  ```shell
  ./server.sh
  ```

#### 压力测试

- 本地测试服务端，客户端
- 配置jemeter压测参数，单机可达3000的qps，主要限制在代理客户端开启的连接数
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

