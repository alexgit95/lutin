package com.alexgit95.lutin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Réglages de Cloudflare Access ; renseignés seulement en mode cloudflare. */
@ConfigurationProperties(prefix = "lutin.cloudflare")
public record CloudflareProperties(String teamDomain, String audience, String emailHmacSecret) {
}
