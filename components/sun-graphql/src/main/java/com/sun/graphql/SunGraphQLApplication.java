package com.sun.graphql;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * Gateway entry point. Aggregates every enabled component module (see the {@code dbModules} /
 * {@code statelessModules} lists in {@code build.gradle}) into one Netflix DGS GraphQL schema
 * backed by a single shared database.
 *
 * <p>All variables from the single backend {@code .env} are exposed as system properties, so any
 * new env var is picked up automatically without code changes here.
 */
@SpringBootApplication(scanBasePackages = "com.sun")
@EntityScan(basePackages = "com.sun")
public class SunGraphQLApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        SpringApplication.run(SunGraphQLApplication.class, args);
    }
}
