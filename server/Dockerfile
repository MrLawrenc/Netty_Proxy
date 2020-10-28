# 基础镜像使用java
FROM sapmachine/jdk11
# 作者
MAINTAINER mars <mrliu943903861@163.com>
# VOLUME 指定了临时文件目录为/tmp。
# 其效果是在主机 /var/lib/docker 目录下创建了一个临时文件，并链接到容器的/tmp。加入需要挂载的文件是/var/lib/docker/tmp/a/b.txt，则使用/tmp/a/b.txt可以在容器中读取到
VOLUME /tmp
# 将jar包添加到容器中并更名（jar包名字根据实际情况填写）
ADD server-RELEASE.jar server.jar
RUN bash -c 'touch /server.jar'

# 运行jar包
#ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/server.jar","-p 9999","-pwd 123456"]
ENTRYPOINT  java  ${JAVA_OPTS}  -jar  /server.jar -port  ${P} -password ${PWD}

#构建 docker build -f ./Dockerfile -t liu/server:latest .
#启动 docker run  -d -e  JAVA_OPTS='-Xmx128m -Xms128m' -e PORT=9999 -e PASSWORD=123456  --name server -p9999:9999 liu/server