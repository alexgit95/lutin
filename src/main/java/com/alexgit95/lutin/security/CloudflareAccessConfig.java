package com.alexgit95.lutin.security;

import com.alexgit95.lutin.config.CloudflareProperties;
import com.alexgit95.lutin.config.ModeCloudflare;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.List;
import java.util.regex.Pattern;

/** Paramètres et décodeur de jeton de Cloudflare Access ; le démarrage échoue si la configuration est incomplète. */
@Configuration
@ModeCloudflare
public class CloudflareAccessConfig {

    static final int SECRET_MIN = 32;
    private static final Pattern DOMAINE = Pattern.compile("^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*\\.cloudflareaccess\\.com$");

    private final CloudflareProperties proprietes;

    public CloudflareAccessConfig(CloudflareProperties proprietes) {
        if (proprietes.teamDomain() == null || !DOMAINE.matcher(proprietes.teamDomain()).matches()) {
            throw new IllegalStateException(
                    "CF_ACCESS_TEAM_DOMAIN doit être un domaine se terminant par .cloudflareaccess.com, sans schéma");
        }
        if (proprietes.audience() == null || proprietes.audience().isBlank()) {
            throw new IllegalStateException("CF_ACCESS_AUD est obligatoire");
        }
        if (proprietes.emailHmacSecret() == null || proprietes.emailHmacSecret().length() < SECRET_MIN) {
            throw new IllegalStateException("EMAIL_HMAC_SECRET est obligatoire (" + SECRET_MIN + " caractères minimum)");
        }
        this.proprietes = proprietes;
    }

    @Bean
    JwtDecoder cloudflareJwtDecoder() {
        String emetteur = "https://" + proprietes.teamDomain();
        NimbusJwtDecoder decodeur = NimbusJwtDecoder.withJwkSetUri(emetteur + "/cdn-cgi/access/certs")
                .jwsAlgorithm(SignatureAlgorithm.RS256).build();
        decodeur.setJwtValidator(validateurs(emetteur, proprietes.audience()));
        return decodeur;
    }

    /** Expiration, émetteur du team et audience de l'application : l'audience écarte les jetons d'une autre application du team. */
    public static OAuth2TokenValidator<Jwt> validateurs(String emetteur, String audience) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(emetteur),
                new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains(audience)));
    }
}
