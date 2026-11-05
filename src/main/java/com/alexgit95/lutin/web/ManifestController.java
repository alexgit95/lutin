package com.alexgit95.lutin.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ManifestController {

    private static final Resource MANIFESTE = new ClassPathResource("static/manifest.webmanifest");

    // Le gestionnaire de ressources statiques le servirait en application/octet-stream.
    @GetMapping(value = "/manifest.webmanifest", produces = "application/manifest+json")
    @ResponseBody
    public Resource manifeste() {
        return MANIFESTE;
    }
}
