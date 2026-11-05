package com.alexgit95.lutin.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ElementIntrouvableException extends RuntimeException {

    public ElementIntrouvableException(String message) {
        super(message);
    }
}
