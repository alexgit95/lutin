package com.alexgit95.lutin.security;

import com.alexgit95.lutin.config.CloudflareProperties;
import com.alexgit95.lutin.config.ModeCloudflare;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CloudflareAccessConfigTest {

    @Configuration
    @EnableConfigurationProperties(CloudflareProperties.class)
    static class Support {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Support.class, CloudflareAccessConfig.class);

    private static CloudflareProperties proprietes(String domaine, String audience, String secret) {
        return new CloudflareProperties(domaine, audience, secret);
    }

    @Test
    void uneConfigurationCompleteEstAcceptee() {
        assertThatCode(() -> new CloudflareAccessConfig(
                proprietes(JetonsCloudflare.EQUIPE, "aud", JetonsCloudflare.SECRET))).doesNotThrowAnyException();
    }

    @Test
    void leDomaineDoitTerminerParCloudflareaccessSansSchema() {
        for (String domaine : new String[]{null, "", "exemple.com", "https://equipe.cloudflareaccess.com",
                "equipe.cloudflareaccess.com/chemin", "equipe.cloudflareaccess.com.evil.org"}) {
            assertThatThrownBy(() -> new CloudflareAccessConfig(proprietes(domaine, "aud", JetonsCloudflare.SECRET)))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("CF_ACCESS_TEAM_DOMAIN");
        }
    }

    @Test
    void lAudienceEstObligatoire() {
        for (String audience : new String[]{null, "", "  "}) {
            assertThatThrownBy(() -> new CloudflareAccessConfig(
                    proprietes(JetonsCloudflare.EQUIPE, audience, JetonsCloudflare.SECRET)))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("CF_ACCESS_AUD");
        }
    }

    @Test
    void leSecretDoitFaire32CaracteresAuMoins() {
        for (String secret : new String[]{null, "", "trop-court", "x".repeat(31)}) {
            assertThatThrownBy(() -> new CloudflareAccessConfig(proprietes(JetonsCloudflare.EQUIPE, "aud", secret)))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("EMAIL_HMAC_SECRET")
                    .satisfies(e -> {
                        if (secret != null && !secret.isEmpty()) {
                            assertThat(e.getMessage()).doesNotContain(secret);
                        }
                    });
        }
        assertThatCode(() -> new CloudflareAccessConfig(
                proprietes(JetonsCloudflare.EQUIPE, "aud", "x".repeat(32)))).doesNotThrowAnyException();
    }

    @Test
    void leDemarrageEchoueSiUneVariableManque() {
        runner.withPropertyValues("lutin.acces.mode=cloudflare",
                        "lutin.cloudflare.team-domain=" + JetonsCloudflare.EQUIPE,
                        "lutin.cloudflare.audience=aud")
                .run(contexte -> assertThat(contexte).hasFailed());
    }

    @Test
    void leDemarrageReussitAvecUneConfigurationComplete() {
        runner.withPropertyValues("lutin.acces.mode=cloudflare",
                        "lutin.cloudflare.team-domain=" + JetonsCloudflare.EQUIPE,
                        "lutin.cloudflare.audience=aud",
                        "lutin.cloudflare.email-hmac-secret=" + JetonsCloudflare.SECRET)
                .run(contexte -> assertThat(contexte).hasNotFailed().hasBean("cloudflareJwtDecoder"));
    }

    @Test
    void lAbsenceDeModeNeChargeAucuneConfigurationCloudflare() {
        runner.run(contexte -> assertThat(contexte).hasNotFailed().doesNotHaveBean("cloudflareJwtDecoder"));
    }

    @Test
    void leProfilProdActiveLeModeCloudflareEtExigeLesVariables() throws IOException {
        Properties prod = new Properties();
        try (var flux = new ClassPathResource("application-prod.properties").getInputStream()) {
            prod.load(flux);
        }

        assertThat(prod.getProperty("lutin.acces.mode")).isEqualTo("cloudflare");
        assertThat(prod.getProperty("lutin.cloudflare.team-domain")).isEqualTo("${CF_ACCESS_TEAM_DOMAIN}");
        assertThat(prod.getProperty("lutin.cloudflare.audience")).isEqualTo("${CF_ACCESS_AUD}");
        assertThat(prod.getProperty("lutin.cloudflare.email-hmac-secret")).isEqualTo("${EMAIL_HMAC_SECRET}");
        assertThat(prod.getProperty("lutin.invitation-key")).isEqualTo("${INVITATION_KEY:}");
    }

    @Test
    void laConfigurationCloudflareEstPorteeParLeMode() {
        assertThat(CloudflareAccessConfig.class.isAnnotationPresent(ModeCloudflare.class)).isTrue();
    }
}
