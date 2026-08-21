package com.sun.apollo.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sun.apollo.codegen.types.Song;
import com.sun.apollo.codegen.types.Stem;
import com.sun.apollo.model.SongEntity;
import com.sun.apollo.model.StemEntity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SongMapperTest {

  @Mock private StemMapper stemMapper;
  @InjectMocks private SongMapper songMapper;

  @Test
  void map_shouldMapAllFields() {
    UUID id = UUID.randomUUID();
    StemEntity stemEntity = new StemEntity();
    stemEntity.setId(UUID.randomUUID());
    stemEntity.setFilePath("drums.mp3");
    stemEntity.setName("Drums");

    SongEntity entity = new SongEntity();
    entity.setId(id);
    entity.setName("Test Song");
    entity.setFilePath("test-song");
    entity.setStems(List.of(stemEntity));

    Stem mappedStem = Stem.newBuilder().path("drums.mp3").name("Drums").build();
    when(stemMapper.map(stemEntity)).thenReturn(mappedStem);

    Song result = songMapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getName()).isEqualTo("Test Song");
    assertThat(result.getPath()).isEqualTo("/_components/stem-player/test-song/stems/");
    assertThat(result.getStems()).hasSize(1);
    assertThat(result.getStems().get(0).getName()).isEqualTo("Drums");
  }

  @Test
  void map_withNullStems_shouldMapNull() {
    SongEntity entity = new SongEntity();
    entity.setId(UUID.randomUUID());
    entity.setName("Song");
    entity.setFilePath("file");
    entity.setStems(null);

    Song result = songMapper.map(entity);

    assertThat(result.getStems()).isNull();
    assertThat(result.getPath()).isEqualTo("/_components/stem-player/file/stems/");
  }


}
