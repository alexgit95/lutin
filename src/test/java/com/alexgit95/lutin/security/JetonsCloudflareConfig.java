package com.alexgit95.lutin.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.interfaces.RSAPublicKey;

/** Décodeur de test : mêmes validateurs que la production, clés locales à la place de la JWKS du team. */
@TestConfiguration
public class JetonsCloudflareConfig {

    @Bean
    @Primary
    JwtDecoder decodeurDeTest() {
        NimbusJwtDecoder decodeur = NimbusJwtDecoder
                .withPublicKey((RSAPublicKey) JetonsCloudflare.CLE.getPublic()).build();
        decodeur.setJwtValidator(CloudflareAccessConfig.validateurs(JetonsCloudflare.EMETTEUR, JetonsCloudflare.AUDIENCE));
        return decodeur;
    }
}
