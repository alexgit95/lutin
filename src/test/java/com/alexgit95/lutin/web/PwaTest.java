package com.alexgit95.lutin.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PwaTest {

    @Autowired MockMvc mvc;

    @Test
    void leManifesteEstPublicEtInstallable() throws Exception {
        mvc.perform(get("/manifest.webmanifest")).andExpect(status().isOk())
                .andExpect(content().contentType("application/manifest+json"))
                .andExpect(content().string(containsString("\"display\": \"standalone\"")))
                .andExpect(content().string(containsString("\"start_url\": \"/\"")))
                .andExpect(content().string(containsString("icon-192.png")))
                .andExpect(content().string(containsString("icon-512.png")));
    }

    @Test
    void leServiceWorkerEstPublicEtSansCacheHorsLigne() throws Exception {
        mvc.perform(get("/sw.js")).andExpect(status().isOk())
                .andExpect(content().string(containsString("fetch")))
                .andExpect(content().string(not(containsString("caches."))));
    }

    @Test
    void lesIconesSontPubliques() throws Exception {
        mvc.perform(get("/icons/icon-192.png")).andExpect(status().isOk());
        mvc.perform(get("/icons/icon-512.png")).andExpect(status().isOk());
    }

    @Test
    void lesPagesPubliquesDeclarentLeManifesteEtEnregistrentLeServiceWorker() throws Exception {
        for (String page : new String[]{"/login", "/register"}) {
            mvc.perform(get(page)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("rel=\"manifest\"")))
                    .andExpect(content().string(containsString("/js/app.js")));
        }
    }

    @Test
    void lEnregistrementDuServiceWorkerPointeSurLaRacine() throws Exception {
        mvc.perform(get("/js/app.js")).andExpect(status().isOk())
                .andExpect(content().string(containsString("register('/sw.js')")));
    }
}
