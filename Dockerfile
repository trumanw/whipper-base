FROM        trumanwoo/docker-jebian

MAINTAINER  Truman Woo <chunan.woo@gmail.com>

ENV         APP_VERSION 1.0
ENV 		APP_NAME wipbase

# Install unzip
RUN         apt-get install -y unzip

COPY        target/universal/$APP_NAME-$APP_VERSION.zip /root/apps/
WORKDIR     /root/apps

# Setup database connections in environment variables
ENV 		BASE_DB_HOST jdbc:mysql://db:3306/whipper_base?characterEncoding=utf-8
ENV 		BASE_DB_USER root
ENV 		BASE_DB_PASSWD 123456

# Setup rabbitmq host ip addr
ENV 		RABBIT_MQ_HOST mq
ENV 		RABBIT_MQ_USER admin
ENV 		RABBIT_MQ_PASSWD 654321

RUN         unzip $APP_NAME-$APP_VERSION.zip && \
            rm $APP_NAME-$APP_VERSION.zip
ENTRYPOINT  ["./wipbase-1.0/bin/wipbase", "-Dhttp.port=9000"]
EXPOSE      9000