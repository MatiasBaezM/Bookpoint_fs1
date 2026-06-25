# syntax=docker/dockerfile:1
# ---- Etapa de construccion: compila el JAR del microservicio ms-usuarios ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
# Se copia todo el reactor (pom agregador + modulos) como contexto de build
COPY . .
# La cache de .m2 se comparte entre builds (BuildKit) para no redescargar dependencias
RUN --mount=type=cache,target=/root/.m2     mvn -q -pl ms-usuarios clean package -DskipTests

# ---- Etapa de ejecucion: imagen ligera solo con el runtime de Java ----
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/ms-usuarios/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/app.jar"]
