FROM openjdk:17

WORKDIR /app

COPY . .

RUN javac IDSWebServer.java

EXPOSE 8080

CMD ["java", "IDSWebServer"]
