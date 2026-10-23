import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { DatePipe, EurosPipe, aujourdhui } from '../../core/formats';
import { MOYEN_PAIEMENT } from '../../core/libelles';
import { Caisse, MoyenPaiement } from '../../core/models';
import { NotificationService } from '../../core/notification.service';

function debutSemaine(): string {
  const jour = (new Date().getDay() + 6) % 7; // lundi = 0
  return aujourdhui(-jour);
}

function debutMois(): string {
  return aujourdhui().slice(0, 8) + '01';
}

/** Caisse (admin) : encaissements d'une journée ou d'une période, par moyen de paiement. */
@Component({
  selector: 'app-caisse',
  imports: [FormsModule, RouterLink, DatePipe, EurosPipe],
  template: `
    <div class="page">
      <div class="entete-page">
        <h1>Caisse</h1>
        <button class="btn no-print" (click)="imprimer()">Imprimer</button>
      </div>

      <div class="ligne no-print" style="margin-bottom: 0.75rem">
        @for (p of periodes; track p.libelle) {
          <button class="btn btn-petit" [class.btn-principal]="du === p.du() && au === p.au()" (click)="choisir(p.du(), p.au())">
            {{ p.libelle }}
          </button>
        }
      </div>
      <div class="ligne no-print" style="margin-bottom: 1rem">
        <label class="champ"><span>Du</span><input type="date" [(ngModel)]="du" (change)="charger()" /></label>
        <label class="champ"><span>Au</span><input type="date" [(ngModel)]="au" (change)="charger()" /></label>
      </div>

      @if (caisse(); as c) {
        <div class="totaux">
          <div class="carte total">
            <span class="doux">Total {{ c.du === c.au ? 'du ' + (c.du | date) : 'du ' + (c.du | date) + ' au ' + (c.au | date) }}</span>
            <strong>{{ c.total | euros }}</strong>
          </div>
          @for (m of moyens; track m) {
            <div class="carte">
              <span class="doux">{{ libelles[m] }}</span>
              <strong>{{ c.parMoyen[m] | euros }}</strong>
            </div>
          }
        </div>

        <section class="carte">
          <h2>{{ c.paiements.length }} paiement{{ c.paiements.length > 1 ? 's' : '' }}</h2>
          @if (c.paiements.length) {
            <table class="tableau">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Dossier</th>
                  <th class="masquer-mobile">Encaissé par</th>
                  <th>Moyen</th>
                  <th class="droite">Montant</th>
                </tr>
              </thead>
              <tbody>
                @for (p of c.paiements; track p.id) {
                  <tr>
                    <td>{{ p.createdAt | date: true }}</td>
                    <td>
                      <a [routerLink]="['/dossiers', p.dossierId]">{{ p.clientNom }}</a>
                      <div class="doux petit">{{ p.reference }}</div>
                    </td>
                    <td class="masquer-mobile">{{ p.employe }}</td>
                    <td>{{ libelles[p.moyen] }}</td>
                    <td class="droite gras">{{ p.montant | euros }}</td>
                  </tr>
                }
              </tbody>
            </table>
          } @else {
            <p class="vide">Aucun encaissement sur cette période.</p>
          }
        </section>
      }
    </div>
  `,
  styles: `
    .totaux {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 0 0.75rem;
    }
    .totaux .carte {
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
    }
    .totaux strong {
      font-size: 1.3rem;
    }
    .total {
      grid-column: 1 / -1;
      border-left: 4px solid var(--accent);
    }
    .total strong {
      font-size: 1.8rem;
    }
    .champ {
      flex: 1 1 140px;
    }
  `,
})
export class CaissePage {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);

  protected readonly libelles = MOYEN_PAIEMENT;
  protected readonly moyens = Object.keys(MOYEN_PAIEMENT) as MoyenPaiement[];
  protected readonly periodes = [
    { libelle: 'Aujourd’hui', du: () => aujourdhui(), au: () => aujourdhui() },
    { libelle: 'Hier', du: () => aujourdhui(-1), au: () => aujourdhui(-1) },
    { libelle: 'Cette semaine', du: debutSemaine, au: () => aujourdhui() },
    { libelle: 'Ce mois-ci', du: debutMois, au: () => aujourdhui() },
  ];
  protected du = aujourdhui();
  protected au = aujourdhui();
  protected readonly caisse = signal<Caisse | null>(null);

  constructor() {
    this.charger();
  }

  protected choisir(du: string, au: string): void {
    this.du = du;
    this.au = au;
    this.charger();
  }

  protected charger(): void {
    if (!this.du || !this.au) {
      return;
    }
    this.api.caisse(this.du, this.au).subscribe({
      next: (c) => this.caisse.set(c),
      error: (err) => this.notifications.erreur(err),
    });
  }

  protected imprimer(): void {
    window.print();
  }
}
