package com.alexgit95.lutin.transfert;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ImportEnCoursException extends RuntimeException {

    public ImportEnCoursException() {
        super("Un import est déjà en cours");
    }
}
