import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { telecharger } from '../../core/fichiers';
import { DatePipe, EurosPipe, aujourdhui, lireDate } from '../../core/formats';
import { ChiffreAffaires } from '../../core/models';
import { NotificationService } from '../../core/notification.service';

function date(annee: number, mois: number, jour: number): string {
  const d = new Date(annee, mois, jour);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

const maintenant = () => new Date();

const JOUR = new Intl.DateTimeFormat('fr-BE', { day: 'numeric', month: 'short' });
const JOUR_LONG = new Intl.DateTimeFormat('fr-BE', { weekday: 'long', day: 'numeric', month: 'long' });
const MOIS = new Intl.DateTimeFormat('fr-BE', { month: 'long', year: 'numeric' });
const MOIS_COURT = new Intl.DateTimeFormat('fr-BE', { month: 'short' });

interface Barre {
  cle: string;
  montant: number;
  libelle: string;
  court: string;
}

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
        <div class="tuiles">
          <div class="tuile principale">
            <span class="libelle">Encaissé sur la période</span>
            <span class="montant">{{ c.total.encaisse | euros }}</span>
          </div>
          <div class="tuile">
            <span class="libelle">Réparations ({{ c.reparations }})</span>
            <strong>{{ c.encaisse | euros }}</strong>
          </div>
          <div class="tuile">
            <span class="libelle">Ventes ({{ c.ventes.nombre }})</span>
            <strong>{{ c.ventes.encaisse | euros }}</strong>
          </div>
          <div class="tuile">
            <span class="libelle">Pièces et achats</span>
            <strong class="moins">− {{ c.total.cout | euros }}</strong>
          </div>
          <div class="tuile">
            <span class="libelle">Marge</span>
            <strong class="marge" [class.negatif]="c.total.marge < 0">{{ c.total.marge | euros }}</strong>
          </div>
        </div>

        <!-- Encaissé par jour, semaine ou mois selon la longueur de la période -->
        <section class="carte">
          <h2>
            Encaissé par {{ graphique().unite }}
            <span class="doux petit">Réparations et ventes</span>
          </h2>
          @if (graphique().max > 0) {
            <div class="graphique" role="img" [attr.aria-label]="'Encaissé par ' + graphique().unite">
              <div class="barres">
                @for (b of graphique().barres; track b.cle) {
                  <div class="colonne" tabindex="0" [attr.aria-label]="b.libelle + ' : ' + (b.montant | euros)">
                    <span class="bulle-info">
                      <strong>{{ b.montant | euros }}</strong>
                      <span>{{ b.libelle }}</span>
                    </span>
                    <span class="baton" [style.height.%]="(b.montant / graphique().max) * 100" [class.nul]="!b.montant"></span>
                  </div>
                }
              </div>
              <div class="axe">
                @for (b of graphique().barres; track b.cle; let i = $index) {
                  <span>{{ i % graphique().pas === 0 ? b.court : '' }}</span>
                }
              </div>
            </div>
          } @else {
            <p class="vide">Rien d'encaissé sur cette période.</p>
          }
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
      letter-spacing: -0.02em;
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
    .graphique {
      padding-top: 0.5rem;
    }
    .barres {
      display: flex;
      align-items: flex-end;
      gap: 2px;
      height: 200px;
      border-bottom: 1px solid var(--bordure-forte);
    }
    .colonne {
      position: relative;
      flex: 1;
      height: 100%;
      display: flex;
      align-items: flex-end;
      outline: none;
    }
    .baton {
      width: 100%;
      max-width: 44px;
      margin: 0 auto;
      min-height: 2px;
      border-radius: 4px 4px 0 0;
      background: var(--accent);
      transition: background 0.15s;
    }
    .baton.nul {
      background: var(--bordure);
    }
    .colonne:hover .baton,
    .colonne:focus-visible .baton {
      background: var(--accent-fonce);
    }
    .bulle-info {
      position: absolute;
      bottom: calc(100% + 6px);
      left: 50%;
      transform: translateX(-50%);
      display: none;
      flex-direction: column;
      align-items: center;
      padding: 0.4rem 0.65rem;
      border-radius: 10px;
      background: var(--noir);
      color: var(--sur-noir);
      font-size: 0.8rem;
      white-space: nowrap;
      z-index: 2;
      pointer-events: none;
    }
    .colonne {
      height: 100%;
    }
    .colonne:hover .bulle-info,
    .colonne:focus-visible .bulle-info {
      display: flex;
      bottom: auto;
      top: -8px;
      transform: translate(-50%, -100%);
    }
    .axe {
      display: flex;
      gap: 2px;
      margin-top: 0.4rem;
    }
    .axe span {
      flex: 1;
      text-align: center;
      font-size: 0.72rem;
      color: var(--texte-pale);
      white-space: nowrap;
      overflow: visible;
    }
    @media (max-width: 1100px) {
      .tuiles {
        grid-template-columns: repeat(2, 1fr);
      }
      .tuile.principale {
        grid-column: 1 / -1;
      }
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

  /** Barres du graphique : par jour jusqu'à 31 jours, par semaine jusqu'à 4 mois, sinon par mois. */
  protected readonly graphique = computed(() => {
    const c = this.ca();
    const vide = { unite: 'jour', barres: [] as Barre[], max: 0, pas: 1 };
    if (!c) {
      return vide;
    }
    const debut = lireDate(c.du);
    const fin = lireDate(c.au);
    const jours = Math.round((fin.getTime() - debut.getTime()) / 86_400_000) + 1;
    const unite: 'jour' | 'semaine' | 'mois' = jours <= 31 ? 'jour' : jours <= 124 ? 'semaine' : 'mois';
    const cle = (d: Date): string => {
      if (unite === 'mois') {
        return `${d.getFullYear()}-${d.getMonth()}`;
      }
      const j = new Date(d.getFullYear(), d.getMonth(), d.getDate());
      if (unite === 'semaine') {
        j.setDate(j.getDate() - ((j.getDay() + 6) % 7));
      }
      return j.toDateString();
    };
    const barres: Barre[] = [];
    const index = new Map<string, Barre>();
    const curseur = new Date(debut);
    while (curseur <= fin) {
      const k = cle(curseur);
      if (!index.has(k)) {
        const b: Barre = {
          cle: k,
          montant: 0,
          libelle:
            unite === 'mois'
              ? MOIS.format(curseur)
              : unite === 'semaine'
                ? 'Semaine du ' + JOUR.format(new Date(k))
                : JOUR_LONG.format(curseur),
          court: unite === 'mois' ? MOIS_COURT.format(curseur) : JOUR.format(unite === 'semaine' ? new Date(k) : curseur),
        };
        index.set(k, b);
        barres.push(b);
      }
      curseur.setDate(curseur.getDate() + 1);
    }
    const ajouter = (date: string, montant: number) => {
      const b = index.get(cle(lireDate(date)));
      if (b) {
        b.montant += montant;
      }
    };
    c.lignes.forEach((l) => ajouter(l.renduLe, l.encaisse));
    c.ventes.lignes.forEach((l) => ajouter(l.venduLe, l.encaisse));
    const max = Math.max(0, ...barres.map((b) => b.montant));
    return { unite, barres, max, pas: Math.max(1, Math.ceil(barres.length / 12)) };
  });

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
