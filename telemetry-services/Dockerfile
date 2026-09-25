FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8092

ENV MONGODB_URI=""
ENV RABBITMQ_HOST=""
ENV RABBITMQ_PORT=""
ENV RABBITMQ_USERNAME=""
ENV RABBITMQ_PASSWORD=""
ENV RABBITMQ_VHOST=""

ENV EUREKA.CLIENT.SERVICE-URL.DEFAULTZONE=""

ENTRYPOINT ["java", "-jar", "app.jar"]