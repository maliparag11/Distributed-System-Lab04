package com.example.shortener_service;

import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@Validated

public class ShortenerController {
    private final RestClient redirectClient;
    private final Random random = new Random();
    private final String redirectBaseUrl;

    public ShortenerController(
            RestClient.Builder builder,
            @Value("${redirect.baseUrl:http://redirect-service:8080}") String redirectBaseUrl
    ) {
        this.redirectClient = builder.baseUrl(redirectBaseUrl).build();
        this.redirectBaseUrl = redirectBaseUrl;
    }

    public record ShortenRequest(@NotBlank String longUrl, String customCode) {}

    public record ShortenResponse(String code, String shortUrl) {}

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "shortener");
    }

    @PostMapping("/shorten")
    public ResponseEntity<?> shorten(@Valid @RequestBody ShortenRequest request) {

        String code = (request.customCode() == null || request.customCode().isBlank())
                ? generateCode(6)
                : request.customCode();

        try {
            redirectClient.post()
                    .uri("/register")
                    .body(Map.of("code", code, "longUrl", request.longUrl()))
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "redirect-service unavailable", "details", ex.getMessage()));
        }

        String base = redirectBaseUrl.endsWith("/") ?
                redirectBaseUrl.substring(0, redirectBaseUrl.length() - 1) :
                redirectBaseUrl;

        String shortUrl = base + "/" + code;

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ShortenResponse(code, shortUrl));
    }

    private String generateCode(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
