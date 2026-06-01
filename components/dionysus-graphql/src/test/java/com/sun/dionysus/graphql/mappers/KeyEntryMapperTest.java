package com.sun.dionysus.graphql.mappers;

import com.sun.dionysus.graphql.models.KeyDetail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.sun.dionysus.codegen.types.KeyEntry;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.S3Object;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class KeyEntryMapperTest {

  private KeyEntryMapper keyEntryMapper = new KeyEntryMapper();

  @Test
  void mapDirectory_shouldCreateDirectoryEntry() {
    CommonPrefix prefix = CommonPrefix.builder().prefix("folder/").build();

    KeyEntry result = keyEntryMapper.mapDirectory(prefix);

    assertThat(result.getKey()).isEqualTo("folder/");
    assertThat(result.getIsDirectory()).isTrue();
    assertThat(result.getSize()).isEqualTo(0);
    assertThat(result.getLastModified()).isNull();
  }

  @Test
  void mapDirectory_withKeyDetail_enrichesEntry() {
    CommonPrefix prefix = CommonPrefix.builder().prefix("folder/").build();
    KeyDetail detail = new KeyDetail();
    detail.setName("My Folder");
    detail.setDescription("A folder");

    KeyEntry result = keyEntryMapper.mapDirectory(prefix, detail);

    assertThat(result.getKey()).isEqualTo("folder/");
    assertThat(result.getIsDirectory()).isTrue();
    assertThat(result.getName()).isEqualTo("My Folder");
    assertThat(result.getDescription()).isEqualTo("A folder");
  }

  @Test
  void mapFile_shouldCreateFileEntry() {
    S3Object object = S3Object.builder()
        .key("folder/file.txt")
        .size(123L)
        .lastModified(Instant.parse("2024-01-01T10:00:00Z"))
        .build();

    KeyEntry result = keyEntryMapper.mapFile(object);

    assertThat(result.getKey()).isEqualTo("folder/file.txt");
    assertThat(result.getIsDirectory()).isFalse();
    assertThat(result.getSize()).isEqualTo(123);
    assertThat(result.getLastModified()).isEqualTo("2024-01-01T10:00:00Z");
  }

  @Test
  void mapFile_withKeyDetail_enrichesEntry() {
    S3Object object = S3Object.builder()
        .key("folder/file.txt")
        .size(123L)
        .build();
    KeyDetail detail = new KeyDetail();
    detail.setName("Important File");
    detail.setDescription("Document");

    KeyEntry result = keyEntryMapper.mapFile(object, detail);

    assertThat(result.getKey()).isEqualTo("folder/file.txt");
    assertThat(result.getIsDirectory()).isFalse();
    assertThat(result.getName()).isEqualTo("Important File");
    assertThat(result.getDescription()).isEqualTo("Document");
  }
}

