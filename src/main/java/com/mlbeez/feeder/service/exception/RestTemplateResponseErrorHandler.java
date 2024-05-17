package com.mlbeez.feeder.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;



@Component
public class RestTemplateResponseErrorHandler implements ResponseErrorHandler {


    @Override
    public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
        // Check if the status code is in the CLIENT_ERROR or SERVER_ERROR series
        return (httpResponse.getStatusCode().is4xxClientError() || httpResponse.getStatusCode().is5xxServerError());
    }



    @Override
    public void handleError(ClientHttpResponse httpResponse) throws IOException {
        HttpStatus statusCode = (HttpStatus) httpResponse.getStatusCode();

        if (statusCode.is5xxServerError()) {
            // handle SERVER_ERROR
        } else if (statusCode.is4xxClientError()) {
            // handle CLIENT_ERROR
            if (statusCode == HttpStatus.NOT_FOUND) {
                throw new DataNotFoundException(httpResponse.toString(),httpResponse.getStatusText());

            }




        }
    }
}
