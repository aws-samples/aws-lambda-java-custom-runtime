#!/bin/sh
set -e

# Remember the projects root directory location
PROJECT_ROOT_DIRECTORY=$(pwd)

# Navigate into the infrastructure sub-directory
cd infrastructure

# Deploy the AWS infrastructure via AWS CDK and store the outputs in a file
cdk deploy --outputs-file target/outputs.json

# Test the Amazon API Gateway endpoint - We should see a "successful" message
curl -XPOST $(cat target/outputs.json | jq -r '.LambdaCustomRuntimeMinimalJRE18InfrastructureStack.apiendpoint')/custom-runtime

# Navigate back into the projects root directory
cd $(pwd)
