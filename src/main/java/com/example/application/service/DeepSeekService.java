package com.example.application.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.IOException;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import javax.sound.midi.SysexMessage;

@Service
@Slf4j
public class DeepSeekService {

    private final CloseableHttpClient httpClient;
    private final HttpPost httpPost;

    public DeepSeekService(CloseableHttpClient httpClient, HttpPost httpPost) {
        this.httpClient = httpClient;
        this.httpPost = httpPost;
    }

    public String generateText(String prompt) throws IOException {
        String requestBody = String.format("""
                {
                          "role": "user",
                                                 "content": [
                                                     {"type": "text", "text": "What's in this image?"},
                                                     {
                                                         "type": "image_url",
                                                         "image_url": {
                                                             "url": "https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Gfp-wisconsin-madison-the-nature-boardwalk.jpg/2560px-Gfp-wisconsin-madison-the-nature-boardwalk.jpg",
                                                         },
                                                     },
                                                 ],
                                             }],
                                              """, prompt);

        try {
            httpPost.setEntity(new StringEntity(requestBody));
            CloseableHttpResponse response = httpClient.execute(httpPost);
            return EntityUtils.toString(response.getEntity());

        } catch (IOException e) {
            log.error("API request failed: {}", e.getMessage());
            throw e;
        }
    }
}
