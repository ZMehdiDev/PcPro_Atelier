package pcpro.atelier.features.facture.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Coordonnées du client imprimées sur la facture. Pour une réparation, le nom et l'email viennent de la fiche
 * client s'ils sont laissés vides ; pour une vente au comptoir, le nom est obligatoire.
 *
 * @param clientAdresse adresse postale (obligatoire sur une facture : demandez-la au client)
 * @param clientTva     numéro de TVA d'un client professionnel
 */
public record FactureRequest(
    @Size(max = 150, message = "Nom trop long (150 caractères maximum)")
    String clientNom,

    @Size(max = 300, message = "Adresse trop longue (300 caractères maximum)")
    String clientAdresse,

    @Size(max = 30, message = "Numéro de TVA trop long")
    String clientTva,

    @Email(message = "Adresse email invalide")
    @Size(max = 150, message = "Email trop long")
    String clientEmail
) {

  public static final FactureRequest VIDE = new FactureRequest(null, null, null, null);
}
