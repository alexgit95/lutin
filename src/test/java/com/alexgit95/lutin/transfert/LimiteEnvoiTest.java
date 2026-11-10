package com.alexgit95.lutin.transfert;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.core.io.ClassPathResource;

import com.alexgit95.lutin.web.TransfertAdvice;

import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class LimiteEnvoiTest {

    @Test
    void laLimiteDEnvoiEstDeCentMegaOctets() throws IOException {
        Properties proprietes = new Properties();
        try (var flux = new ClassPathResource("application.properties").getInputStream()) {
            proprietes.load(flux);
        }

        assertThat(proprietes.getProperty("spring.servlet.multipart.max-file-size")).isEqualTo("100MB");
        assertThat(proprietes.getProperty("spring.servlet.multipart.max-request-size")).isEqualTo("100MB");
    }

    @Test
    void unFichierTropGrosRevientSurLEcranAdminAvecUnMessageLisible() {
        MockHttpServletRequest requete = new MockHttpServletRequest();
        FlashMap flash = new FlashMap();
        requete.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, flash);

        String vue = new TransfertAdvice().fichierTropGros(requete);

        assertThat(vue).isEqualTo("redirect:/admin");
        assertThat(flash.get("erreur")).asString().contains("trop volumineux").contains("100 Mo");
    }
}
