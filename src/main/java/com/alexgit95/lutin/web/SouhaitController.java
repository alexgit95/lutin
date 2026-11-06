package com.alexgit95.lutin.web;

import com.alexgit95.lutin.service.AccesWishlistService;
import com.alexgit95.lutin.service.RequeteInvalideException;
import com.alexgit95.lutin.service.SouhaitService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/wishlists/{wishlistId}/souhaits")
public class SouhaitController {

    private final SouhaitService souhaits;
    private final AccesWishlistService acces;

    public SouhaitController(SouhaitService souhaits, AccesWishlistService acces) {
        this.souhaits = souhaits;
        this.acces = acces;
    }

    @GetMapping("/nouveau")
    public String nouveau(@PathVariable Long wishlistId, Principal principal, Model model) {
        acces.proprietaire(wishlistId, principal.getName());
        return formulaire(model, wishlistId, null, new SouhaitForm());
    }

    @PostMapping
    public String creer(@PathVariable Long wishlistId, @ModelAttribute("form") SouhaitForm form,
                        BindingResult liaison, Principal principal, Model model) {
        if (liaison.hasErrors()) {
            model.addAttribute("erreur", "Prix invalide");
            return formulaire(model, wishlistId, null, form);
        }
        try {
            souhaits.creer(wishlistId, principal.getName(), souhaits.completerNom(form.versCommande()));
        } catch (RequeteInvalideException e) {
            model.addAttribute("erreur", e.getMessage());
            return formulaire(model, wishlistId, null, form);
        }
        return "redirect:/wishlists/" + wishlistId;
    }

    @GetMapping("/{souhaitId}/modifier")
    public String modifierFormulaire(@PathVariable Long wishlistId, @PathVariable Long souhaitId,
                                     Principal principal, Model model) {
        SouhaitForm form = SouhaitForm.depuis(souhaits.obtenir(wishlistId, souhaitId, principal.getName()));
        return formulaire(model, wishlistId, souhaitId, form);
    }

    @PostMapping("/{souhaitId}")
    public String modifier(@PathVariable Long wishlistId, @PathVariable Long souhaitId,
                           @ModelAttribute("form") SouhaitForm form, BindingResult liaison,
                           Principal principal, Model model) {
        if (liaison.hasErrors()) {
            model.addAttribute("erreur", "Prix invalide");
            return formulaire(model, wishlistId, souhaitId, form);
        }
        try {
            souhaits.modifier(wishlistId, souhaitId, principal.getName(), souhaits.completerNom(form.versCommande()));
        } catch (RequeteInvalideException e) {
            model.addAttribute("erreur", e.getMessage());
            return formulaire(model, wishlistId, souhaitId, form);
        }
        return "redirect:/wishlists/" + wishlistId;
    }

    @PostMapping("/{souhaitId}/supprimer")
    public String supprimer(@PathVariable Long wishlistId, @PathVariable Long souhaitId, Principal principal) {
        souhaits.supprimer(wishlistId, souhaitId, principal.getName());
        return "redirect:/wishlists/" + wishlistId;
    }

    @PostMapping("/{souhaitId}/pris")
    public String prendre(@PathVariable Long wishlistId, @PathVariable Long souhaitId,
                          @RequestParam(defaultValue = "0") int page, Principal principal,
                          RedirectAttributes flash) {
        try {
            souhaits.prendre(wishlistId, souhaitId, principal.getName());
        } catch (RequeteInvalideException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return retour(wishlistId, page);
    }

    /** Sans confirmation, un article pris par un autre n'est pas libéré : un avertissement est affiché. */
    @PostMapping("/{souhaitId}/libere")
    public String liberer(@PathVariable Long wishlistId, @PathVariable Long souhaitId,
                          @RequestParam(defaultValue = "false") boolean confirme,
                          @RequestParam(defaultValue = "0") int page, Principal principal, Model model) {
        SouhaitService.ResultatLiberation resultat =
                souhaits.liberer(wishlistId, souhaitId, principal.getName(), confirme);
        if (resultat.libere()) {
            return retour(wishlistId, page);
        }
        model.addAttribute("wishlistId", wishlistId);
        model.addAttribute("souhaitId", souhaitId);
        model.addAttribute("preneur", resultat.preneur());
        model.addAttribute("page", Math.max(page, 0));
        return "souhait-confirmation";
    }

    private String retour(Long wishlistId, int page) {
        return "redirect:/wishlists/" + wishlistId + (page > 0 ? "?page=" + page : "");
    }

    private String formulaire(Model model, Long wishlistId, Long souhaitId, SouhaitForm form) {
        model.addAttribute("form", form);
        model.addAttribute("wishlistId", wishlistId);
        model.addAttribute("souhaitId", souhaitId);
        return "souhait-form";
    }
}
