package pcpro.atelier.core.enums;

public enum TypeActionEvenement {
  CREATION_DOSSIER,
  MODIF_DOSSIER,
  CHANGEMENT_STATUT,
  CHANGEMENT_TECHNICIEN,
  CHANGEMENT_PRIORITE,
  /** Conservé pour l'historique existant (le blocage a été retiré en V8). */
  CHANGEMENT_BLOCAGE,
  AJOUT_NOTE,
  AJOUT_PIECE,
  MODIF_PIECE,
  CHANGEMENT_STATUT_PIECE,
  AJOUT_PHOTO,
  SUPPRESSION_PHOTO,
  CLOTURE,
  REOUVERTURE,
  DEVIS,
  DECISION_DEVIS,
  PAIEMENT,
  ANNULATION_PAIEMENT,
  EMAIL_CLIENT,
  /** Conservé pour l'historique existant (remplacé par les types CHANGEMENT_*). */
  ACTION_SENSIBLE
}
