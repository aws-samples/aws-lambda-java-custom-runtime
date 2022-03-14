#!/bin/sh
set -e

# let's remember the projects root directory location
PROJECT_ROOT_DIRECTORY=$(pwd)

# navigate into the infrastructure sub-directory
cd infrastructure

# deploy the AWS infrastructure
cdk deploy --outputs-file target/outputs.json

# test the Amazon API Gateway endpoint
# we should see an HTTP 200 status code
curl -i -XPOST $(cat target/outputs.json | jq -r '.LambdaCustomRuntimeMinimalJRE18InfrastructureStack.apiendpoint')/custom-runtime

# navigate back into the projects root directory
cd $(pwd)
