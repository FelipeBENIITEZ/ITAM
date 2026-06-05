FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copiar archivos de build primero para aprovechar cache de dependencias
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN mvn -B -DskipTests dependency:go-offline || true

# Copiar el resto del proyecto y compilar
COPY . .
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

# Copiar jar construido
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 9090

ENTRYPOINT ["java","-jar","/app/app.jar"]
