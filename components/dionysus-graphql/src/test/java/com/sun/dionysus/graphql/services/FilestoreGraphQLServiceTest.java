package com.sun.dionysus.graphql.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.sun.dionysus.codegen.types.Bucket;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class FilestoreGraphQLServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FilestoreGraphQLService filestoreGraphQLService;

    @Test
    void health_returnsResponseFromRestApi() {
        String expected = "{\"status\":\"ok\"}";
        when(restTemplate.getForObject("https://filestore.scarlettparker.co.uk/api/health", String.class))
            .thenReturn(expected);

        String result = filestoreGraphQLService.health();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void listBuckets_returnsBucketsFromRestApi() {
        Bucket b = new Bucket();
        b.setId("b1");
        Bucket[] arr = {b};
        when(restTemplate.exchange("https://filestore.scarlettparker.co.uk/api/v2/ListBuckets", HttpMethod.GET, any(), eq(Bucket[].class)))
            .thenReturn(new ResponseEntity<>(arr, HttpStatus.OK));

        List<Bucket> result = filestoreGraphQLService.listBuckets();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("b1");
    }
}
