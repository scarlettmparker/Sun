package com.sun.dionysus.graphql.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
}
