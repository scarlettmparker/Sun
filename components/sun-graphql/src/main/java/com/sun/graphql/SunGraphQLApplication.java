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
        setSystemProperty(dotenv, "DION_SPRING_DATASOURCE_URL", "DION_SPRING_DATASOURCE_URL");
        setSystemProperty(dotenv, "DION_SPRING_DATASOURCE_USERNAME", "DION_SPRING_DATASOURCE_USERNAME");
        setSystemProperty(dotenv, "DION_SPRING_DATASOURCE_PASSWORD", "DION_SPRING_DATASOURCE_PASSWORD");
        // Echo datasource
        setSystemProperty(dotenv, "ECHO_SPRING_DATASOURCE_URL", "ECHO_SPRING_DATASOURCE_URL");
        setSystemProperty(dotenv, "ECHO_SPRING_DATASOURCE_USERNAME", "ECHO_SPRING_DATASOURCE_USERNAME");
        setSystemProperty(dotenv, "ECHO_SPRING_DATASOURCE_PASSWORD", "ECHO_SPRING_DATASOURCE_PASSWORD");
        // Fates datasource
        setSystemProperty(dotenv, "FATES_SPRING_DATASOURCE_URL", "FATES_SPRING_DATASOURCE_URL");
        setSystemProperty(dotenv, "FATES_SPRING_DATASOURCE_USERNAME", "FATES_SPRING_DATASOURCE_USERNAME");
        setSystemProperty(dotenv, "FATES_SPRING_DATASOURCE_PASSWORD", "FATES_SPRING_DATASOURCE_PASSWORD");
        // Gaia datasource
        setSystemProperty(dotenv, "GAIA_SPRING_DATASOURCE_URL", "GAIA_SPRING_DATASOURCE_URL");
        setSystemProperty(dotenv, "GAIA_SPRING_DATASOURCE_USERNAME", "GAIA_SPRING_DATASOURCE_USERNAME");
        setSystemProperty(dotenv, "GAIA_SPRING_DATASOURCE_PASSWORD", "GAIA_SPRING_DATASOURCE_PASSWORD");
        // Hades datasource
        setSystemProperty(dotenv, "HADES_SPRING_DATASOURCE_URL", "HADES_SPRING_DATASOURCE_URL");
        setSystemProperty(dotenv, "HADES_SPRING_DATASOURCE_USERNAME", "HADES_SPRING_DATASOURCE_USERNAME");
        setSystemProperty(dotenv, "HADES_SPRING_DATASOURCE_PASSWORD", "HADES_SPRING_DATASOURCE_PASSWORD");
        // Icarus datasource
        setSystemProperty(dotenv, "ICARUS_SPRING_DATASOURCE_URL", "ICARUS_SPRING_DATASOURCE_URL");
        setSystemProperty(dotenv, "ICARUS_SPRING_DATASOURCE_USERNAME", "ICARUS_SPRING_DATASOURCE_USERNAME");
        setSystemProperty(dotenv, "ICARUS_SPRING_DATASOURCE_PASSWORD", "ICARUS_SPRING_DATASOURCE_PASSWORD");
        // Auth
        setSystemProperty(dotenv, "JWT_SECRET", "JWT_SECRET");
        // Discord OAuth
        setSystemProperty(dotenv, "DISCORD_CLIENT_ID", "DISCORD_CLIENT_ID");
        setSystemProperty(dotenv, "DISCORD_CLIENT_SECRET", "DISCORD_CLIENT_SECRET");
        setSystemProperty(dotenv, "DISCORD_GUILD_ID", "DISCORD_GUILD_ID");
        setSystemProperty(dotenv, "DISCORD_REDIRECT_URI", "DISCORD_REDIRECT_URI");
        setSystemProperty(dotenv, "DISCORD_BOT_TOKEN", "DISCORD_BOT_TOKEN");
        // Email
        setSystemProperty(dotenv, "EMAIL_CLIENT_ID", "EMAIL_CLIENT_ID");
        setSystemProperty(dotenv, "EMAIL_CLIENT_SECRET", "EMAIL_CLIENT_SECRET");
        setSystemProperty(dotenv, "EMAIL_REFRESH_TOKEN", "EMAIL_REFRESH_TOKEN");
        setSystemProperty(dotenv, "EMAIL_ADDRESS", "EMAIL_ADDRESS");
        setSystemProperty(dotenv, "GARAGE_SECRET_KEY", "GARAGE_SECRET_KEY");
        setSystemProperty(dotenv, "AWS_ACCESS_KEY_ID", "AWS_ACCESS_KEY_ID");
        setSystemProperty(dotenv, "AWS_SECRET_ACCESS_KEY", "AWS_SECRET_ACCESS_KEY");
        setSystemProperty(dotenv, "S3_ENDPOINT", "S3_ENDPOINT");
        SpringApplication.run(SunGraphQLApplication.class, args);
    }

    private static void setSystemProperty(Dotenv dotenv, String propertyName, String envName) {
        String value = dotenv.get(envName);
        if (value != null) {
            System.setProperty(propertyName, value);
        }
    }

}