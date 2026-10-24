package com.sun.dionysus.graphql.resolvers;

import com.sun.dionysus.codegen.types.DeleteSubtitleInput;
import com.sun.dionysus.codegen.types.DeleteSubtitleResponse;
import com.sun.dionysus.codegen.types.Subtitle;
import com.sun.dionysus.graphql.mappers.SubtitleMapper;
import com.sun.dionysus.service.SubtitleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubtitleDataFetcherTest {

  @Mock
  private S3Client s3Client;

  @Mock
  private SubtitleService subtitleService;

  @Mock
  private com.sun.dionysus.subtitle.OpenSubtitlesService openSubtitlesService;

  @Mock
  private SubtitleMapper subtitleMapper;

  @InjectMocks
  private SubtitleDataFetcher fetcher;

  @Test
  void deleteSubtitle_delegatesToService() {
    DeleteSubtitleInput input = DeleteSubtitleInput.newBuilder()
        .bucket("b")
        .subtitleKey("movie.en.srt")
        .build();

    DeleteSubtitleResponse response = fetcher.deleteSubtitle(input);

    assertThat(response.getKey()).isEqualTo("movie.en.srt");
    verify(subtitleService).deleteBySubtitleKey("b", "movie.en.srt");
  }
}
