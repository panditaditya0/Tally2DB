package com.tally_backup.tally_backup.Services;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GChatApiClient {
    private final RestTemplate restTemplate = new RestTemplate();

    public void sendToGoogleChat(String message) {
        String webhookUrl = "https://chat.googleapis.com/v1/spaces/AAQABhAAA3E/messages?key=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI&token=-jvWmESGAjRgYl3k8G6LzIn31TpQDGcTwvFVkFQ8vOA";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> payload = Map.of("text", message);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
        restTemplate.postForObject(webhookUrl, request, String.class);
    }
}