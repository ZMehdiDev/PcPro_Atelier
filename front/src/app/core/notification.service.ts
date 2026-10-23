import { Injectable, signal } from '@angular/core';
import { messageErreur } from './erreurs';

export interface Toast {
  id: number;
  texte: string;
  erreur: boolean;
}

/** Petits messages en bas de l'écran (« Dossier créé », erreurs de l'API...). */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private compteur = 0;
  readonly toasts = signal<Toast[]>([]);

  ok(texte: string): void {
    this.ajouter(texte, false);
  }

  erreur(err: unknown): void {
    this.ajouter(typeof err === 'string' ? err : messageErreur(err), true);
  }

  fermer(id: number): void {
    this.toasts.update((t) => t.filter((x) => x.id !== id));
  }

  private ajouter(texte: string, erreur: boolean): void {
    const id = ++this.compteur;
    this.toasts.update((t) => [...t, { id, texte, erreur }]);
    setTimeout(() => this.fermer(id), erreur ? 7000 : 3500);
  }
}
