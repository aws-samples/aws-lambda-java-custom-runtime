FROM --platform=linux/amd64 amazonlinux:2 AS packer

# Add the Amazon Corretto repository
RUN rpm --import https://yum.corretto.aws/corretto.key
RUN curl -L -o /etc/yum.repos.d/corretto.repo https://yum.corretto.aws/corretto.repo

# Update the packages and install Amazon Corretto 18, Maven and Zip
RUN yum -y update
RUN yum install -y java-18-amazon-corretto-devel maven zip

# Set Java 18 as the default
RUN update-alternatives --set java "/usr/lib/jvm/java-18-amazon-corretto/bin/java"
RUN update-alternatives --set javac "/usr/lib/jvm/java-18-amazon-corretto/bin/javac"

# Copy the software folder to the image and build the function
COPY software software
WORKDIR /software/example-function
RUN mvn clean package

# Find JDK module dependencies dynamically from our uber jar
RUN jdeps \
    # dont worry about missing modules
    --ignore-missing-deps \
    # suppress any warnings printed to console
    -q \
    # java release version targeting
    --multi-release 18 \
    # output the dependencies at end of run
    --print-module-deps \
    # pipe the result of running jdeps on the function jar to file
    target/function.jar > jre-deps.info

# Create a slim Java 18 JRE which only contains the required modules to run this function
RUN jlink --verbose \
    --compress 2 \
    --strip-java-debug-attributes \
    --no-header-files \
    --no-man-pages \
    --output /jre18-slim \
    --add-modules $(cat jre-deps.info)


# Use Javas Application Class Data Sharing feature
# It creates the file /jre18-slim/lib/server/classes.jsa
RUN /jre18-slim/bin/java -Xshare:dump -Xbootclasspath/a:/software/example-function/target/function.jar

# Package everything together into a custom runtime archive
WORKDIR /

COPY bootstrap bootstrap
RUN chmod 755 bootstrap
RUN cp /software/example-function/target/function.jar function.jar
RUN zip -r runtime.zip \
    bootstrap \
    function.jar \
    /jre18-slim
