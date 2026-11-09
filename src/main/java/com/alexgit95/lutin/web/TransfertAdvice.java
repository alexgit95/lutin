package com.alexgit95.lutin.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.support.RequestContextUtils;

/** Un fichier trop gros est refusé avant d'atteindre le contrôleur : retour sur l'écran Admin avec un message lisible. */
@ControllerAdvice
public class TransfertAdvice {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String fichierTropGros(HttpServletRequest requete) {
        RequestContextUtils.getOutputFlashMap(requete)
                .put("erreur", "Fichier trop volumineux : la limite est de 100 Mo");
        return "redirect:/admin";
    }
}
