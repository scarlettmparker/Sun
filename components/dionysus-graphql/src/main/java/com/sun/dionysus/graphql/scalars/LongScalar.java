package com.sun.dionysus.graphql.scalars;

import com.netflix.graphql.dgs.DgsScalar;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

/**
 * GraphQL Long scalar, serialised as a string so byte sizes past the 2GB Int
 * limit survive the trip to the client without losing precision.
 */
@DgsScalar(name = "Long")
public class LongScalar implements Coercing<Long, String> {

  @Override
  public String serialize(Object dataFetcherResult) {
    if (dataFetcherResult == null) {
      return null;
    }
    if (dataFetcherResult instanceof Long value) {
      return value.toString();
    }
    if (dataFetcherResult instanceof Number number) {
      return Long.toString(number.longValue());
    }
    throw new CoercingSerializeException("Expected a Long, got " + dataFetcherResult.getClass());
  }

  @Override
  public Long parseValue(Object input) {
    if (input == null) {
      return null;
    }
    if (input instanceof String value) {
      return Long.parseLong(value);
    }
    if (input instanceof Number number) {
      return number.longValue();
    }
    throw new CoercingParseValueException("Expected a Long");
  }

  @Override
  public Long parseLiteral(Object input) {
    if (input instanceof StringValue value) {
      return Long.parseLong(value.getValue());
    }
    throw new CoercingParseLiteralException("Expected a StringValue");
  }
}
