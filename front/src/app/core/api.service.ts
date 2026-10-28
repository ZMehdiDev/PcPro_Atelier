import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, shareReplay } from 'rxjs';
import {
  Caisse,
  ChiffreAffaires,
  Vente,
  VenteRequest,
  ChangeStatutRequest,
  Client,
  ClientDetail,
  ClientRequest,
  CreateDossierRequest,
  Devis,
  DossierDetail,
  DossierFiltres,
  DossierResume,
  Employe,
  EtatPiece,
  Facture,
  FactureRequest,
  Garantie,
  LigneDevis,
  Note,
  Page,
  Paiement,
  PaiementRequest,
  Photo,
  Piece,
  Priorite,
  Role,
  Stats,
  Suivi,
  UpdateDossierRequest,
} from './models';

/** Paramètres de requête sans les valeurs vides. */
function params(valeurs: object): HttpParams {
  let p = new HttpParams();
  for (const [cle, valeur] of Object.entries(valeurs)) {
    if (valeur !== undefined && valeur !== null && valeur !== '' && valeur !== false) {
      p = p.set(cle, String(valeur));
    }
  }
  return p;
}

export interface PieceRequest {
  reference: string;
  fournisseur: string | null;
  cout: number | null;
  etat?: EtatPiece;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private employesCache: Observable<Employe[]> | null = null;

  // Dossiers

  dossiers(filtres: DossierFiltres): Observable<Page<DossierResume>> {
    return this.http.get<Page<DossierResume>>('/api/dossiers', { params: params(filtres) });
  }

  stats(): Observable<Stats> {
    return this.http.get<Stats>('/api/dossiers/stats');
  }

  garanties(clientId: number | null, numeroSerie: string | null): Observable<Garantie[]> {
    return this.http.get<Garantie[]>('/api/dossiers/garanties', { params: params({ clientId, numeroSerie }) });
  }

  dossier(id: number): Observable<DossierDetail> {
    return this.http.get<DossierDetail>(`/api/dossiers/${id}`);
  }

  creerDossier(req: CreateDossierRequest): Observable<DossierResume> {
    return this.http.post<DossierResume>('/api/dossiers', req);
  }

  modifierDossier(id: number, req: UpdateDossierRequest): Observable<DossierResume> {
    return this.http.put<DossierResume>(`/api/dossiers/${id}`, req);
  }

  changerStatut(id: number, req: ChangeStatutRequest): Observable<DossierResume> {
    return this.http.patch<DossierResume>(`/api/dossiers/${id}/statut`, req);
  }

  changerTechnicien(id: number, technicienId: number | null): Observable<DossierResume> {
    return this.http.patch<DossierResume>(`/api/dossiers/${id}/technicien`, { technicienId });
  }

  changerPriorite(id: number, priorite: Priorite): Observable<DossierResume> {
    return this.http.patch<DossierResume>(`/api/dossiers/${id}/priorite`, { priorite });
  }

  relancer(id: number): Observable<void> {
    return this.http.post<void>(`/api/dossiers/${id}/relance`, {});
  }

  bonDepot(id: number): Observable<Blob> {
    return this.http.get(`/api/dossiers/${id}/bon-depot`, { responseType: 'blob' });
  }

  // Devis

  devisPdf(id: number): Observable<Blob> {
    return this.http.get(`/api/dossiers/${id}/devis/pdf`, { responseType: 'blob' });
  }

  enregistrerDevis(id: number, lignes: LigneDevis[], commentaire: string | null): Observable<Devis> {
    return this.http.put<Devis>(`/api/dossiers/${id}/devis`, { lignes, commentaire });
  }

  deciderDevis(id: number, accepte: boolean, commentaire: string | null): Observable<Devis> {
    return this.http.post<Devis>(`/api/dossiers/${id}/devis/decision`, { accepte, commentaire });
  }

  envoyerDevis(id: number): Observable<void> {
    return this.http.post<void>(`/api/dossiers/${id}/devis/envoi`, {});
  }

  supprimerDevis(id: number): Observable<void> {
    return this.http.delete<void>(`/api/dossiers/${id}/devis`);
  }

  // Paiements

  encaisser(id: number, req: PaiementRequest): Observable<Paiement> {
    return this.http.post<Paiement>(`/api/dossiers/${id}/paiements`, req);
  }

  annulerPaiement(id: number, paiementId: number): Observable<void> {
    return this.http.delete<void>(`/api/dossiers/${id}/paiements/${paiementId}`);
  }

  caisse(du: string, au: string): Observable<Caisse> {
    return this.http.get<Caisse>('/api/caisse', { params: params({ du, au }) });
  }

  enregistrerVente(req: VenteRequest): Observable<Vente> {
    return this.http.post<Vente>('/api/ventes', req);
  }

  annulerVente(id: number): Observable<void> {
    return this.http.delete<void>(`/api/ventes/${id}`);
  }

  // Factures

  /** Facture du dossier, ou null s'il n'en a pas encore (réponse 204). */
  factureDuDossier(id: number): Observable<Facture | null> {
    return this.http.get<Facture | null>(`/api/dossiers/${id}/facture`);
  }

  /** Émet la facture du dossier (ou renvoie celle qui existe). */
  facturerDossier(id: number, req: FactureRequest): Observable<Facture> {
    return this.http.post<Facture>(`/api/dossiers/${id}/facture`, req);
  }

  factureDeLaVente(id: number): Observable<Facture | null> {
    return this.http.get<Facture | null>(`/api/ventes/${id}/facture`);
  }

  facturerVente(id: number, req: FactureRequest): Observable<Facture> {
    return this.http.post<Facture>(`/api/ventes/${id}/facture`, req);
  }

  factures(du: string, au: string): Observable<Facture[]> {
    return this.http.get<Facture[]>('/api/factures', { params: params({ du, au }) });
  }

  facturePdf(id: number): Observable<Blob> {
    return this.http.get(`/api/factures/${id}/pdf`, { responseType: 'blob' });
  }

  envoyerFacture(id: number, email: string | null): Observable<void> {
    return this.http.post<void>(`/api/factures/${id}/envoi`, { email });
  }

  /** Annule la facture par une note de crédit (admin). */
  annulerFacture(id: number): Observable<Facture> {
    return this.http.post<Facture>(`/api/factures/${id}/annulation`, {});
  }

  journalRecettes(du: string, au: string): Observable<Blob> {
    return this.http.get('/api/factures/journal', { params: params({ du, au }), responseType: 'blob' });
  }

  chiffreAffaires(du: string, au: string): Observable<ChiffreAffaires> {
    return this.http.get<ChiffreAffaires>('/api/chiffre-affaires', { params: params({ du, au }) });
  }

  exportChiffreAffaires(du: string, au: string): Observable<Blob> {
    return this.http.get('/api/chiffre-affaires/export', { params: params({ du, au }), responseType: 'blob' });
  }

  // Pièces, notes, photos

  ajouterPiece(id: number, req: PieceRequest): Observable<Piece> {
    return this.http.post<Piece>(`/api/dossiers/${id}/pieces`, req);
  }

  modifierPiece(id: number, pieceId: number, req: PieceRequest): Observable<Piece> {
    return this.http.put<Piece>(`/api/dossiers/${id}/pieces/${pieceId}`, req);
  }

  changerEtatPiece(id: number, pieceId: number, nouvelEtat: EtatPiece): Observable<Piece> {
    return this.http.patch<Piece>(`/api/dossiers/${id}/pieces/${pieceId}/etat`, { nouvelEtat });
  }

  ajouterNote(id: number, contenu: string, sensible: boolean): Observable<Note> {
    return this.http.post<Note>(`/api/dossiers/${id}/notes`, { contenu, sensible });
  }

  ajouterPhoto(id: number, fichier: Blob, nom: string, description: string | null): Observable<Photo> {
    const form = new FormData();
    form.append('file', fichier, nom);
    if (description) {
      form.append('description', description);
    }
    return this.http.post<Photo>(`/api/dossiers/${id}/photos`, form);
  }

  contenuPhoto(id: number, photoId: number): Observable<Blob> {
    return this.http.get(`/api/dossiers/${id}/photos/${photoId}/contenu`, { responseType: 'blob' });
  }

  supprimerPhoto(id: number, photoId: number): Observable<void> {
    return this.http.delete<void>(`/api/dossiers/${id}/photos/${photoId}`);
  }

  // Clients

  clients(q: string, page = 0, size = 20): Observable<Page<Client>> {
    return this.http.get<Page<Client>>('/api/clients', { params: params({ q, page, size }) });
  }

  client(id: number): Observable<ClientDetail> {
    return this.http.get<ClientDetail>(`/api/clients/${id}`);
  }

  modifierClient(id: number, req: ClientRequest): Observable<Client> {
    return this.http.put<Client>(`/api/clients/${id}`, req);
  }

  // Employés

  /** Employés actifs (gardés en mémoire : ils changent rarement). */
  techniciens(): Observable<Employe[]> {
    if (!this.employesCache) {
      this.employesCache = this.http.get<Employe[]>('/api/employes').pipe(shareReplay(1));
    }
    return this.employesCache;
  }

  employes(): Observable<Employe[]> {
    return this.http.get<Employe[]>('/api/employes', { params: params({ inclureInactifs: true }) });
  }

  creerEmploye(req: { prenom: string; login: string; motDePasse: string; role: Role }): Observable<Employe> {
    return this.http.post<Employe>('/api/employes', req).pipe(map((e) => this.oublierEmployes(e)));
  }

  modifierEmploye(id: number, req: { prenom: string; role: Role; actif: boolean }): Observable<Employe> {
    return this.http.put<Employe>(`/api/employes/${id}`, req).pipe(map((e) => this.oublierEmployes(e)));
  }

  reinitialiserMotDePasse(id: number, motDePasse: string): Observable<void> {
    return this.http.post<void>(`/api/employes/${id}/mot-de-passe`, { motDePasse });
  }

  /** Téléphone perdu ou volé : l'employé est déconnecté de tous ses appareils. */
  deconnecterEmploye(id: number): Observable<void> {
    return this.http.post<void>(`/api/employes/${id}/deconnecter`, {});
  }

  // Suivi client (public)

  suivi(reference: string, acces: { code?: string; telephone?: string }): Observable<Suivi> {
    return this.http.get<Suivi>(`/api/public/suivi/${encodeURIComponent(reference)}`, {
      params: params(acces),
    });
  }

  repondreDevis(reference: string, code: string, accepte: boolean): Observable<Suivi> {
    return this.http.post<Suivi>(`/api/public/suivi/${encodeURIComponent(reference)}/devis`, { code, accepte });
  }

  private oublierEmployes<T>(valeur: T): T {
    this.employesCache = null;
    return valeur;
  }
}
