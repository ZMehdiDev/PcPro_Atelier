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

  /** Horaires en tableau : lignes séparées par « · », jours et heures par « : » (comme sur le bon de dépôt). */
  protected readonly horaires = computed(() =>
    (this.suivi()?.atelier.horaires ?? '')
      .split('·')
      .map((ligne) => ligne.trim())
      .filter((ligne) => ligne)
      .map((ligne) => {
        const i = ligne.indexOf(':');
        return i < 0 ? { jours: ligne, heures: '' } : { jours: ligne.slice(0, i).trim(), heures: ligne.slice(i + 1).trim() };
      }),
  );

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

  /** Une phrase simple pour le client, selon l'avancement. */
  protected readonly explication = computed(() => {
    switch (this.suivi()?.statut) {
      case 'RECU':
        return 'Un technicien va bientôt l’examiner.';
      case 'DIAGNOSTIC':
        return 'Un technicien cherche la panne. Si une réparation payante est nécessaire, vous recevrez un devis.';
      case 'ATTENTE_CLIENT':
        return 'L’atelier a besoin de votre accord pour continuer : répondez au devis ci-dessous ou appelez-nous.';
      case 'ATTENTE_PIECE':
        return 'La réparation reprend dès que la pièce arrive.';
      case 'REPARATION':
        return 'Nous vous prévenons dès que c’est prêt.';
      case 'PRET':
        return 'Pensez à prendre votre bon de dépôt. Les horaires sont en bas de la page.';
      case 'CLOTURE':
        return 'Merci de votre confiance.';
      default:
        return '';
    }
  });

  protected itineraire(adresse: string): string {
    return 'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(adresse);
  }

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
