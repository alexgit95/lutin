package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.ApiKey;
import com.alexgit95.lutin.repository.ApiKeyRepository;
import com.alexgit95.lutin.vue.VueCleApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class CleApiServiceTest {

    private static final Instant T0 = Instant.parse("2026-11-10T08:12:00Z");
    private static final Instant T1 = Instant.parse("2026-11-11T21:30:00Z");

    @Autowired CleApiService service;
    @Autowired ApiKeyRepository cles;
    @MockitoBean Clock horloge;

    @BeforeEach
    void preparer() {
        cles.deleteAll();
        when(horloge.instant()).thenReturn(T0);
    }

    @AfterEach
    void nettoyer() {
        cles.deleteAll();
    }

    private ApiKey ligne(String nom) {
        return cles.findAll().stream().filter(k -> k.getNom().equals(nom)).findFirst().orElseThrow();
    }

    @Test
    void uneCleGenereeEstPrefixeeAleatoireEtSeulSonHacheEstConserve() {
        CleApiService.CleGeneree generee = service.generer("  nas-backup ");
        CleApiService.CleGeneree autre = service.generer("autre");

        assertThat(generee.nom()).isEqualTo("nas-backup");
        assertThat(generee.cle()).matches("lutin_[A-Za-z0-9_-]{43}").isNotEqualTo(autre.cle());
        ApiKey k = ligne("nas-backup");
        assertThat(k.getEmpreinte()).matches("[0-9a-f]{64}").isEqualTo(CleApiService.empreinte(generee.cle()))
                .doesNotContain(generee.cle());
        assertThat(k.getNom()).doesNotContain("lutin_");
        assertThat(k.getCreeLe()).isEqualTo(T0);
        assertThat(k.getDerniereUtilisation()).isNull();
    }

    @Test
    void lEmpreinteEstLeSha256HexadecimalDeLaCle() {
        assertThat(CleApiService.empreinte("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void plusieursClesRestentActivesEnMemeTemps() {
        String a = service.generer("a").cle();
        String b = service.generer("b").cle();

        assertThat(service.authentifier(a)).contains("a");
        assertThat(service.authentifier(b)).contains("b");
        assertThat(service.lister()).extracting(VueCleApi::nom).containsExactly("a", "b");
    }

    @Test
    void unNomAbsentOuTropLongEstRefuse() {
        for (String mauvais : new String[]{null, "", "   ", "\u0000\u202E\n"}) {
            assertThatThrownBy(() -> service.generer(mauvais))
                    .isInstanceOf(RequeteInvalideException.class).hasMessageContaining("obligatoire");
        }
        assertThatThrownBy(() -> service.generer("x".repeat(101)))
                .isInstanceOf(RequeteInvalideException.class).hasMessageContaining("trop long");
        assertThat(cles.count()).isZero();
        service.generer("x".repeat(100));
        assertThat(cles.count()).isEqualTo(1);
    }

    @Test
    void leNomEstNettoyeDesCaracteresDeControle() {
        assertThat(service.generer("Mon\u0000  NAS\n").nom()).isEqualTo("Mon NAS");
    }

    @Test
    void unNomDejaUtiliseEstRefuseSansTenirCompteDeLaCasseNiDesEspacesDeBordure() {
        service.generer("NAS");

        assertThatThrownBy(() -> service.generer("  nas "))
                .isInstanceOf(RequeteInvalideException.class).hasMessageContaining("déjà");
        assertThat(cles.count()).isEqualTo(1);
    }

    @Test
    void lePlafondDeVingtClesEstApplique() {
        for (int i = 0; i < CleApiService.MAX_CLES; i++) {
            service.generer("cle-" + i);
        }

        assertThatThrownBy(() -> service.generer("de-trop"))
                .isInstanceOf(RequeteInvalideException.class).hasMessageContaining("20");
        assertThat(cles.count()).isEqualTo(CleApiService.MAX_CLES);
        service.revoquer(ligne("cle-0").getId());
        service.generer("de-trop");
        assertThat(cles.count()).isEqualTo(CleApiService.MAX_CLES);
    }

    @Test
    void laListeAfficheLesDatesDansLeFuseauEtJamaisUtiliseeTantQueLaCleNaPasServi() {
        service.generer("nas");

        VueCleApi vue = service.lister().get(0);

        assertThat(vue.creeLe()).isEqualTo("10/11/2026 09:12");
        assertThat(vue.derniereUtilisation()).isNull();
    }

    @Test
    void laPremiereUtilisationRenseigneLaDate() {
        String cle = service.generer("nas").cle();
        when(horloge.instant()).thenReturn(T1);

        assertThat(service.authentifier(cle)).contains("nas");

        assertThat(ligne("nas").getDerniereUtilisation()).isEqualTo(T1);
        assertThat(service.lister().get(0).derniereUtilisation()).isEqualTo("11/11/2026 22:30");
    }

    @Test
    void lesUtilisationsSuccessivesGardentLaDateDeCreation() {
        String cle = service.generer("nas").cle();
        service.authentifier(cle);
        when(horloge.instant()).thenReturn(T1);

        service.authentifier(cle);

        assertThat(ligne("nas").getDerniereUtilisation()).isEqualTo(T1);
        assertThat(ligne("nas").getCreeLe()).isEqualTo(T0);
    }

    @Test
    void seuleLaCleUtiliseeVoitSaDateModifiee() {
        String a = service.generer("a").cle();
        service.generer("b");
        when(horloge.instant()).thenReturn(T1);

        service.authentifier(a);

        assertThat(ligne("a").getDerniereUtilisation()).isEqualTo(T1);
        assertThat(ligne("b").getDerniereUtilisation()).isNull();
    }

    @Test
    void unJetonInvalideNeModifieAucuneDate() {
        String cle = service.generer("nas").cle();
        when(horloge.instant()).thenReturn(T1);
        List<String> mauvais = java.util.Arrays.asList(null, "", " ", "lutin_inconnue", cle + "x",
                cle.toUpperCase(), "x".repeat(CleApiService.JETON_MAX + 1));

        for (String jeton : mauvais) {
            assertThat(service.authentifier(jeton)).as(String.valueOf(jeton)).isEmpty();
        }

        assertThat(ligne("nas").getDerniereUtilisation()).isNull();
    }

    @Test
    void uneCleRevoqueeNeFonctionnePlusEtUnIdentifiantInconnuEstIgnore() {
        String cle = service.generer("nas").cle();

        service.revoquer(ligne("nas").getId());
        service.revoquer(9999L);

        assertThat(service.authentifier(cle)).isEmpty();
        assertThat(service.lister()).isEmpty();
    }
}
