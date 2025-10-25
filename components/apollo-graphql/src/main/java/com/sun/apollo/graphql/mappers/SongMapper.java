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
   * @param domainSong the domain SongEntity to map
   * @return the mapped GraphQL Song type
   */
  public Song map(SongEntity domainSong) {
    logger.debug("Mapping song {}", domainSong.getName());

    List<Stem> stems = null;
    if (domainSong.getStems() != null) {
      stems = domainSong.getStems().stream()
          .map(stemMapper::map)
          .collect(Collectors.toList());
    }

    Song graphQLSong = Song.newBuilder()
        .id(domainSong.getId().toString())
        .name(domainSong.getName())
        .stems(stems)
        .build();

    logger.debug("Mapped song {} with id {}", domainSong.getName(), graphQLSong.getId());

    return graphQLSong;
  }
}