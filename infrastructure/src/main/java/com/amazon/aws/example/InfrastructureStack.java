package com.amazon.aws.example;

import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.CfnOutputProps;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.apigatewayv2.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.HttpApiProps;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.amazon.awscdk.services.apigatewayv2.PayloadFormatVersion;
import software.amazon.awscdk.services.apigatewayv2.integrations.LambdaProxyIntegration;
import software.amazon.awscdk.services.apigatewayv2.integrations.LambdaProxyIntegrationProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;

public class InfrastructureStack extends Stack {
    public InfrastructureStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Table exampleTable = new Table(this, "ExampleTable", TableProps.builder()
                .partitionKey(Attribute.builder()
                        .type(AttributeType.STRING)
                        .name("id").build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());

        Function customJava18Function = new Function(this, "LambdaCustomRuntimeJava18", FunctionProps.builder()
                .functionName("custom-runtime-java-18")
                .handler("com.amazon.aws.example.ExampleDynamoDbHandler::handleRequest")
                .runtime(Runtime.PROVIDED_AL2)
                .architecture(Architecture.X86_64)
                .code(Code.fromAsset("../runtime.zip"))
                .memorySize(512)
                .environment(Map.of("TABLE_NAME", exampleTable.getTableName()))
                .timeout(Duration.seconds(20))
                .logRetention(RetentionDays.ONE_WEEK)
                .build());

        exampleTable.grantWriteData(customJava18Function);

        HttpApi httpApi = new HttpApi(this, "ExampleApi", HttpApiProps.builder()
                .apiName("ExampleApi")
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/custom-runtime")
                .methods(singletonList(HttpMethod.POST))
                .integration(new LambdaProxyIntegration(LambdaProxyIntegrationProps.builder()
                        .handler(customJava18Function)
                        .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                        .build()))
                .build());

        new CfnOutput(this, "api-endpoint", CfnOutputProps.builder()
                .value(httpApi.getApiEndpoint())
                .build());
    }
}
