package pcpro.atelier.features.devis.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record DevisRequest(
    @NotEmpty(message = "Le devis doit contenir au moins une ligne")
    @Size(max = 50, message = "50 lignes maximum")
    List<@Valid LigneDevisRequest> lignes,

    @Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères")
    String commentaire
) {}
