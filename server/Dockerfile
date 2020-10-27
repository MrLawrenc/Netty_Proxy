# 基础镜像使用java
FROM sapmachine/jdk11
# 作者
MAINTAINER mars <mrliu943903861@163.com>
# VOLUME 指定了临时文件目录为/tmp。
# 其效果是在主机 /var/lib/docker 目录下创建了一个临时文件，并链接到容器的/tmp
VOLUME /tmp
# 将jar包添加到容器中并更名
ADD server-2.1.3.jar server.jar
RUN bash -c 'touch /server.jar'

# 运行jar包
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/server.jar","-p9999"]