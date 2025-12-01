package com.sun.apollo.graphql.mappers;

import com.sun.apollo.codegen.types.Song;
import com.sun.apollo.codegen.types.Stem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.sun.apollo.model.SongEntity;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting domain Song entities to GraphQL Song types.
 */
@Component
public class SongMapper {

  private static final Logger logger = LoggerFactory.getLogger(SongMapper.class);

  @Autowired
  private StemMapper stemMapper;

  /**
   * Maps a domain SongEntity to a GraphQL Song type.
   *
   * @param songEntity the domain SongEntity to map
   * @return the mapped GraphQL Song type
   */
  public Song map(SongEntity songEntity) {
    logger.debug("Mapping song {}", songEntity.getName());

    List<Stem> stems = null;
    if (songEntity.getStems() != null) {
      stems = songEntity.getStems().stream()
        .map(stemMapper::map)
        .collect(Collectors.toList());
    }

    Song song = Song.newBuilder()
      .id(songEntity.getId().toString())
      .path("/_components/stem-player/" + songEntity.getFilePath() + "/stems/")
      .name(songEntity.getName())
      .stems(stems)
      .build();

    logger.debug("Mapped song {} with id {}", songEntity.getName(), song.getId());

    return song;
  }
}