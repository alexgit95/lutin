package com.alexgit95.lutin.web;

import com.alexgit95.lutin.security.AccesAuthentication;
import com.alexgit95.lutin.service.DateDuJour;
import com.alexgit95.lutin.transfert.ExportService;
import com.alexgit95.lutin.transfert.IdentiteAdmin;
import com.alexgit95.lutin.transfert.ImportException;
import com.alexgit95.lutin.transfert.ImportService;
import com.alexgit95.lutin.transfert.ResumeImport;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.stream.Collectors;

/** Sauvegarde et restauration de toute la base, réservées à l'administrateur (voir {@code /admin/**}). */
@Controller
@RequestMapping("/admin")
public class AdminTransfertController {

    static final String MOT_DE_CONFIRMATION = "IMPORTER";

    private final ExportService export;
    private final ImportService importService;
    private final DateDuJour dateDuJour;

    public AdminTransfertController(ExportService export, ImportService importService, DateDuJour dateDuJour) {
        this.export = export;
        this.importService = importService;
        this.dateDuJour = dateDuJour;
    }

    @PostMapping("/export")
    public void exporter(Authentication authentication, HttpServletResponse reponse) throws IOException {
        reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        reponse.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        reponse.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"lutin-export-" + dateDuJour.aujourdhui() + ".json\"");
        export.exporter(reponse.getOutputStream(), authentication.getName());
    }

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String importer(@RequestParam(value = "fichier", required = false) MultipartFile fichier,
                           @RequestParam(defaultValue = "") String confirmation,
                           Authentication authentication, RedirectAttributes flash) {
        if (!MOT_DE_CONFIRMATION.equals(confirmation)) {
            flash.addFlashAttribute("erreur", "Saisissez exactement « " + MOT_DE_CONFIRMATION + " » pour confirmer");
            return "redirect:/admin";
        }
        if (fichier == null || fichier.isEmpty()) {
            flash.addFlashAttribute("erreur", "Choisissez le fichier d'export à importer");
            return "redirect:/admin";
        }
        try {
            ResumeImport resume = importService.importer(fichier::getInputStream, identite(authentication));
            flash.addFlashAttribute("info", "Import terminé : " + resume.total() + " lignes ("
                    + resume.lignesParTable().entrySet().stream().filter(e -> e.getValue() > 0)
                    .map(e -> e.getKey() + " " + e.getValue()).collect(Collectors.joining(", ")) + ")");
        } catch (ImportException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin";
    }

    private static IdentiteAdmin identite(Authentication authentication) {
        String empreinte = authentication instanceof AccesAuthentication acces ? acces.getEmailHmac() : null;
        return new IdentiteAdmin(authentication.getName(), empreinte);
    }
}
