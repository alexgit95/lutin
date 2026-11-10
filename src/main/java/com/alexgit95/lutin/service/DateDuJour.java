package com.alexgit95.lutin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** Date du jour dans le fuseau de l'utilisateur : l'horloge de l'application est en UTC. */
@Component
public class DateDuJour {

    private final Clock horloge;
    private final ZoneId fuseau;

    public DateDuJour(Clock horloge, @Value("${lutin.fuseau-horaire:Europe/Paris}") ZoneId fuseau) {
        this.horloge = horloge;
        this.fuseau = fuseau;
    }

    public LocalDate aujourdhui() {
        return LocalDate.now(horloge.withZone(fuseau));
    }
}
