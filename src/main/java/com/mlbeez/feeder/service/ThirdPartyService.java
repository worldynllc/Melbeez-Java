package com.mlbeez.feeder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mlbeez.feeder.model.UserRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;


@Service
public class ThirdPartyService {

    private static final Logger logger = LoggerFactory.getLogger(ThirdPartyService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${akko.api.uri}")
    private  String akkoUri;

    @Value("${akko.api.key}")
    private  String akkoApiKey;

    public ThirdPartyService(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;

    }

    public void sendUserDetails(UserRequest userRequest) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(userRequest);
            logger.info("Sending payload: {}", jsonPayload);

        } catch (Exception e) {
            logger.error("Error serializing request or in API call: {}",e.getMessage());
        }

        webClient.post()
                .uri(akkoUri)
                .header("x-api-key", akkoApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(userRequest))
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(response ->
                    logger.info("Response received: " + response),
                        error ->logger.error("Error occurred: " + error.getMessage()),
                        () -> logger.info("Request completed successfully to Akko API")
                );
    }
}
