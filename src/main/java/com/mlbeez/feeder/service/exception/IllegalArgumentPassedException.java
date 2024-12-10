package com.mlbeez.feeder.service.exception;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class IllegalArgumentPassedException extends RuntimeException{

    public IllegalArgumentPassedException(String message) {
        super(message);
    }

}
