package com.sun.briareus.graphql.config;

import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
@EnableJpaAuditing
public class GraphQLConfig {

  @Bean
  public RuntimeWiringConfigurer runtimeWiringConfigurer() {
    return wiringBuilder -> wiringBuilder.scalar(dateScalar());
  }

  private GraphQLScalarType dateScalar() {
    return GraphQLScalarType.newScalar()
        .name("Date")
        .description("A scalar for LocalDateTime")
        .coercing(new graphql.schema.Coercing<LocalDateTime, String>() {
          @Override
          public String serialize(Object dataFetcherResult) {
            if (dataFetcherResult instanceof LocalDateTime) {
              return ((LocalDateTime) dataFetcherResult).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            return null;
          }

          @Override
          public LocalDateTime parseValue(Object input) {
            if (input instanceof String) {
              return LocalDateTime.parse((String) input, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            return null;
          }

          @Override
          public LocalDateTime parseLiteral(Object input) {
            if (input instanceof graphql.language.StringValue) {
              return LocalDateTime.parse(((graphql.language.StringValue) input).getValue(),
                  DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            return null;
          }
        })
        .build();
  }
}