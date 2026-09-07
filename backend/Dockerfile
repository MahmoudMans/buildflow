FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .

RUN mvn --batch-mode --no-transfer-progress -DskipTests dependency:go-offline

COPY src ./src

RUN mvn --batch-mode --no-transfer-progress -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl

RUN addgroup -S buildflow
RUN adduser -S buildflow -G buildflow

WORKDIR /app

COPY --from=build /workspace/target/buildflow-backend-0.1.0-SNAPSHOT.jar /app/buildflow.jar

USER buildflow

EXPOSE 8080

ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-XX:+ExitOnOutOfMemoryError","-jar","/app/buildflow.jar"]
