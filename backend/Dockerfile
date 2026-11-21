# Image de production de l'API SmartShop (Render, ou tout hebergeur Docker).
# Build : docker build -t smartshop-api backend/

# ---------- Build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Dependances d'abord : couche mise en cache tant que pom.xml ne change pas
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B package -DskipTests

# ---------- Execution ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S smartshop && adduser -S smartshop -G smartshop
COPY --from=build /app/target/*.jar app.jar
USER smartshop

# Profil prod : configuration uniquement par variables d'environnement (voir application-prod.yml)
ENV SPRING_PROFILES_ACTIVE=prod
# Reglage pour une instance de 512 Mo (offre gratuite) : tas limite, GC serie, JIT C1 seul (demarrage rapide)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:ReservedCodeCacheSize=64m"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
