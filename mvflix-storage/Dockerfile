FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/*jar eccomerce.jar

LABEL authors="user"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "eccomerce.jar"]