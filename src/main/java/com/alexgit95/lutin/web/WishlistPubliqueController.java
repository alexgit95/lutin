package com.alexgit95.lutin.web;

import com.alexgit95.lutin.service.RequeteInvalideException;
import com.alexgit95.lutin.service.SouhaitService;
import com.alexgit95.lutin.service.WishlistPubliqueService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Wishlist publique : accessible avec le jeton, sans connexion. Aucune route d'édition. */
@Controller
@RequestMapping("/p/{jeton}")
public class WishlistPubliqueController {

    private final WishlistPubliqueService publique;

    public WishlistPubliqueController(WishlistPubliqueService publique) {
        this.publique = publique;
    }

    @GetMapping
    public String afficher(@PathVariable String jeton, @RequestParam(defaultValue = "0") int page,
                           Authentication authentication, Model model) {
        WishlistPubliqueService.Consultation c = publique.consulter(jeton, login(authentication), page);
        if (c.vue() == null) {
            return "redirect:/wishlists/" + c.wishlistId();
        }
        model.addAttribute("vue", c.vue());
        model.addAttribute("jeton", jeton);
        model.addAttribute("invite", c.profil() == WishlistPubliqueService.Profil.INVITE);
        return "wishlist-publique";
    }

    @PostMapping("/souhaits/{souhaitId}/pris")
    public String prendre(@PathVariable String jeton, @PathVariable Long souhaitId,
                          @RequestParam(required = false) String nom, @RequestParam(defaultValue = "0") int page,
                          Authentication authentication, RedirectAttributes flash) {
        try {
            publique.prendre(jeton, login(authentication), souhaitId, nom);
        } catch (RequeteInvalideException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return retour(jeton, page);
    }

    /** Sans confirmation, l'article d'un autre ou d'un invité n'est pas libéré : un avertissement est affiché. */
    @PostMapping("/souhaits/{souhaitId}/libere")
    public String liberer(@PathVariable String jeton, @PathVariable Long souhaitId,
                          @RequestParam(defaultValue = "false") boolean confirme,
                          @RequestParam(defaultValue = "0") int page, Authentication authentication, Model model) {
        SouhaitService.ResultatLiberation resultat =
                publique.liberer(jeton, login(authentication), souhaitId, confirme);
        if (resultat.libere()) {
            return retour(jeton, page);
        }
        model.addAttribute("jeton", jeton);
        model.addAttribute("souhaitId", souhaitId);
        model.addAttribute("preneur", resultat.preneur());
        model.addAttribute("page", Math.max(page, 0));
        return "public-confirmation";
    }

    private static String login(Authentication authentication) {
        boolean connecte = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        return connecte ? authentication.getName() : null;
    }

    private static String retour(String jeton, int page) {
        return "redirect:/p/" + jeton + (page > 0 ? "?page=" + page : "");
    }
}
