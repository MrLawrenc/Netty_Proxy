# 基础镜像使用java
FROM sapmachine/jdk11
# 作者
MAINTAINER mars <mrliu943903861@163.com>
# VOLUME 指定了临时文件目录为/tmp。
# 其效果是在主机 /var/lib/docker 目录下创建了一个临时文件，并链接到容器的/tmp
VOLUME /tmp
# 将jar包添加到容器中并更名
ADD client-2.1.3.jar client.jar
RUN bash -c 'touch /client.jar'

# 只是单纯的拼接
#ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/client.jar","-profile","/tmp/testclient/client.pro"]
#可以替换变量
ENTRYPOINT  java -Xmx512m -Xms512m ${JAVA_OPTS}  -jar  /client.jar -profile   ${CONF_PATH}
#CMD ["-profile /tmp/testclient/client.pro"]