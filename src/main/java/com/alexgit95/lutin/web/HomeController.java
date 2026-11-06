package com.alexgit95.lutin.web;

import com.alexgit95.lutin.service.ListeAchatService;
import com.alexgit95.lutin.service.WishlistService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class HomeController {

    private final ListeAchatService listes;
    private final WishlistService wishlists;

    public HomeController(ListeAchatService listes, WishlistService wishlists) {
        this.listes = listes;
        this.wishlists = wishlists;
    }

    @GetMapping("/")
    public String accueil(Principal principal, Model model) {
        String login = principal.getName();
        model.addAttribute("nombreListes", listes.mesListes(login).size());
        model.addAttribute("nombreListesPartagees", listes.listesPartagees(login).size());
        model.addAttribute("nombreWishlists", wishlists.mesWishlists(login).size());
        model.addAttribute("nombreWishlistsPartagees", wishlists.wishlistsPartagees(login).size());
        return "accueil";
    }

    @GetMapping("/listes")
    public String mesListes(Principal principal, Model model) {
        model.addAttribute("mesListes", listes.mesListes(principal.getName()));
        model.addAttribute("listesPartagees", listes.listesPartagees(principal.getName()));
        return "home";
    }
}
