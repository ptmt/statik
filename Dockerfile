FROM amazoncorretto:21

RUN yum install -y tar gzip coreutils && \
    yum clean all

COPY . /src

WORKDIR /src

# Ensure amper is executable and verify it exists
RUN chmod +x ./amper && ls -la ./amper

# Pre-build the project to cache dependencies
RUN ./amper build

# Verify amper still exists after build
RUN ls -la ./amper && ./amper --help || echo "amper help failed"

ENTRYPOINT ["./amper"]
CMD ["run"]