FROM amazoncorretto:21

RUN yum install -y tar gzip coreutils && \
    yum clean all

COPY . /src

WORKDIR /src

RUN chmod +x ./amper

# Pre-build the project to cache dependencies
RUN ./amper build

ENTRYPOINT ["./amper"]
CMD ["run"]