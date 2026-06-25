# syntax=docker/dockerfile:1
# ---- Etapa de construccion: compila el JAR del microservicio ms-proveedores ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
# Se copia todo el reactor (pom agregador + modulos) como contexto de build
COPY . .
# La cache de .m2 se comparte entre builds (BuildKit) para no redescargar dependencias
RUN --mount=type=cache,target=/root/.m2     mvn -q -pl ms-proveedores clean package -DskipTests

# ---- Etapa de ejecucion: imagen ligera solo con el runtime de Java ----
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/ms-proveedores/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java","-jar","/app/app.jar"]
