package com.alexgit95.lutin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Clé d'invitation fixe exigée à l'inscription (variable d'environnement INVITATION_KEY). */
@ConfigurationProperties(prefix = "lutin")
public record LutinProperties(String invitationKey) {
}
