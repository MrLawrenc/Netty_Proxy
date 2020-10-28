FROM sapmachine/jdk11
MAINTAINER mars <mrliu943903861@163.com>
VOLUME /tmp

ADD client-RELEASE.jar client.jar
RUN bash -c 'touch /client.jar'

# 只是单纯的拼接
#ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/client.jar","-profile","/tmp/testclient/client.pro"]
#可以替换变量
ENTRYPOINT  java  ${JAVA_OPTS}  -jar  /client.jar -profile   ${CONF_PATH}
#CMD ["-profile /tmp/testclient/client.pro"]

#构建 docker build -f ./Dockerfile -t liu/client:latest .
#启动 docker run -v /tmp:/tmp -e  JAVA_OPTS='-Xmx128m -Xms128m' -e CONF_PATH='配置在容器中读取路径' liu/client