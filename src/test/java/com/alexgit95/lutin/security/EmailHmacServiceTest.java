package com.alexgit95.lutin.security;

import com.alexgit95.lutin.config.CloudflareProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailHmacServiceTest {

    private static EmailHmacService service(String secret) {
        return new EmailHmacService(new CloudflareProperties(JetonsCloudflare.EQUIPE, "aud", secret));
    }

    @Test
    void lEmpreinteEstUnHexadecimalDe64Caracteres() {
        String empreinte = service(JetonsCloudflare.SECRET).hacher("alice@example.org");

        assertThat(empreinte).hasSize(64).matches("[0-9a-f]{64}").doesNotContain("alice");
    }

    @Test
    void laCasseEtLesEspacesEnBordureNeChangentPasLEmpreinte() {
        EmailHmacService service = service(JetonsCloudflare.SECRET);

        assertThat(service.hacher("  Alice@Example.ORG "))
                .isEqualTo(service.hacher("alice@example.org"));
    }

    @Test
    void desAdressesDifferentesDonnentDesEmpreintesDifferentes() {
        EmailHmacService service = service(JetonsCloudflare.SECRET);

        assertThat(service.hacher("alice@example.org")).isNotEqualTo(service.hacher("bob@example.org"));
    }

    @Test
    void unAutreSecretDonneUneAutreEmpreinte() {
        String a = service(JetonsCloudflare.SECRET).hacher("alice@example.org");
        String b = service("un-tout-autre-secret-de-plus-de-32-caracteres").hacher("alice@example.org");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void lEmpreinteEstStable() {
        EmailHmacService service = service(JetonsCloudflare.SECRET);

        assertThat(service.hacher("alice@example.org")).isEqualTo(service.hacher("alice@example.org"));
    }
}
