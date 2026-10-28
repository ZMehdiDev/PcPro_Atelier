import {
  EtatPiece,
  MoyenPaiement,
  Priorite,
  StatutDevis,
  StatutDossier,
  TypeAppareil,
  TypeLigneDevis,
} from './models';

export const STATUTS: StatutDossier[] = [
  'RECU',
  'DIAGNOSTIC',
  'ATTENTE_CLIENT',
  'ATTENTE_PIECE',
  'REPARATION',
  'PRET',
  'CLOTURE',
];

export const STATUT: Record<StatutDossier, string> = {
  RECU: 'Reçu',
  DIAGNOSTIC: 'Diagnostic',
  ATTENTE_CLIENT: 'Attente client',
  ATTENTE_PIECE: 'Attente pièce',
  REPARATION: 'En réparation',
  PRET: 'Prêt',
  CLOTURE: 'Rendu',
};

/** Texte du bouton qui fait passer le dossier à ce statut. */
export function actionStatut(cible: StatutDossier, actuel: StatutDossier): string {
  if (actuel === 'CLOTURE') {
    return 'Rouvrir le dossier';
  }
  switch (cible) {
    case 'DIAGNOSTIC':
      return actuel === 'RECU' ? 'Commencer le diagnostic' : 'Revenir au diagnostic';
    case 'ATTENTE_CLIENT':
      return 'Attendre le client';
    case 'ATTENTE_PIECE':
      return 'Attendre une pièce';
    case 'REPARATION':
      return actuel === 'PRET' ? 'Reprendre la réparation' : 'Passer en réparation';
    case 'PRET':
      return actuel === 'RECU' || actuel === 'DIAGNOSTIC' || actuel === 'ATTENTE_CLIENT'
        ? 'Prêt (sans réparation)'
        : 'C’est prêt';
    case 'CLOTURE':
      return 'Rendre au client';
    default:
      return STATUT[cible];
  }
}

/** Étape suivante « normale », mise en avant sur la fiche. */
export const ETAPE_SUIVANTE: Partial<Record<StatutDossier, StatutDossier>> = {
  RECU: 'DIAGNOSTIC',
  DIAGNOSTIC: 'REPARATION',
  ATTENTE_CLIENT: 'REPARATION',
  ATTENTE_PIECE: 'REPARATION',
  REPARATION: 'PRET',
  PRET: 'CLOTURE',
};

export const PRIORITE: Record<Priorite, string> = {
  BASSE: 'Basse',
  NORMALE: 'Normale',
  HAUTE: 'Urgente',
};

export const TYPE_APPAREIL: Record<TypeAppareil, string> = {
  PORTABLE: 'Portable',
  TOUR: 'Tour / fixe',
  TELEVISION: 'Télévision',
  CONSOLE: 'Console',
  AUTRE: 'Autre',
};

export const ETAT_PIECE: Record<EtatPiece, string> = {
  A_COMMANDER: 'À commander',
  COMMANDEE: 'Commandée',
  RECUE: 'Reçue',
  MONTEE: 'Montée',
  ANNULEE: 'Annulée',
};

export const ACTION_PIECE: Record<EtatPiece, string> = {
  A_COMMANDER: 'À commander',
  COMMANDEE: 'Commandée',
  RECUE: 'Reçue',
  MONTEE: 'Montée',
  ANNULEE: 'Annuler',
};

export const MOYEN_PAIEMENT: Record<MoyenPaiement, string> = {
  ESPECES: 'Espèces',
  BANCONTACT: 'Bancontact',
  VIREMENT: 'Virement',
  AUTRE: 'Autre',
};

export const STATUT_DEVIS: Record<StatutDevis, string> = {
  EN_ATTENTE: 'En attente de réponse',
  ACCEPTE: 'Accepté',
  REFUSE: 'Refusé',
};

export const TYPE_LIGNE: Record<TypeLigneDevis, string> = {
  MAIN_OEUVRE: 'Main-d’œuvre',
  PIECE: 'Pièce',
  AUTRE: 'Autre',
};

export const EVENEMENT: Record<string, string | undefined> = {
  CREATION_DOSSIER: 'Dossier créé',
  MODIF_DOSSIER: 'Dossier modifié',
  CHANGEMENT_STATUT: 'Statut',
  CHANGEMENT_TECHNICIEN: 'Technicien',
  CHANGEMENT_PRIORITE: 'Priorité',
  CHANGEMENT_BLOCAGE: 'Blocage',
  AJOUT_NOTE: 'Note ajoutée',
  AJOUT_PIECE: 'Pièce ajoutée',
  MODIF_PIECE: 'Pièce modifiée',
  CHANGEMENT_STATUT_PIECE: 'Pièce',
  AJOUT_PHOTO: 'Photo ajoutée',
  SUPPRESSION_PHOTO: 'Photo supprimée',
  CLOTURE: 'Rendu au client',
  REOUVERTURE: 'Dossier rouvert',
  DEVIS: 'Devis',
  DECISION_DEVIS: 'Réponse au devis',
  PAIEMENT: 'Paiement',
  ANNULATION_PAIEMENT: 'Paiement annulé',
  EMAIL_CLIENT: 'Email au client',
  FACTURE: 'Facture',
  ACTION_SENSIBLE: 'Action',
};

/** Remplace les codes (« Statut: PRET ») par leurs libellés dans l'historique. */
export function traduireValeur(valeur: string | null): string | null {
  if (!valeur) {
    return valeur;
  }
  const libelles: Record<string, string> = { ...STATUT, ...ETAT_PIECE, ...PRIORITE, ...MOYEN_PAIEMENT };
  return valeur.replace(/\b[A-Z][A-Z_]+[A-Z]\b/g, (code) => libelles[code] ?? code);
}

export function appareil(d: { typeAppareil?: TypeAppareil; marque: string | null; modele: string | null }): string {
  const nom = [d.marque, d.modele].filter((s) => s && s.trim()).join(' ');
  return nom || (d.typeAppareil ? TYPE_APPAREIL[d.typeAppareil] : 'Appareil');
}

/** « Martin Dubois » → « MD », pour la pastille ronde des listes. */
export function initiales(nom: string | null | undefined): string {
  const mots = (nom ?? '').trim().split(/\s+/).filter((m) => m);
  return mots.slice(0, 2).map((m) => m.charAt(0)).join('') || '?';
}

const TEINTES = [214, 162, 24, 268, 338, 192, 42, 128];

/** Teinte stable tirée du nom : chaque client garde la même couleur de pastille partout. */
export function teinte(nom: string | null | undefined): number {
  let h = 0;
  for (const c of nom ?? '') {
    h = (h * 31 + c.charCodeAt(0)) >>> 0;
  }
  return TEINTES[h % TEINTES.length];
}
