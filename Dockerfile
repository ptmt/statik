FROM amazoncorretto:21

# Install dependencies
RUN yum install -y tar gzip coreutils && \
    yum clean all

# Copy files to container
COPY . .

# Make amper executable
RUN chmod +x ./amper

CMD ["./amper"]