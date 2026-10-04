# ---------- compilación ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Dependencias primero: mientras el pom no cambie, esta capa se reutiliza.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Sin pruebas: la única que hay levanta el contexto completo y pide una base.
RUN mvn -B -q -DskipTests package && cp target/*.jar app.jar

# ---------- ejecución ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app \
 && mkdir -p /app/uploads/materials \
 && chown -R app:app /app

COPY --from=build --chown=app:app /app/app.jar ./app.jar
USER app

# La JVM toma su tope del límite del contenedor, no de la RAM del VPS entero.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
# /api/categories es público (GET) y toca la base: si responde, la API y
# Postgres están bien. 127.0.0.1, nunca localhost (en alpine puede ir a ::1).
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/api/categories >/dev/null || exit 1

CMD ["java", "-jar", "/app/app.jar"]
