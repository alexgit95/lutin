package com.alexgit95.lutin.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Fabrique de jetons Cloudflare Access signés avec une clé locale de test. */
public final class JetonsCloudflare {

    public static final String EQUIPE = "equipe.cloudflareaccess.com";
    public static final String EMETTEUR = "https://" + EQUIPE;
    public static final String AUDIENCE = "aud-lutin";
    public static final String SECRET = "secret-de-test-0123456789-abcdefghij";

    public static final KeyPair CLE = nouvelleCle();
    public static final KeyPair AUTRE_CLE = nouvelleCle();

    private JetonsCloudflare() {
    }

    public static String valide(String email) {
        return jeton(email, AUDIENCE, EMETTEUR, Instant.now().plus(Duration.ofHours(1)), CLE);
    }

    public static String jeton(String email, String audience, String emetteur, Instant expiration, KeyPair signataire) {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(emetteur).audience(audience).subject("utilisateur-test")
                .issueTime(Date.from(Instant.now().minusSeconds(30)))
                .expirationTime(Date.from(expiration));
        if (email != null) {
            claims.claim("email", email);
        }
        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID("test").build(),
                    claims.build());
            jwt.sign(new RSASSASigner(signataire.getPrivate()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static KeyPair nouvelleCle() {
        try {
            KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
            generateur.initialize(2048);
            return generateur.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
