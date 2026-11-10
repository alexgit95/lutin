package com.alexgit95.lutin.web;

import com.alexgit95.lutin.service.DateDuJour;
import com.alexgit95.lutin.transfert.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/** Sauvegarde de la base pour les scripts : authentifiée par clé d'API, voir {@code SecurityApiConfig}. */
@RestController
@RequestMapping("/api")
@Tag(name = "Sauvegarde", description = "Récupération de la sauvegarde de la base par clé d'API")
@SecurityScheme(name = "cleApi", type = SecuritySchemeType.HTTP, scheme = "bearer",
        description = "Clé d'API de sauvegarde générée dans l'écran d'administration")
public class ApiSauvegardeController {

    private final ExportService export;
    private final DateDuJour dateDuJour;

    public ApiSauvegardeController(ExportService export, DateDuJour dateDuJour) {
        this.export = export;
        this.dateDuJour = dateDuJour;
    }

    @GetMapping(path = "/sauvegarde", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Télécharger la sauvegarde complète de la base",
            description = "Renvoie le même fichier JSON que l'export de l'administrateur, produit en flux.",
            security = @SecurityRequirement(name = "cleApi"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fichier d'export lutin-export-AAAA-MM-JJ.json",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "401", description = "Clé absente, mal formée, inconnue ou révoquée",
                    content = @Content)})
    public void sauvegarder(Authentication authentication, HttpServletResponse reponse) throws IOException {
        reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        reponse.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        reponse.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"lutin-export-" + dateDuJour.aujourdhui() + ".json\"");
        export.exporter(reponse.getOutputStream(), "clé d'API « " + authentication.getName() + " »");
    }
}
