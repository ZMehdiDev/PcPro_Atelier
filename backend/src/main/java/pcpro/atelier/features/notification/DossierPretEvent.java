package pcpro.atelier.features.notification;

/** Publié quand un dossier passe au statut PRET et que le client doit être prévenu. */
public record DossierPretEvent(Long dossierId, Long employeId) {
}
