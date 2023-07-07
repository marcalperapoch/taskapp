FROM eclipse-temurin:17
RUN mkdir /opt/app
ARG APP_JAR
ENV APP_JAR_ENV=${APP_JAR}
COPY dockerized/${APP_JAR} /opt/app/${APP_JAR}
COPY dockerized/tasks-app-config.yaml /opt/app/config/tasks-app-config.yaml
WORKDIR /opt/app
EXPOSE 8080 8081
ENTRYPOINT java -jar ${APP_JAR_ENV} server config/tasks-app-config.yaml