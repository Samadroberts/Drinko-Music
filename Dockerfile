FROM amazoncorretto:11-alpine-jdk

COPY . drinko-music
WORKDIR drinko-music

RUN apk add maven
RUN mvn clean install -U -DskipTests && mvn package

CMD ["java", "-jar", "target/drinko-music-0.0.7-SNAPSHOT.jar"]
