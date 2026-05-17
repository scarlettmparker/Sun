package com.sun.dionysus.graphql.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DionysusGraphQLServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private DionysusGraphQLService dionysusGraphQLService;

    @Test
    void health_returnsResponseFromRestApi() {
        String expected = "{\"status\":\"ok\"}";
        when(restTemplate.getForObject("https://filestore.scarlettparker.co.uk/api/health", String.class))
            .thenReturn(expected);

        String result = dionysusGraphQLService.health();

        assertThat(result).isEqualTo(expected);
    }
}
