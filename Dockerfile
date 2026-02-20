FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN javac IDSWebServer.java

EXPOSE 8080

CMD ["java", "IDSWebServer1"]
