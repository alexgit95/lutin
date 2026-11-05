package com.alexgit95.lutin.web;

import com.alexgit95.lutin.service.RequeteInvalideException;
import com.alexgit95.lutin.service.TagService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class TagController {

    private final TagService tags;

    public TagController(TagService tags) {
        this.tags = tags;
    }

    @PostMapping("/listes/{listeId}/tags")
    public String creer(@PathVariable Long listeId, @RequestParam String nom, Principal principal,
                        RedirectAttributes flash) {
        try {
            tags.creer(listeId, principal.getName(), nom);
        } catch (RequeteInvalideException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/listes/" + listeId;
    }
}
