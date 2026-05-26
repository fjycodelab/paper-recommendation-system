package com.lencode.paper.importer.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ArxivClient {

    private final RestTemplate restTemplate;
    private final String apiUrl;

    @Autowired
    public ArxivClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${app.paper.arxiv.api-url}") String apiUrl,
            @Value("${app.paper.arxiv.timeout-ms}") long timeoutMs) {
        this(restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build(), apiUrl);
    }

    ArxivClient(RestTemplate restTemplate, String apiUrl) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
    }

    public String fetch(String query, int maxResults) {
        String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("search_query", query)
                .queryParam("start", 0)
                .queryParam("max_results", maxResults)
                .queryParam("sortBy", "submittedDate")
                .queryParam("sortOrder", "descending")
                .toUriString();
        return restTemplate.getForObject(url, String.class);
    }
}
