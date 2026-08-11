FROM node:22-alpine AS web-build
WORKDIR /build/web
COPY web/package.json ./
ARG DEPENDENCY_REFRESH=local
RUN test -n "$DEPENDENCY_REFRESH"
RUN npm install --no-package-lock
COPY web/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS server-build
WORKDIR /build/server
COPY server/pom.xml ./
COPY server/src ./src
COPY --from=web-build /build/web/dist ./src/main/resources/static
RUN mvn package -B -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system netdesk && useradd --system --gid netdesk --home-dir /app netdesk \
    && mkdir -p /app/data && chown -R netdesk:netdesk /app
COPY --from=server-build --chown=netdesk:netdesk /build/server/target/myofficedevice-*.jar /app/myofficedevice.jar
USER netdesk
ENV NETDESK_DATA_DIR=/app/data
EXPOSE 8765
VOLUME ["/app/data"]
ENTRYPOINT ["java", "-jar", "/app/myofficedevice.jar"]
