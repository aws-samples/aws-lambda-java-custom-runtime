package com.amazon.aws.example;

import software.amazon.awscdk.App;

public class InfrastructureApp {
    public static void main(final String[] args) {
        App app = new App();

        new InfrastructureStack(app, "LambdaCustomRuntimeMinimalJRE18InfrastructureStack");

        app.synth();
    }
}
