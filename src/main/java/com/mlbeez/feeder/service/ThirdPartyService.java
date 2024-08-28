package com.mlbeez.feeder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mlbeez.feeder.model.UserRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ThirdPartyService {

    private static final Logger logger = LoggerFactory.getLogger(ThirdPartyService.class);
    private final WebClient webClient;

    public ThirdPartyService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> sendUserDetails(UserRequest userRequest) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonPayload = objectMapper.writeValueAsString(userRequest);
            logger.info("Sending payload: {}", jsonPayload);
        } catch (Exception e) {
            logger.error("Error serializing request: ", e);
        }


        return webClient.post()
                .uri("https://api-gateway.staging.cloud.getakko.com/api/v2/partners/users/")
                .header("x-api-key", "eaff5c23-f23e-4eb3-8872-41abfd732d8b")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(BodyInserters.fromValue(userRequest))
                .retrieve()
                .bodyToMono(String.class);
    }
}
