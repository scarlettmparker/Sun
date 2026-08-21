package com.sun.briareus.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.briareus.codegen.types.BlogPostType;
import com.sun.briareus.model.BlogPostTypeEntity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BlogPostTypeMapperTest {

  private final BlogPostTypeMapper mapper = new BlogPostTypeMapper();

  @Test
  void map_entityToGraphQL() {
    BlogPostTypeEntity entity = new BlogPostTypeEntity();
    entity.setId(UUID.randomUUID());
    entity.setName("BOT_FAQ");
    entity.setDescription("Bot FAQ content");

    BlogPostType result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(entity.getId().toString());
    assertThat(result.getName()).isEqualTo("BOT_FAQ");
    assertThat(result.getDescription()).isEqualTo("Bot FAQ content");
  }

  @Test
  void map_omitsDescriptionWhenAbsent() {
    BlogPostTypeEntity entity = new BlogPostTypeEntity();
    entity.setId(UUID.randomUUID());
    entity.setName("BLOG");

    assertThat(mapper.map(entity).getDescription()).isNull();
  }

}
