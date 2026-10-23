import { Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { DatePipe, EurosPipe } from '../../core/formats';
import { MOYEN_PAIEMENT } from '../../core/libelles';
import { DossierDetail, MoyenPaiement, Paiement } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { Dialogue } from '../../shared/dialogue';

/** Paiements du dossier : acomptes, solde. L'annulation est réservée aux admins. */
@Component({
  selector: 'app-paiements-carte',
  imports: [FormsModule, EurosPipe, DatePipe, Dialogue],
  template: `
    <section class="carte">
      <h2>
        Paiement
        @if (dossier().resteAPayer !== null) {
          @if (dossier().resteAPayer! > 0) {
            <span class="badge badge-alerte">Reste {{ dossier().resteAPayer | euros }}</span>
          } @else {
            <span class="badge badge-ok">Payé</span>
          }
        }
      </h2>
      @if (dossier().paiements.length) {
        <ul class="liste">
          @for (p of dossier().paiements; track p.id) {
            <li class="ligne ligne-espace">
              <span>
                <strong>{{ p.montant | euros }}</strong> · {{ libelles[p.moyen] }}
                <span class="doux petit"><br />{{ p.createdAt | date: true }} par {{ p.employe }}</span>
              </span>
              @if (auth.estAdmin()) {
                <button class="btn btn-lien btn-petit" (click)="annuler(p)">Annuler</button>
              }
            </li>
          }
        </ul>
        <p class="gras">Total payé : {{ dossier().totalPaye | euros }}</p>
      } @else {
        <p class="doux">Rien encaissé pour l'instant. Le paiement se fait en rendant l'appareil.</p>
      }
      @if (dossier().statut !== 'CLOTURE') {
        <button class="btn btn-petit" (click)="ouvrir()">Encaisser un acompte</button>
      }
    </section>

    <app-dialogue titre="Encaisser un acompte" [ouvert]="dialogue()" (fermer)="dialogue.set(false)">
      <div class="pile">
        <label class="champ">
          <span>Montant</span>
          <input type="number" inputmode="decimal" min="0.01" step="0.01" [(ngModel)]="montant" />
        </label>
        <div class="choix">
          @for (m of moyens; track m) {
            <label>
              <input type="radio" name="moyenAcompte" [value]="m" [(ngModel)]="moyen" />
              {{ libelles[m] }}
            </label>
          }
        </div>
      </div>
      <div class="dialogue-actions">
        <button class="btn" (click)="dialogue.set(false)">Annuler</button>
        <button class="btn btn-principal" [disabled]="envoi() || !montant" (click)="encaisser()">Encaisser</button>
      </div>
    </app-dialogue>
  `,
})
export class PaiementsCarte {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);
  protected readonly auth = inject(AuthService);

  readonly dossier = input.required<DossierDetail>();
  readonly modifie = output<void>();

  protected readonly libelles = MOYEN_PAIEMENT;
  protected readonly moyens = Object.keys(MOYEN_PAIEMENT) as MoyenPaiement[];
  protected readonly dialogue = signal(false);
  protected readonly envoi = signal(false);
  protected montant: number | null = null;
  protected moyen: MoyenPaiement = 'BANCONTACT';

  protected ouvrir(): void {
    this.montant = null;
    this.moyen = 'BANCONTACT';
    this.dialogue.set(true);
  }

  protected encaisser(): void {
    this.envoi.set(true);
    this.api.encaisser(this.dossier().id, { montant: this.montant!, moyen: this.moyen }).subscribe({
      next: () => {
        this.envoi.set(false);
        this.dialogue.set(false);
        this.notifications.ok('Paiement enregistré');
        this.modifie.emit();
      },
      error: (err) => {
        this.envoi.set(false);
        this.notifications.erreur(err);
      },
    });
  }

  protected annuler(p: Paiement): void {
    if (!confirm(`Annuler le paiement de ${p.montant} € ?`)) {
      return;
    }
    this.api.annulerPaiement(this.dossier().id, p.id).subscribe({
      next: () => {
        this.notifications.ok('Paiement annulé');
        this.modifie.emit();
      },
      error: (err) => this.notifications.erreur(err),
    });
  }
}
