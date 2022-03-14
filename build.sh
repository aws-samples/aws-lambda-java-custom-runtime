#!/bin/sh
# remove a maybe earlier build custom runtime archives
rm runtime.zip

# Build the docker image which will:
#   1. Use the latest Amazon Linux 2 image and install Amazon Corretto 18
#   2. Copy the software directory into the Docker container and run the build using Maven, which creates an uber jar
#   3. run jdeps to calculate the module dependencies for this uber jar
#   4. feeding the jdeps result into jlink, creating a minimal Java 18 JRE which only contains the necessary modules to run this jar
#   5. Use Javas Application Class Data Sharing for further optimizations
#   6. Create the runtime.zip archive, based on the AWS Lambda custom runtime specification
docker build -f Dockerfile --progress=plain -t lambda-custom-runtime-minimal-jre-18-x86 .
# Extract the runtime.zip from the Docker container and store it locally
docker run --rm --entrypoint cat lambda-custom-runtime-minimal-jre-18-x86 runtime.zip > runtime.zip
