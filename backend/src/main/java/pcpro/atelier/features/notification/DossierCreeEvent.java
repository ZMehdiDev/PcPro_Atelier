package pcpro.atelier.features.notification;

/** Publié à la création d'un dossier : le client reçoit son bon de dépôt par email. */
public record DossierCreeEvent(Long dossierId, Long employeId) {
}
