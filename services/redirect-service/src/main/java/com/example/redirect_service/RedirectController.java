package com.example.redirect_service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
public class RedirectController {
    private final Map<String, String> store = new ConcurrentHashMap<>();

    public record RegisterRequest(
            @NotBlank String code,
            @NotBlank String longUrl
            ) {

    }

    public record ResolveResponse(String code, String longUrl) {

    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest body) {
        if (store.containsKey(body.code())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "code already exists"));
        }

        store.put(body.code(), body.longUrl());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> resolve(@PathVariable("code") String code) {
        String longUrl = store.get(code);

        if (longUrl == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "code not found"));
        }

        return ResponseEntity.ok(new ResolveResponse(code, longUrl));
    }

    
}
