// Types renvoyés par l'API (voir les DTO du backend). Les montants arrivent en nombres JSON.

export type StatutDossier =
  | 'RECU'
  | 'DIAGNOSTIC'
  | 'ATTENTE_CLIENT'
  | 'ATTENTE_PIECE'
  | 'REPARATION'
  | 'PRET'
  | 'CLOTURE';
export type Priorite = 'BASSE' | 'NORMALE' | 'HAUTE';
export type TypeAppareil = 'PORTABLE' | 'TOUR' | 'TELEVISION' | 'CONSOLE' | 'AUTRE';
export type EtatPiece = 'A_COMMANDER' | 'COMMANDEE' | 'RECUE' | 'MONTEE' | 'ANNULEE';
export type MoyenPaiement = 'ESPECES' | 'BANCONTACT' | 'VIREMENT' | 'AUTRE';
export type StatutDevis = 'EN_ATTENTE' | 'ACCEPTE' | 'REFUSE';
export type TypeLigneDevis = 'MAIN_OEUVRE' | 'PIECE' | 'AUTRE';
export type Role = 'ADMIN' | 'TECHNICIEN';

export interface Page<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface Employe {
  id: number;
  prenom: string;
  login: string;
  role: Role;
  actif: boolean;
  motDePasseDefini: boolean;
  createdAt: string;
}

export interface LoginResponse {
  token: string;
  expireLe: string;
  employe: Employe;
}

export interface Technicien {
  id: number;
  prenom: string;
}

export interface Client {
  id: number;
  nom: string;
  telephone: string;
  email: string | null;
  createdAt: string;
}

export interface ClientRequest {
  nom: string;
  telephone: string;
  email: string | null;
}

export interface ClientDetail {
  client: Client;
  dossiers: DossierResume[];
}

export interface Appareil {
  type: TypeAppareil;
  marque: string | null;
  modele: string | null;
  numeroSerie: string | null;
}

export interface DossierResume {
  id: number;
  reference: string;
  clientId: number;
  clientNom: string;
  clientTelephone: string;
  typeAppareil: TypeAppareil;
  marque: string | null;
  modele: string | null;
  statut: StatutDossier;
  priorite: Priorite;
  technicien: Technicien | null;
  dateEntree: string;
  dateEstimee: string | null;
  enRetard: boolean;
  pretDepuis: string | null;
  nonRecupere: boolean;
  finGarantie: string | null;
  createdAt: string;
  closedAt: string | null;
}

export interface Garantie {
  dossierId: number;
  reference: string;
  marque: string | null;
  modele: string | null;
  numeroSerie: string | null;
  finGarantie: string;
}

export interface LigneDevis {
  type: TypeLigneDevis;
  libelle: string;
  quantite: number;
  prixUnitaire: number;
  total?: number;
}

export interface Devis {
  statut: StatutDevis;
  lignes: LigneDevis[];
  total: number;
  commentaire: string | null;
  updatedAt: string;
  decideAt: string | null;
}

export interface Paiement {
  id: number;
  montant: number;
  moyen: MoyenPaiement;
  employe: string;
  createdAt: string;
}

export interface Piece {
  id: number;
  reference: string;
  fournisseur: string | null;
  cout: number | null;
  etat: EtatPiece;
  etatsPossibles: EtatPiece[];
  createdAt: string;
}

export interface Note {
  id: number;
  contenu: string;
  sensible: boolean;
  auteur: string | null;
  createdAt: string;
}

export interface Photo {
  id: number;
  url: string;
  mimeType: string;
  tailleBytes: number;
  description: string | null;
  createdAt: string;
}

export interface Evenement {
  id: number;
  employePrenom: string | null;
  typeAction: string;
  ancienneValeur: string | null;
  nouvelleValeur: string | null;
  commentaire: string | null;
  createdAt: string;
}

export interface DossierDetail {
  id: number;
  reference: string;
  client: Client;
  appareil: Appareil;
  descriptionProbleme: string | null;
  accessoires: string | null;
  statut: StatutDossier;
  statutsPossibles: StatutDossier[];
  technicien: Technicien | null;
  priorite: Priorite;
  dateEntree: string;
  dateEstimee: string | null;
  enRetard: boolean;
  pretDepuis: string | null;
  nonRecupere: boolean;
  createdAt: string;
  closedAt: string | null;
  codeSuivi: string;
  lienSuivi: string | null;
  finGarantie: string | null;
  garanties: Garantie[];
  devis: Devis | null;
  paiements: Paiement[];
  totalPaye: number;
  resteAPayer: number | null;
  coutPieces: number;
  pieces: Piece[];
  notes: Note[];
  photos: Photo[];
  evenements: Evenement[];
}

export interface CreateDossierRequest {
  clientId: number | null;
  client: ClientRequest | null;
  appareil: Appareil;
  priorite: Priorite;
  descriptionProbleme: string | null;
  accessoires: string | null;
  dateEstimee: string | null;
  technicienAssigneId: number | null;
}

export interface UpdateDossierRequest {
  appareil: Appareil;
  descriptionProbleme: string | null;
  accessoires: string | null;
  dateEstimee: string | null;
}

export interface PaiementRequest {
  montant: number;
  moyen: MoyenPaiement;
}

export interface ChangeStatutRequest {
  statut: StatutDossier;
  commentaire?: string | null;
  notifierClient?: boolean;
  garantieMois?: number;
  paiement?: PaiementRequest | null;
}

export interface Stats {
  parStatut: Record<StatutDossier, number>;
  actifs: number;
  enRetard: number;
  nonRecuperes: number;
  mesDossiers: number;
}

/** Ce que les réparations rendues sur une période ont rapporté (admin). */
export interface ChiffreAffaires {
  du: string;
  au: string;
  reparations: number;
  encaisse: number;
  coutPieces: number;
  marge: number;
  lignes: {
    dossierId: number;
    reference: string;
    renduLe: string;
    clientNom: string;
    appareil: string;
    technicien: string | null;
    encaisse: number;
    coutPieces: number;
    marge: number;
  }[];
  ventes: {
    nombre: number;
    encaisse: number;
    cout: number;
    marge: number;
    lignes: {
      venteId: number;
      venduLe: string;
      article: string;
      quantite: number;
      employe: string;
      encaisse: number;
      cout: number;
      marge: number;
    }[];
  };
  /** Réparations et ventes ensemble. */
  total: { encaisse: number; cout: number; marge: number };
}

export interface Caisse {
  du: string;
  au: string;
  total: number;
  parMoyen: Record<MoyenPaiement, number>;
  paiements: {
    id: number;
    dossierId: number;
    reference: string;
    clientNom: string;
    montant: number;
    moyen: MoyenPaiement;
    employe: string;
    createdAt: string;
  }[];
  /** Ventes au comptoir, comptées dans le total et par moyen. */
  ventes: {
    id: number;
    articles: string;
    montant: number;
    moyen: MoyenPaiement;
    employe: string;
    createdAt: string;
  }[];
}

export interface VenteRequest {
  moyen: MoyenPaiement;
  lignes: { article: string; quantite: number; prixUnitaire: number; coutUnitaire: number | null }[];
}

export interface Vente {
  id: number;
  moyen: MoyenPaiement;
  total: number;
  employe: string;
  createdAt: string;
}

export interface Suivi {
  reference: string;
  appareil: string;
  statut: StatutDossier;
  statutLibelle: string;
  dateEntree: string;
  dateEstimee: string | null;
  pretDepuis: string | null;
  finGarantie: string | null;
  devis: Devis | null;
  peutRepondreDevis: boolean;
  resteAPayer: number | null;
  atelier: { nom: string; adresse: string; telephone: string; horaires: string };
}

export interface DossierFiltres {
  q?: string;
  statut?: StatutDossier;
  actifs?: boolean;
  priorite?: Priorite;
  technicienId?: number;
  mesDossiers?: boolean;
  enRetard?: boolean;
  nonRecuperes?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

export type TypeFacture = 'FACTURE' | 'NOTE_CREDIT';

/** Facture ou note de crédit (total TVAC, négatif pour une note de crédit). */
export interface Facture {
  id: number;
  type: TypeFacture;
  numero: string;
  dateEmission: string;
  dossierId: number | null;
  dossierReference: string | null;
  venteId: number | null;
  clientNom: string;
  clientAdresse: string | null;
  clientTva: string | null;
  clientEmail: string | null;
  total: number;
  annuleePar: string | null;
  factureOrigine: string | null;
  createdAt: string;
}

/** Coordonnées du client à imprimer sur la facture (vides : celles de la fiche client). */
export interface FactureRequest {
  clientNom?: string | null;
  clientAdresse?: string | null;
  clientTva?: string | null;
  clientEmail?: string | null;
}
