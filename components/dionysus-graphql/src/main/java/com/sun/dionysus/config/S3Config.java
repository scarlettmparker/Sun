package com.sun.dionysus.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import java.net.URI;

@Configuration
public class S3Config {

  @Value("${FILESTORE_ENDPOINT:http://127.0.0.1:3900}")
  private String endpoint;

  @Value("${AWS_ACCESS_KEY_ID:}")
  private String accessKey;

  @Value("${AWS_SECRET_ACCESS_KEY:}")
  private String secretKey;

  @Bean
  public S3Client s3Client() {
    return S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey)))
        .region(Region.of("garage"))
        .serviceConfiguration(S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build())
        .build();
  }
}
