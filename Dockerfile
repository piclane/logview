FROM openjdk:11-jdk-slim AS build

RUN apt-get -y update && \
    apt-get -y install curl nodejs npm git && \
    npm install n -g && \
    n stable && \
    apt purge -y nodejs npm

ADD ./ /app/

WORKDIR /app/
ENV JVM_OPTS="-Xmx2048m"
RUN ./gradlew --no-daemon bootJar --info

FROM openjdk:11-jdk-slim
COPY --from=build /app/build/libs/logview*.jar /app/
RUN mkdir /app/mnt ; echo "app.fs.root=/app/mnt/" > /app/application.properties
WORKDIR /app/
CMD ["sh", "-c", "java -jar logview*.jar"]
