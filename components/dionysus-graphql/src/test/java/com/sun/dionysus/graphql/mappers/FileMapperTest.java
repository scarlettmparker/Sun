package com.sun.dionysus.graphql.mappers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.sun.dionysus.codegen.types.File;
import software.amazon.awssdk.services.s3.model.S3Object;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FileMapperTest {

  private FileMapper fileMapper = new FileMapper();

  @Test
  void mapObject_shouldCreateFileType() {
    S3Object object = S3Object.builder()
        .key("folder/file.txt")
        .size(123L)
        .lastModified(Instant.parse("2024-01-01T10:00:00Z"))
        .build();

    File result = fileMapper.mapObject(object);

    assertThat(result.getKey()).isEqualTo("folder/file.txt");
    assertThat(result.getSize()).isEqualTo(123);
    assertThat(result.getLastModified()).isEqualTo("2024-01-01T10:00:00Z");
  }
}
