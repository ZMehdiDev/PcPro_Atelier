import { Injectable, signal } from '@angular/core';

export interface DemandeConfirmation {
  titre: string;
  message?: string;
  /** Texte du bouton qui confirme (« Supprimer », « Annuler la vente »...). */
  bouton: string;
  /** Bouton rouge pour ce qui ne se rattrape pas. */
  danger?: boolean;
}

/** Remplace confirm() du navigateur par une fenêtre aux couleurs de l'atelier, avec des boutons clairs. */
@Injectable({ providedIn: 'root' })
export class ConfirmationService {
  readonly demande = signal<DemandeConfirmation | null>(null);
  private reponse: ((ok: boolean) => void) | null = null;

  demander(demande: DemandeConfirmation): Promise<boolean> {
    this.reponse?.(false);
    this.demande.set(demande);
    return new Promise((ok) => (this.reponse = ok));
  }

  repondre(ok: boolean): void {
    this.reponse?.(ok);
    this.reponse = null;
    this.demande.set(null);
  }
}
