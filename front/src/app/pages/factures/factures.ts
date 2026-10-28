import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { ouvrirPdf } from '../../core/fichiers';
import { DatePipe, EurosPipe, aujourdhui } from '../../core/formats';
import { Facture } from '../../core/models';
import { NotificationService } from '../../core/notification.service';

function date(annee: number, mois: number, jour: number): string {
  const d = new Date(annee, mois, jour);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

const maintenant = () => new Date();

/**
 * Factures (admin) : celles émises sur la période, à rouvrir ou annuler par une note de crédit,
 * et le journal des recettes en PDF pour le comptable.
 */
@Component({
  selector: 'app-factures',
  imports: [FormsModule, RouterLink, DatePipe, EurosPipe],
  template: `
    <div class="page">
      <div class="entete-page">
        <h1>Factures</h1>
        <button class="btn btn-noir" [disabled]="!du || !au" (click)="journal()">Journal des recettes (PDF)</button>
      </div>

      <section class="carte periode">
        <div class="ligne">
          @for (p of periodes; track p.libelle) {
            <button class="btn btn-petit" [class.btn-principal]="du === p.du() && au === p.au()" (click)="choisir(p.du(), p.au())">
              {{ p.libelle }}
            </button>
          }
        </div>
        <div class="ligne">
          <label class="champ"><span>Du</span><input type="date" [(ngModel)]="du" (change)="charger()" /></label>
          <label class="champ"><span>Au</span><input type="date" [(ngModel)]="au" (change)="charger()" /></label>
        </div>
      </section>

      <section class="carte solde">
        <div>
          <span class="doux">Facturé sur la période</span>
          <div class="montant">{{ total() | euros }}</div>
        </div>
        <p class="doux petit">Notes de crédit déduites · {{ factures().length }} document{{ factures().length > 1 ? 's' : '' }}</p>
      </section>

      <section class="carte">
        @if (factures().length) {
          <div class="defile">
            <table class="tableau">
              <thead>
                <tr>
                  <th>Numéro</th>
                  <th>Date</th>
                  <th>Client</th>
                  <th class="masquer-mobile">Pour</th>
                  <th class="droite">Montant</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (f of factures(); track f.id) {
                  <tr [class.annulee]="f.annuleePar">
                    <td>
                      <strong>{{ f.numero }}</strong>
                      @if (f.type === 'NOTE_CREDIT') {
                        <div class="doux petit">Note de crédit · annule {{ f.factureOrigine }}</div>
                      } @else if (f.annuleePar) {
                        <div class="doux petit">Annulée par {{ f.annuleePar }}</div>
                      }
                    </td>
                    <td>{{ f.dateEmission | date }}</td>
                    <td>{{ f.clientNom }}</td>
                    <td class="masquer-mobile">
                      @if (f.dossierId) {
                        <a [routerLink]="['/dossiers', f.dossierId]">Réparation {{ f.dossierReference }}</a>
                      } @else {
                        Vente au comptoir
                      }
                    </td>
                    <td class="droite gras">{{ f.total | euros }}</td>
                    <td class="droite actions">
                      <button class="btn btn-petit" (click)="ouvrir(f)">PDF</button>
                      @if (f.type === 'FACTURE' && !f.annuleePar) {
                        <button class="btn btn-petit btn-lien" [disabled]="envoi()" (click)="annuler(f)">Annuler</button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        } @else {
          <p class="vide">Aucune facture sur cette période. Les factures se font depuis un dossier (onglet Devis et paiement) ou après une vente.</p>
        }
      </section>
    </div>
  `,
  styles: `
    .periode {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      align-items: flex-end;
      gap: 0.75rem 1.5rem;
    }
    .periode .champ {
      flex: 1 1 150px;
    }
    .solde {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 1rem 2rem;
      padding: 1.75rem 2rem;
    }
    .defile {
      overflow-x: auto;
    }
    .actions {
      white-space: nowrap;
    }
    tr.annulee td {
      color: var(--texte-doux);
    }
  `,
})
export class FacturesPage {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);

  protected readonly periodes = [
    { libelle: 'Ce mois-ci', du: () => date(maintenant().getFullYear(), maintenant().getMonth(), 1), au: () => aujourdhui() },
    {
      libelle: 'Le mois dernier',
      du: () => date(maintenant().getFullYear(), maintenant().getMonth() - 1, 1),
      au: () => date(maintenant().getFullYear(), maintenant().getMonth(), 0),
    },
    {
      libelle: 'Ce trimestre',
      du: () => date(maintenant().getFullYear(), Math.floor(maintenant().getMonth() / 3) * 3, 1),
      au: () => aujourdhui(),
    },
    { libelle: 'Cette année', du: () => date(maintenant().getFullYear(), 0, 1), au: () => aujourdhui() },
  ];
  protected du = this.periodes[0].du();
  protected au = this.periodes[0].au();
  protected readonly factures = signal<Facture[]>([]);
  protected readonly envoi = signal(false);
  protected readonly total = computed(() => this.factures().reduce((t, f) => t + f.total, 0));

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
    this.api.factures(this.du, this.au).subscribe({
      next: (f) => this.factures.set(f),
      error: (err) => this.notifications.erreur(err),
    });
  }

  protected ouvrir(f: Facture): void {
    const nom = (f.type === 'NOTE_CREDIT' ? 'note-credit-' : 'facture-') + f.numero + '.pdf';
    ouvrirPdf(this.api.facturePdf(f.id), nom, (err) => this.notifications.erreur(err));
  }

  protected journal(): void {
    ouvrirPdf(this.api.journalRecettes(this.du, this.au), `journal-recettes-${this.du}-${this.au}.pdf`, (err) =>
      this.notifications.erreur(err),
    );
  }

  protected annuler(f: Facture): void {
    if (!confirm(`Annuler la facture ${f.numero} ? Une note de crédit du même montant sera émise.`)) {
      return;
    }
    this.envoi.set(true);
    this.api.annulerFacture(f.id).subscribe({
      next: (note) => {
        this.envoi.set(false);
        this.notifications.ok(`Note de crédit ${note.numero} émise`);
        this.charger();
      },
      error: (err) => {
        this.envoi.set(false);
        this.notifications.erreur(err);
      },
    });
  }
}
