package com.sun.dionysus.subtitle;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenSubtitlesCacheTest {

  @Test
  void sameKeyServedFromCache() {
    CountingService service = new CountingService();

    List<SubtitleSearchResult> first = service.search("Movie", 2019, "en", "hash", 100L);
    List<SubtitleSearchResult> second = service.search("Movie", 2019, "en", "hash", 100L);

    assertThat(first).hasSize(1);
    assertThat(second).hasSize(1);
    assertThat(service.calls).isEqualTo(1);
  }

  @Test
  void expiredEntryTriggersNewSearch() throws InterruptedException {
    CountingService service = new CountingService(Duration.ofMillis(20), 10);

    service.search("Movie", 2019, "en", "hash", 100L);
    Thread.sleep(60);
    service.search("Movie", 2019, "en", "hash", 100L);

    assertThat(service.calls).isEqualTo(2);
  }

  @Test
  void differentKeysDoNotShareCache() {
    CountingService service = new CountingService();

    service.search("Movie", 2019, "en", "hash", 100L);
    service.search("Other", 2019, "en", "hash", 100L);

    assertThat(service.calls).isEqualTo(2);
  }

  private static final class CountingService extends OpenSubtitlesService {

    int calls;

    CountingService() {
      super("key", "user", "pass", "agent", Duration.ofHours(1), 10);
    }

    CountingService(Duration ttl, int maxSize) {
      super("key", "user", "pass", "agent", ttl, maxSize);
    }

    @Override
    protected List<SubtitleSearchResult> searchRemote(
        String query, Integer year, String language, String movieHash, Long movieBytes) {
      calls++;
      return List.of(new SubtitleSearchResult(
          com.sun.dionysus.model.enums.SubtitleSource.OPENSUBTITLES,
          null,
          42,
          language,
          "label",
          false,
          7,
          8.5));
    }
  }
}
