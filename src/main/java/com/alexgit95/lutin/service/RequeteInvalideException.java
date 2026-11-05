package com.alexgit95.lutin.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Demande invalide : message affichable à l'utilisateur. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RequeteInvalideException extends RuntimeException {

    public RequeteInvalideException(String message) {
        super(message);
    }
}
