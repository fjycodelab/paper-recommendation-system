package com.lencode.paper.download.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import com.lencode.paper.download.dto.DownloadedResource;

@Component
public class PaperDownloadClient {

    private final RestTemplate restTemplate;

    @Autowired
    public PaperDownloadClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${app.paper.download-timeout-ms}") long timeoutMs) {
        this(restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build());
    }

    PaperDownloadClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public DownloadedResource download(String downloadUrl) {
        return restTemplate.execute(downloadUrl, HttpMethod.GET, null, response -> {
            HttpHeaders headers = response.getHeaders();
            return new DownloadedResource(
                    StreamUtils.copyToByteArray(response.getBody()),
                    headers.getContentType(),
                    headers.getContentDisposition().getFilename()
            );
        });
    }
}
