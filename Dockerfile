FROM amazoncorretto:21

RUN yum install -y tar gzip coreutils && \
    yum clean all

COPY . /src

WORKDIR /src

RUN chmod +x ./amper

RUN ./amper