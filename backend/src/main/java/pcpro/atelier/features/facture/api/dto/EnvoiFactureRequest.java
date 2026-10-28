package pcpro.atelier.features.facture.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Destinataire de l'email (vide : l'email noté sur la facture). */
public record EnvoiFactureRequest(
    @Email(message = "Adresse email invalide")
    @Size(max = 150, message = "Email trop long")
    String email
) {
}
