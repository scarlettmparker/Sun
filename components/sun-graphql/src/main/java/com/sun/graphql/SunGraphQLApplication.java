package com.sun.graphql;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.sun")
@EntityScan(basePackages = "com.sun")
public class SunGraphQLApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().load();
        // Apollo datasource
        System.setProperty("APOLLO_SPRING_DATASOURCE_URL", dotenv.get("APOLLO_SPRING_DATASOURCE_URL"));
        System.setProperty("APOLLO_SPRING_DATASOURCE_USERNAME", dotenv.get("APOLLO_SPRING_DATASOURCE_USERNAME"));
        System.setProperty("APOLLO_SPRING_DATASOURCE_PASSWORD", dotenv.get("APOLLO_SPRING_DATASOURCE_PASSWORD"));
        // Briareus datasource
        System.setProperty("BRI_SPRING_DATASOURCE_URL", dotenv.get("BRI_SPRING_DATASOURCE_URL"));
        System.setProperty("BRI_SPRING_DATASOURCE_USERNAME", dotenv.get("BRI_SPRING_DATASOURCE_USERNAME"));
        System.setProperty("BRI_SPRING_DATASOURCE_PASSWORD", dotenv.get("BRI_SPRING_DATASOURCE_PASSWORD"));
        // Cerberus datasource
        System.setProperty("CERB_SPRING_DATASOURCE_URL", dotenv.get("CERB_SPRING_DATASOURCE_URL"));
        System.setProperty("CERB_SPRING_DATASOURCE_USERNAME", dotenv.get("CERB_SPRING_DATASOURCE_USERNAME"));
        System.setProperty("CERB_SPRING_DATASOURCE_PASSWORD", dotenv.get("CERB_SPRING_DATASOURCE_PASSWORD"));
        SpringApplication.run(SunGraphQLApplication.class, args);
    }

}