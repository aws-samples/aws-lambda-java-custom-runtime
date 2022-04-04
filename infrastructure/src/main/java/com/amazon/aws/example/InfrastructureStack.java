package com.amazon.aws.example;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.RestApiProps;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

import java.util.Map;

public class InfrastructureStack extends Stack {
    public InfrastructureStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        var exampleTable = new Table(this, "ExampleTable", TableProps.builder()
                .partitionKey(Attribute.builder()
                        .type(AttributeType.STRING)
                        .name("id").build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());

        var customJava18Function = new Function(this, "LambdaCustomRuntimeJava18", FunctionProps.builder()
                .functionName("custom-runtime-java-18")
                .handler("com.amazon.aws.example.ExampleDynamoDbHandler::handleRequest")
                .runtime(Runtime.PROVIDED_AL2)
                .code(Code.fromAsset("../runtime.zip"))
                .memorySize(512)
                .environment(Map.of("TABLE_NAME", exampleTable.getTableName()))
                .timeout(Duration.seconds(20))
                .logRetention(RetentionDays.ONE_WEEK)
                .build());

        exampleTable.grantWriteData(customJava18Function);

        var restApi = new RestApi(this, "ExampleApi", RestApiProps.builder()
                .restApiName("ExampleApi")
                .build());

        restApi.getRoot()
                .addResource("custom-runtime")
                .addMethod("POST", new LambdaIntegration(customJava18Function));

        new CfnOutput(this, "api-endpoint", CfnOutputProps.builder()
                .value(restApi.getUrl())
                .build());
    }
}
