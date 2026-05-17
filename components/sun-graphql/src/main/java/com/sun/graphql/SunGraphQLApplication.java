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
        setSystemProperty(dotenv, "APOLLO_SPRING_DATASOURCE_URL", "APOLLO_SPRING_DATASOURCE_URL");
        setSystemProperty(dotenv, "APOLLO_SPRING_DATASOURCE_USERNAME", "APOLLO_SPRING_DATASOURCE_USERNAME");
        setSystemProperty(dotenv, "APOLLO_SPRING_DATASOURCE_PASSWORD", "APOLLO_SPRING_DATASOURCE_PASSWORD");
        // Briareus datasource
        setSystemProperty(dotenv, "BRI_SPRING_DATASOURCE_URL", "BRI_SPRING_DATASOURCE_URL");
        setSystemProperty(dotenv, "BRI_SPRING_DATASOURCE_USERNAME", "BRI_SPRING_DATASOURCE_USERNAME");
        setSystemProperty(dotenv, "BRI_SPRING_DATASOURCE_PASSWORD", "BRI_SPRING_DATASOURCE_PASSWORD");
        // Cerberus datasource
        setSystemProperty(dotenv, "CERB_SPRING_DATASOURCE_URL", "CERB_SPRING_DATASOURCE_URL");
        setSystemProperty(dotenv, "CERB_SPRING_DATASOURCE_USERNAME", "CERB_SPRING_DATASOURCE_USERNAME");
        setSystemProperty(dotenv, "CERB_SPRING_DATASOURCE_PASSWORD", "CERB_SPRING_DATASOURCE_PASSWORD");
        // Filestore / Garage S3
        setSystemProperty(dotenv, "AWS_ACCESS_KEY_ID", "AWS_ACCESS_KEY_ID");
        setSystemProperty(dotenv, "AWS_SECRET_ACCESS_KEY", "AWS_SECRET_ACCESS_KEY");
        System.setProperty("FILESTORE_ENDPOINT", dotenv.get("FILESTORE_ENDPOINT", "http://127.0.0.1:3900"));
        SpringApplication.run(SunGraphQLApplication.class, args);
    }

    private static void setSystemProperty(Dotenv dotenv, String propertyName, String envName) {
        String value = dotenv.get(envName);
        if (value != null) {
            System.setProperty(propertyName, value);
        }
    }

}