package com.alexgit95.lutin.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JetonCloudflareTest {

    private final JwtDecoder decodeur = decodeur();

    private static JwtDecoder decodeur() {
        NimbusJwtDecoder d = NimbusJwtDecoder.withPublicKey((RSAPublicKey) JetonsCloudflare.CLE.getPublic()).build();
        d.setJwtValidator(CloudflareAccessConfig.validateurs(JetonsCloudflare.EMETTEUR, JetonsCloudflare.AUDIENCE));
        return d;
    }

    private static Instant dansUneHeure() {
        return Instant.now().plus(Duration.ofHours(1));
    }

    @Test
    void unJetonValideEstAccepte() {
        Jwt jwt = decodeur.decode(JetonsCloudflare.valide("alice@example.org"));

        assertThat(jwt.getClaimAsString("email")).isEqualTo("alice@example.org");
    }

    @Test
    void unJetonExpireEstRefuse() {
        String jeton = JetonsCloudflare.jeton("alice@example.org", JetonsCloudflare.AUDIENCE,
                JetonsCloudflare.EMETTEUR, Instant.now().minus(Duration.ofMinutes(10)), JetonsCloudflare.CLE);

        assertThatThrownBy(() -> decodeur.decode(jeton)).isInstanceOf(JwtException.class);
    }

    @Test
    void uneMauvaiseAudienceEstRefusee() {
        String jeton = JetonsCloudflare.jeton("alice@example.org", "autre-application",
                JetonsCloudflare.EMETTEUR, dansUneHeure(), JetonsCloudflare.CLE);

        assertThatThrownBy(() -> decodeur.decode(jeton)).isInstanceOf(JwtException.class);
    }

    @Test
    void unMauvaisEmetteurEstRefuse() {
        String jeton = JetonsCloudflare.jeton("alice@example.org", JetonsCloudflare.AUDIENCE,
                "https://autre.cloudflareaccess.com", dansUneHeure(), JetonsCloudflare.CLE);

        assertThatThrownBy(() -> decodeur.decode(jeton)).isInstanceOf(JwtException.class);
    }

    @Test
    void uneSignatureFalsifieeEstRefusee() {
        String jeton = JetonsCloudflare.jeton("alice@example.org", JetonsCloudflare.AUDIENCE,
                JetonsCloudflare.EMETTEUR, dansUneHeure(), JetonsCloudflare.AUTRE_CLE);

        assertThatThrownBy(() -> decodeur.decode(jeton)).isInstanceOf(JwtException.class);
    }

    @Test
    void unJetonModifieEstRefuse() {
        String[] parties = JetonsCloudflare.valide("alice@example.org").split("\\.");
        String altere = parties[0] + "." + parties[1] + "A." + parties[2];

        assertThatThrownBy(() -> decodeur.decode(altere)).isInstanceOf(JwtException.class);
    }

    @Test
    void unTexteQuelconqueEstRefuse() {
        assertThatThrownBy(() -> decodeur.decode("n'importe quoi")).isInstanceOf(JwtException.class);
    }
}
