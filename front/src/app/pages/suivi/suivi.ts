import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { messageErreur } from '../../core/erreurs';
import { DatePipe, EurosPipe } from '../../core/formats';
import { Suivi } from '../../core/models';

const ETAPES = ['Reçu', 'Diagnostic', 'Réparation', 'Prêt', 'Récupéré'];

/**
 * Page publique de suivi pour les clients : ouverte par le QR code du bon de dépôt
 * (/suivi/REF?code=XXX) ou en saisissant sa référence et son code ou son téléphone.
 */
@Component({
  selector: 'app-suivi',
  imports: [FormsModule, DatePipe, EurosPipe],
  templateUrl: './suivi.html',
  styleUrl: './suivi.css',
})
export class SuiviPage {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  readonly reference = input<string>();
  readonly code = input<string>();

  protected ref = '';
  protected cle = '';
  protected readonly suivi = signal<Suivi | null>(null);
  protected readonly erreur = signal<string | null>(null);
  protected readonly chargement = signal(false);
  protected readonly confirmation = signal<'accepter' | 'refuser' | null>(null);
  protected readonly etapes = ETAPES;

  /** Code de suivi utilisé (seul le code permet de répondre au devis). */
  private codeUtilise: string | null = null;

  protected readonly etape = computed(() => {
    switch (this.suivi()?.statut) {
      case 'RECU':
        return 0;
      case 'DIAGNOSTIC':
      case 'ATTENTE_CLIENT':
        return 1;
      case 'ATTENTE_PIECE':
      case 'REPARATION':
        return 2;
      case 'PRET':
        return 3;
      case 'CLOTURE':
        return 4;
      default:
        return -1;
    }
  });

  constructor() {
    effect(() => {
      const reference = this.reference();
      const code = this.code();
      untracked(() => {
        this.ref = reference ?? '';
        this.cle = code ?? '';
        if (reference && code && this.suivi()?.reference !== reference) {
          this.consulter();
        }
      });
    });
  }

  protected consulter(): void {
    const reference = this.ref.trim().toUpperCase();
    const saisie = this.cle.trim();
    if (!reference || !saisie) {
      this.erreur.set('Indiquez la référence du dossier et votre code de suivi ou votre téléphone.');
      return;
    }
    // Un code de suivi contient des lettres, un téléphone seulement des chiffres
    const acces = /[a-z]/i.test(saisie) ? { code: saisie.toUpperCase() } : { telephone: saisie };
    this.chargement.set(true);
    this.erreur.set(null);
    this.api.suivi(reference, acces).subscribe({
      next: (s) => {
        this.chargement.set(false);
        this.suivi.set(s);
        this.codeUtilise = acces.code ?? null;
        if (this.reference() !== s.reference) {
          this.router.navigate(['/suivi', s.reference], { queryParams: acces.code ? { code: acces.code } : {}, replaceUrl: true });
        }
      },
      error: (err) => {
        this.chargement.set(false);
        this.erreur.set(messageErreur(err));
      },
    });
  }

  protected repondre(accepte: boolean): void {
    const s = this.suivi()!;
    this.chargement.set(true);
    this.api.repondreDevis(s.reference, this.codeUtilise!, accepte).subscribe({
      next: (r) => {
        this.chargement.set(false);
        this.confirmation.set(null);
        this.suivi.set(r);
      },
      error: (err) => {
        this.chargement.set(false);
        this.erreur.set(messageErreur(err));
      },
    });
  }

  protected autre(): void {
    this.suivi.set(null);
    this.ref = '';
    this.cle = '';
    this.router.navigate(['/suivi']);
  }
}
