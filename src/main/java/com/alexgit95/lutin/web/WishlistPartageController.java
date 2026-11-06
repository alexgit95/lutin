package com.alexgit95.lutin.web;

import com.alexgit95.lutin.service.RequeteInvalideException;
import com.alexgit95.lutin.service.WishlistPartageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class WishlistPartageController {

    private final WishlistPartageService partages;

    public WishlistPartageController(WishlistPartageService partages) {
        this.partages = partages;
    }

    @GetMapping("/wishlists/{id}/partages")
    public String afficher(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("wishlistId", id);
        model.addAttribute("partages", partages.lister(id, principal.getName()));
        model.addAttribute("candidats", partages.candidats(id, principal.getName()));
        return "wishlist-partages";
    }

    @PostMapping("/wishlists/{id}/partages")
    public String partager(@PathVariable Long id, @RequestParam String lecteur, Principal principal,
                           RedirectAttributes flash) {
        try {
            partages.partager(id, principal.getName(), lecteur);
        } catch (RequeteInvalideException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/wishlists/" + id + "/partages";
    }

    @PostMapping("/wishlists/{id}/partages/{partageId}/retirer")
    public String retirer(@PathVariable Long id, @PathVariable Long partageId, Principal principal) {
        partages.retirer(id, partageId, principal.getName());
        return "redirect:/wishlists/" + id + "/partages";
    }
}
