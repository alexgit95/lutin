package com.alexgit95.lutin.web;

import com.alexgit95.lutin.service.ListeAchatService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class HomeController {

    private final ListeAchatService listes;

    public HomeController(ListeAchatService listes) {
        this.listes = listes;
    }

    @GetMapping("/")
    public String home(Principal principal, Model model) {
        model.addAttribute("mesListes", listes.mesListes(principal.getName()));
        model.addAttribute("listesPartagees", listes.listesPartagees(principal.getName()));
        return "home";
    }
}
