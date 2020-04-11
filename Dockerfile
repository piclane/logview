FROM openjdk:11-jdk-slim AS build

RUN apt-get -y update && \
    apt-get -y install curl nodejs npm git && \
    npm install n -g && \
    n stable && \
    apt purge -y nodejs npm

ADD ./ /logview/
ADD ./src/docker/context.xml /logview/web/META-INF/context.xml

RUN cd /logview && \
    npm install && \
    ./gradlew war -Pdocker.build -Pjdk11

FROM tomcat:9-jdk11-openjdk-slim

COPY --from=build /logview/build/libs/logview.war /usr/local/tomcat/webapps/

RUN mkdir -p /var/lib/logview

# VOLUME /var/lib/logview

EXPOSE 8080
CMD ["catalina.sh", "run"]
