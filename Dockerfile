# syntax=docker/dockerfile:1
FROM amazoncorretto:21

COPY build/tasks/_statik_executableJarJvm/statik-jvm-executable.jar /app/statik.jar
COPY docker-entrypoint.sh /usr/local/bin/statik-entrypoint
RUN chmod +x /usr/local/bin/statik-entrypoint

WORKDIR /github/workspace

EXPOSE 3000

ENV AMPER_OVERRIDE_MAVEN_CENTRAL_URL_TO_CACHE_REDIRECTOR="true"

ENTRYPOINT ["statik-entrypoint"]
CMD ["run"]
