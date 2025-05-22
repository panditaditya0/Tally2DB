FROM amazoncorretto:17-alpine-jdk
MAINTAINER baeldung.com
COPY target/tally-backup.jar app.jar
ENV JAVA_OPTS="-Xms2g -Xmx3g"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]