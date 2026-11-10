package com.alexgit95.lutin.web;

import com.alexgit95.lutin.config.ModeLocal;
import com.alexgit95.lutin.service.InscriptionException;
import com.alexgit95.lutin.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@ModeLocal
public class AuthController {

    private final RegistrationService registrationService;

    public AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form, BindingResult result) {
        if (result.hasErrors()) {
            return "register";
        }
        try {
            registrationService.register(form.getLogin(), form.getPassword(), form.getInvitationKey());
        } catch (InscriptionException e) {
            switch (e.getMotif()) {
                case CLE_INVALIDE -> result.rejectValue("invitationKey", "invitation.invalide", "Clé d'invitation invalide");
                case LOGIN_UTILISE -> result.rejectValue("login", "login.utilise", "Ce login est déjà utilisé");
            }
            return "register";
        }
        return "redirect:/login?inscrit";
    }
}
