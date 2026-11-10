package com.alexgit95.lutin.security;

import com.alexgit95.lutin.config.CloudflareProperties;
import com.alexgit95.lutin.config.ModeCloudflare;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Locale;

/** Seule empreinte de l'email conservée : HMAC-SHA256 hexadécimal de l'adresse normalisée. */
@Service
@ModeCloudflare
public class EmailHmacService {

    private static final String ALGORITHME = "HmacSHA256";

    private final SecretKeySpec cle;

    public EmailHmacService(CloudflareProperties proprietes) {
        this.cle = new SecretKeySpec(proprietes.emailHmacSecret().getBytes(StandardCharsets.UTF_8), ALGORITHME);
    }

    public String hacher(String email) {
        String normalise = email.trim().toLowerCase(Locale.ROOT);
        try {
            Mac mac = Mac.getInstance(ALGORITHME);
            mac.init(cle);
            return HexFormat.of().formatHex(mac.doFinal(normalise.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC indisponible", e);
        }
    }
}
