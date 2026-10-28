import { Injectable, signal } from '@angular/core';

export interface Recent {
  id: number;
  titre: string;
  detail: string;
}

const CLE = 'atelier.recents';
const MAX = 5;

function lire(): Recent[] {
  try {
    const r = JSON.parse(localStorage.getItem(CLE) ?? '[]');
    return Array.isArray(r) ? r.slice(0, MAX) : [];
  } catch {
    return [];
  }
}

/** Les derniers dossiers ouverts sur cet appareil, proposés en premier dans la recherche rapide. */
@Injectable({ providedIn: 'root' })
export class RecentsService {
  readonly dossiers = signal<Recent[]>(lire());

  ajouter(r: Recent): void {
    this.dossiers.update((l) => [r, ...l.filter((x) => x.id !== r.id)].slice(0, MAX));
    try {
      localStorage.setItem(CLE, JSON.stringify(this.dossiers()));
    } catch {
      // Navigation privée : pas de récents, sans importance.
    }
  }
}
