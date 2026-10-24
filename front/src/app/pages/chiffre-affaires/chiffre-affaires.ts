import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { telecharger } from '../../core/fichiers';
import { DatePipe, EurosPipe, aujourdhui } from '../../core/formats';
import { ChiffreAffaires } from '../../core/models';
import { NotificationService } from '../../core/notification.service';

function date(annee: number, mois: number, jour: number): string {
  const d = new Date(annee, mois, jour);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

const maintenant = () => new Date();

/**
 * Chiffre d'affaires (admin) : pour chaque réparation rendue et chaque article vendu sur la
 * période, ce qui a été encaissé, ce que les pièces ou l'article ont coûté et ce qu'il reste.
 */
@Component({
  selector: 'app-chiffre-affaires',
  imports: [FormsModule, RouterLink, DatePipe, EurosPipe],
  template: `
    <div class="page">
      <div class="entete-page">
        <h1>Chiffre d'affaires</h1>
        <button class="btn btn-noir" [disabled]="(!ca()?.reparations && !ca()?.ventes?.nombre) || export()" (click)="exporter()">
          Exporter pour Excel
        </button>
      </div>

      <section class="carte periode">
        <div class="ligne">
          @for (p of periodes; track p.libelle) {
            <button
              class="btn btn-petit"
              [class.btn-principal]="du === p.du() && au === p.au()"
              (click)="choisir(p.du(), p.au())"
            >
              {{ p.libelle }}
            </button>
          }
        </div>
        <div class="ligne">
          <label class="champ"><span>Du</span><input type="date" [(ngModel)]="du" (change)="charger()" /></label>
          <label class="champ"><span>Au</span><input type="date" [(ngModel)]="au" (change)="charger()" /></label>
        </div>
      </section>

      @if (ca(); as c) {
        <section class="carte solde">
          <div>
            <span class="doux">Encaissé sur la période</span>
            <div class="montant">{{ c.total.encaisse | euros }}</div>
          </div>
          <div class="chiffres">
            <div>
              <span class="doux petit">Réparations ({{ c.reparations }})</span>
              <strong>{{ c.encaisse | euros }}</strong>
            </div>
            <div>
              <span class="doux petit">Ventes ({{ c.ventes.nombre }})</span>
              <strong>{{ c.ventes.encaisse | euros }}</strong>
            </div>
            <div>
              <span class="doux petit">Pièces et achats</span>
              <strong class="moins">− {{ c.total.cout | euros }}</strong>
            </div>
            <div>
              <span class="doux petit">Marge</span>
              <strong class="marge" [class.negatif]="c.total.marge < 0">{{ c.total.marge | euros }}</strong>
            </div>
          </div>
        </section>

        <section class="carte">
          <h2>Réparations rendues</h2>
          @if (c.lignes.length) {
            <div class="defile">
              <table class="tableau">
                <thead>
                  <tr>
                    <th>Rendu le</th>
                    <th>Client et appareil</th>
                    <th class="masquer-mobile">Technicien</th>
                    <th class="droite">Encaissé</th>
                    <th class="droite">Pièces</th>
                    <th class="droite">Marge</th>
                  </tr>
                </thead>
                <tbody>
                  @for (l of c.lignes; track l.dossierId) {
                    <tr class="cliquable" (click)="router.navigate(['/dossiers', l.dossierId])">
                      <td>{{ l.renduLe | date }}</td>
                      <td>
                        <a [routerLink]="['/dossiers', l.dossierId]" (click)="$event.stopPropagation()">{{ l.clientNom }}</a>
                        <div class="doux petit">{{ l.appareil }}</div>
                      </td>
                      <td class="masquer-mobile">{{ l.technicien ?? '—' }}</td>
                      <td class="droite">{{ l.encaisse | euros }}</td>
                      <td class="droite moins">{{ l.coutPieces | euros }}</td>
                      <td class="droite gras" [class.negatif]="l.marge < 0">{{ l.marge | euros }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          } @else {
            <p class="vide">Aucune réparation rendue sur cette période.</p>
          }
        </section>

        <section class="carte">
          <h2>Ventes au comptoir</h2>
          @if (c.ventes.lignes.length) {
            <div class="defile">
              <table class="tableau">
                <thead>
                  <tr>
                    <th>Vendu le</th>
                    <th>Article</th>
                    <th class="masquer-mobile">Vendeur</th>
                    <th class="droite">Encaissé</th>
                    <th class="droite">Acheté à</th>
                    <th class="droite">Marge</th>
                  </tr>
                </thead>
                <tbody>
                  @for (l of c.ventes.lignes; track $index) {
                    <tr>
                      <td>{{ l.venduLe | date }}</td>
                      <td>{{ l.quantite > 1 ? l.quantite + ' × ' : '' }}{{ l.article }}</td>
                      <td class="masquer-mobile">{{ l.employe }}</td>
                      <td class="droite">{{ l.encaisse | euros }}</td>
                      <td class="droite moins">{{ l.cout | euros }}</td>
                      <td class="droite gras" [class.negatif]="l.marge < 0">{{ l.marge | euros }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          } @else {
            <p class="vide">Aucune vente sur cette période.</p>
          }
        </section>
      }
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
      gap: 1.25rem 2rem;
      padding: 1.75rem 2rem;
    }
    .chiffres {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem 2.25rem;
    }
    .chiffres > div {
      display: flex;
      flex-direction: column;
    }
    .chiffres strong {
      font-size: 1.25rem;
    }
    .marge {
      color: var(--ok);
    }
    .moins {
      color: var(--texte-doux);
    }
    .negatif,
    .marge.negatif {
      color: var(--danger);
    }
    .defile {
      overflow-x: auto;
    }
  `,
})
export class ChiffreAffairesPage {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);
  protected readonly router = inject(Router);

  protected readonly periodes = [
    { libelle: 'Ce mois-ci', du: () => date(maintenant().getFullYear(), maintenant().getMonth(), 1), au: () => aujourdhui() },
    {
      libelle: 'Le mois dernier',
      du: () => date(maintenant().getFullYear(), maintenant().getMonth() - 1, 1),
      au: () => date(maintenant().getFullYear(), maintenant().getMonth(), 0),
    },
    {
      libelle: '3 derniers mois',
      du: () => date(maintenant().getFullYear(), maintenant().getMonth() - 2, 1),
      au: () => aujourdhui(),
    },
    { libelle: 'Cette année', du: () => date(maintenant().getFullYear(), 0, 1), au: () => aujourdhui() },
    {
      libelle: 'L’an dernier',
      du: () => date(maintenant().getFullYear() - 1, 0, 1),
      au: () => date(maintenant().getFullYear() - 1, 11, 31),
    },
  ];
  protected du = this.periodes[0].du();
  protected au = this.periodes[0].au();
  protected readonly ca = signal<ChiffreAffaires | null>(null);
  protected readonly export = signal(false);

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
    this.api.chiffreAffaires(this.du, this.au).subscribe({
      next: (c) => this.ca.set(c),
      error: (err) => this.notifications.erreur(err),
    });
  }

  protected exporter(): void {
    this.export.set(true);
    telecharger(this.api.exportChiffreAffaires(this.du, this.au), `chiffre-affaires_${this.du}_${this.au}.csv`, (err) => {
      this.notifications.erreur(err);
    });
    setTimeout(() => this.export.set(false), 1500);
  }
}
