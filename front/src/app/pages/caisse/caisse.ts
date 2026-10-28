import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { DatePipe, EurosPipe, aujourdhui } from '../../core/formats';
import { MOYEN_PAIEMENT } from '../../core/libelles';
import { Caisse, MoyenPaiement } from '../../core/models';
import { FactureVente } from '../vente/facture-vente';
import { ConfirmationService } from '../../core/confirmation.service';
import { NotificationService } from '../../core/notification.service';

function debutSemaine(): string {
  const jour = (new Date().getDay() + 6) % 7; // lundi = 0
  return aujourdhui(-jour);
}

function debutMois(): string {
  return aujourdhui().slice(0, 8) + '01';
}

interface Mouvement {
  cle: string;
  createdAt: string;
  titre: string;
  detail: string;
  dossierId: number | null;
  venteId: number | null;
  employe: string;
  moyen: MoyenPaiement;
  montant: number;
}

/** Caisse (admin) : réparations et ventes encaissées sur une journée ou une période, par moyen de paiement. */
@Component({
  selector: 'app-caisse',
  imports: [FormsModule, RouterLink, DatePipe, EurosPipe, FactureVente],
  template: `
    <div class="page">
      <div class="entete-page">
        <h1>Caisse</h1>
        <div class="ligne no-print">
          <button class="btn" (click)="imprimer()">Imprimer</button>
          <a routerLink="/vente" class="btn btn-principal">Nouvelle vente</a>
        </div>
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
        <div class="tuiles">
          <div class="tuile principale">
            <span class="libelle">Encaissé {{ c.du === c.au ? 'le ' + (c.du | date) : 'du ' + (c.du | date) + ' au ' + (c.au | date) }}</span>
            <span class="montant">{{ c.total | euros }}</span>
          </div>
          @for (m of moyens; track m) {
            <div class="tuile">
              <span class="libelle">{{ libelles[m] }}</span>
              <strong>{{ c.parMoyen[m] | euros }}</strong>
            </div>
          }
        </div>

        <section class="carte">
          <h2>{{ mouvements(c).length }} encaissement{{ mouvements(c).length > 1 ? 's' : '' }}</h2>
          @if (mouvements(c).length) {
            <div class="defile">
            <table class="tableau">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Pour</th>
                  <th class="masquer-mobile">Par</th>
                  <th>Moyen</th>
                  <th class="droite">Montant</th>
                  <th class="no-print"></th>
                </tr>
              </thead>
              <tbody>
                @for (m of mouvements(c); track m.cle) {
                  <tr>
                    <td>{{ m.createdAt | date: true }}</td>
                    <td>
                      @if (m.dossierId) {
                        <a [routerLink]="['/dossiers', m.dossierId]">{{ m.titre }}</a>
                        <div class="doux petit">Réparation {{ m.detail }}</div>
                      } @else {
                        <strong>{{ m.titre }}</strong>
                        <div class="doux petit">Vente au comptoir</div>
                      }
                    </td>
                    <td class="masquer-mobile">{{ m.employe }}</td>
                    <td>{{ libelles[m.moyen] }}</td>
                    <td class="droite gras">{{ m.montant | euros }}</td>
                    <td class="droite no-print">
                      @if (m.venteId) {
                        <app-facture-vente [venteId]="m.venteId" [discret]="true" />
                        <button class="btn btn-petit btn-lien" (click)="annulerVente(m.venteId)">Annuler</button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
            </div>
          } @else {
            <p class="vide">Aucun encaissement sur cette période.</p>
          }
        </section>
      }
    </div>
  `,
  styles: `
    .tuiles {
      display: grid;
      grid-template-columns: 1.6fr repeat(4, 1fr);
      gap: 0.75rem;
      margin-bottom: 1.25rem;
    }
    .tuile {
      display: flex;
      flex-direction: column;
      justify-content: flex-end;
      gap: 0.3rem;
      padding: 1.1rem 1.25rem;
      border: 1px solid var(--bordure);
      border-radius: 18px;
      background: var(--surface);
    }
    .tuile .libelle {
      font-size: 0.85rem;
      color: var(--texte-doux);
    }
    .tuile strong {
      font-size: 1.35rem;
      font-weight: 650;
      font-variant-numeric: tabular-nums;
    }
    .tuile.principale {
      border: 0;
      color: #fff;
      background: linear-gradient(135deg, #12346b 0%, #1d5fc9 100%);
    }
    .tuile.principale .libelle {
      color: rgba(255, 255, 255, 0.78);
    }
    @media (max-width: 1100px) {
      .tuiles {
        grid-template-columns: repeat(2, 1fr);
      }
      .tuile.principale {
        grid-column: 1 / -1;
      }
    }
    @media print {
      .tuile.principale {
        color: inherit;
        background: none;
        border: 1px solid #ddd;
      }
    }
    .champ {
      flex: 1 1 130px;
      max-width: 220px;
    }
    .defile {
      overflow-x: auto;
    }
  `,
})
export class CaissePage {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);
  private readonly confirmation = inject(ConfirmationService);

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

  /** Paiements des réparations et ventes au comptoir, dans l'ordre où ils ont été encaissés. */
  protected mouvements(c: Caisse): Mouvement[] {
    return [
      ...c.paiements.map((p) => ({
        cle: 'p' + p.id,
        createdAt: p.createdAt,
        titre: p.clientNom,
        detail: p.reference,
        dossierId: p.dossierId,
        venteId: null,
        employe: p.employe,
        moyen: p.moyen,
        montant: p.montant,
      })),
      ...(c.ventes ?? []).map((v) => ({
        cle: 'v' + v.id,
        createdAt: v.createdAt,
        titre: v.articles,
        detail: '',
        dossierId: null,
        venteId: v.id,
        employe: v.employe,
        moyen: v.moyen,
        montant: v.montant,
      })),
    ].sort((a, b) => a.createdAt.localeCompare(b.createdAt));
  }

  protected async annulerVente(id: number): Promise<void> {
    const ok = await this.confirmation.demander({
      titre: 'Annuler cette vente ?',
      message: 'Elle sera retirée de la caisse et du chiffre d’affaires.',
      bouton: 'Annuler la vente',
      danger: true,
    });
    if (!ok) {
      return;
    }
    this.api.annulerVente(id).subscribe({
      next: () => {
        this.notifications.ok('Vente annulée');
        this.charger();
      },
      error: (err) => this.notifications.erreur(err),
    });
  }

  protected imprimer(): void {
    window.print();
  }
}
