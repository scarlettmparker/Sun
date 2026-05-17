package com.sun.dionysus.graphql.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import com.sun.dionysus.codegen.types.Bucket;
import java.util.Arrays;
import java.util.List;

@Service
public class FilestoreGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(FilestoreGraphQLService.class);

  @Autowired
  private RestTemplate restTemplate;

  public String health() {
    logger.info("Calling external health REST API");
    String url = "https://filestore.scarlettparker.co.uk/api/health";
    String response = restTemplate.getForObject(url, String.class);
    logger.info("Health response: {}", response);
    return response;
  }

  public List<Bucket> listBuckets() {
    logger.info("Calling ListBuckets REST API");
    String url = "https://filestore.scarlettparker.co.uk/api/v2/ListBuckets";
    ResponseEntity<Bucket[]> resp = authenticatedGet(url, Bucket[].class);
    Bucket[] buckets = resp.getBody();
    List<Bucket> result = buckets != null ? Arrays.asList(buckets) : List.of();
    logger.info("Retrieved {} buckets", result.size());
    return result;
  }

  private <T> ResponseEntity<T> authenticatedGet(String url, Class<T> responseType) {
    String token = System.getProperty("GARAGE_SECRET_KEY");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    return restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
  }
}
