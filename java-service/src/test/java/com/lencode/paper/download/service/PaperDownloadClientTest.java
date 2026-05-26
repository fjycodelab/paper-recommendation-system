package com.lencode.paper.download.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.lencode.paper.download.dto.DownloadedResource;

class PaperDownloadClientTest {

    @Test
    void downloadsBodyAndHeadersFromHttpResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("https://example.com/demo.pdf"))
                .andRespond(withSuccess("%PDF-1.4", MediaType.APPLICATION_PDF));

        PaperDownloadClient client = new PaperDownloadClient(restTemplate);

        DownloadedResource resource = client.download("https://example.com/demo.pdf");

        assertThat(new String(resource.getBody())).isEqualTo("%PDF-1.4");
        assertThat(resource.getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(resource.getFileName()).isNull();
        server.verify();
    }
}
