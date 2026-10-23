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
 * Chiffre d'affaires (admin) : pour chaque réparation rendue sur la période,
 * ce qui a été encaissé, ce que les pièces ont coûté et ce qu'il reste.
 */
@Component({
  selector: 'app-chiffre-affaires',
  imports: [FormsModule, RouterLink, DatePipe, EurosPipe],
  template: `
    <div class="page">
      <div class="entete-page">
        <div>
          <h1>Chiffre d'affaires</h1>
          <p class="doux" style="margin: 0">Réparations rendues au client sur la période choisie.</p>
        </div>
        <button class="btn btn-principal" [disabled]="!ca()?.reparations || export()" (click)="exporter()">
          ⤓ Exporter pour Excel (CSV)
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
        <div class="totaux">
          <div class="carte">
            <span class="doux">Réparations rendues</span>
            <strong>{{ c.reparations }}</strong>
          </div>
          <div class="carte">
            <span class="doux">Encaissé</span>
            <strong>{{ c.encaisse | euros }}</strong>
          </div>
          <div class="carte">
            <span class="doux">Pièces achetées</span>
            <strong class="moins">− {{ c.coutPieces | euros }}</strong>
          </div>
          <div class="carte marge">
            <span class="doux">Ce qu'il reste (marge)</span>
            <strong [class.negatif]="c.marge < 0">{{ c.marge | euros }}</strong>
          </div>
        </div>

        <section class="carte">
          <h2>Détail par réparation</h2>
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
                        <div class="doux petit">{{ l.appareil }} · {{ l.reference }}</div>
                      </td>
                      <td class="masquer-mobile">{{ l.technicien ?? '—' }}</td>
                      <td class="droite">{{ l.encaisse | euros }}</td>
                      <td class="droite moins">{{ l.coutPieces ? '− ' : '' }}{{ l.coutPieces | euros }}</td>
                      <td class="droite gras" [class.negatif]="l.marge < 0">{{ l.marge | euros }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
            <p class="doux petit aide">
              Le prix des pièces vient de l'onglet « Pièces » de chaque réparation (les pièces annulées ne comptent pas).
              Ce qui est encaissé comprend les acomptes.
            </p>
          } @else {
            <p class="vide">Aucune réparation rendue sur cette période.</p>
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
    .totaux {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 0 1rem;
    }
    .totaux .carte {
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
    }
    .totaux strong {
      font-size: 1.6rem;
    }
    .marge {
      border-left: 5px solid var(--ok);
    }
    .marge strong {
      color: var(--ok);
    }
    .moins {
      color: var(--texte-doux);
    }
    .negatif,
    .marge strong.negatif {
      color: var(--danger);
    }
    .defile {
      overflow-x: auto;
    }
    .aide {
      margin: 0.75rem 0 0;
    }
    @media (max-width: 900px) {
      .totaux {
        grid-template-columns: repeat(2, 1fr);
        gap: 0 0.75rem;
      }
      .totaux strong {
        font-size: 1.25rem;
      }
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
