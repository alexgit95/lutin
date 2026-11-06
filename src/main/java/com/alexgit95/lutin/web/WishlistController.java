package com.alexgit95.lutin.web;

import com.alexgit95.lutin.domain.Wishlist;
import com.alexgit95.lutin.service.AccesWishlistService;
import com.alexgit95.lutin.service.AvisService;
import com.alexgit95.lutin.service.RequeteInvalideException;
import com.alexgit95.lutin.service.SouhaitService;
import com.alexgit95.lutin.service.WishlistService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.security.Principal;

@Controller
@RequestMapping("/wishlists")
public class WishlistController {

    private final WishlistService wishlists;
    private final SouhaitService souhaits;
    private final AccesWishlistService acces;
    private final AvisService avis;

    public WishlistController(WishlistService wishlists, SouhaitService souhaits, AccesWishlistService acces,
                              AvisService avis) {
        this.wishlists = wishlists;
        this.souhaits = souhaits;
        this.acces = acces;
        this.avis = avis;
    }

    @GetMapping
    public String accueil(@RequestParam(defaultValue = "0") int pageAvis, Principal principal, Model model) {
        avis.purgerExpires();
        model.addAttribute("mesWishlists", wishlists.mesWishlists(principal.getName()));
        model.addAttribute("wishlistsPartagees", wishlists.wishlistsPartagees(principal.getName()));
        model.addAttribute("avis", avis.lister(principal.getName(), pageAvis));
        return "wishlists";
    }

    @PostMapping
    public String creer(@RequestParam String nom, Principal principal, RedirectAttributes flash) {
        try {
            Wishlist wishlist = wishlists.creer(principal.getName(), nom);
            return "redirect:/wishlists/" + wishlist.getId();
        } catch (RequeteInvalideException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/wishlists";
        }
    }

    /** Deux vues et deux gabarits distincts : celui du propriétaire ne manipule jamais l'état « pris ». */
    @GetMapping("/{id}")
    public String afficher(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
                           Principal principal, Model model) {
        if (acces.consultation(id, principal.getName()).proprietaire()) {
            model.addAttribute("vue", souhaits.vueProprietaire(id, principal.getName(), page));
            String jeton = wishlists.jetonPublic(id, principal.getName());
            model.addAttribute("lienPublic", jeton == null ? null : lien(jeton));
            return "wishlist";
        }
        model.addAttribute("vue", souhaits.vueLecteur(id, principal.getName(), page));
        model.addAttribute("avis", avis.pourWishlist(principal.getName(), id));
        return "wishlist-lecteur";
    }

    @GetMapping("/{id}/reglages")
    public String reglages(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("wishlistId", id);
        model.addAttribute("nomWishlist", wishlists.nom(id, principal.getName()));
        String jeton = wishlists.jetonPublic(id, principal.getName());
        model.addAttribute("lienPublic", jeton == null ? null : lien(jeton));
        return "wishlist-reglages";
    }

    @PostMapping("/{id}/public/activer")
    public String rendrePublique(@PathVariable Long id, Principal principal) {
        wishlists.rendrePublique(id, principal.getName());
        return "redirect:/wishlists/" + id + "/reglages";
    }

    @PostMapping("/{id}/public/regenerer")
    public String regenererLien(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        try {
            wishlists.regenererLien(id, principal.getName());
        } catch (RequeteInvalideException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/wishlists/" + id + "/reglages";
    }

    @PostMapping("/{id}/public/desactiver")
    public String rendrePrivee(@PathVariable Long id, Principal principal) {
        wishlists.rendrePrivee(id, principal.getName());
        return "redirect:/wishlists/" + id + "/reglages";
    }

    private static String lien(String jeton) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/p/").path(jeton).toUriString();
    }

    @PostMapping("/{id}/renommer")
    public String renommer(@PathVariable Long id, @RequestParam String nom, Principal principal,
                           RedirectAttributes flash) {
        try {
            wishlists.renommer(id, principal.getName(), nom);
        } catch (RequeteInvalideException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/wishlists/" + id + "/reglages";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id, Principal principal) {
        wishlists.supprimer(id, principal.getName());
        return "redirect:/wishlists";
    }
}
