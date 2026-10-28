import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { EurosPipe } from '../../core/formats';
import { MOYEN_PAIEMENT } from '../../core/libelles';
import { MoyenPaiement, Vente } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { FactureVente } from './facture-vente';

const ARTICLES_COURANTS = [
  'Protection d’écran',
  'Écouteurs',
  'Chargeur',
  'Câble',
  'Coque',
  'Souris',
  'Clavier',
  'Clé USB',
];

interface Ligne {
  article: string;
  quantite: number;
  prix: number | null;
  cout: number | null;
}

const nouvelleLigne = (article = ''): Ligne => ({ article, quantite: 1, prix: null, cout: null });

/** Vente au comptoir (accessoires, PC...) : les articles, le moyen de paiement, et c'est encaissé. */
@Component({
  selector: 'app-vente',
  imports: [FormsModule, RouterLink, EurosPipe, FactureVente],
  template: `
    <div class="page">
      <div class="entete-page">
        <div>
          <h1>Vente au comptoir</h1>
          <p class="sous-titre-page">Accessoires, PC reconditionnés... encaissé en deux clics.</p>
        </div>
        @if (auth.estAdmin()) {
          <a routerLink="/caisse" class="btn">Voir la caisse</a>
        }
      </div>

      @if (derniere(); as v) {
        <div class="bandeau bandeau-ok ligne ligne-espace">
          <span><strong>Vente enregistrée :</strong> {{ v.total | euros }} en {{ libellesMoyen[v.moyen].toLowerCase() }}.</span>
          <span class="ligne">
            <app-facture-vente [venteId]="v.id" />
            <button class="btn btn-petit btn-lien" (click)="derniere.set(null)">Fermer</button>
          </span>
        </div>
      }

      <div class="caisse">
        <section class="carte">
          <h2>Articles</h2>
          <p class="surtitre">Ajout rapide</p>
          <div class="rapides">
            @for (a of articlesCourants; track a) {
              <button type="button" class="btn btn-petit" (click)="ajouter(a)">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 6v12M6 12h12" /></svg>
                {{ a }}
              </button>
            }
          </div>

          <div class="articles">
            @for (l of lignes(); track $index; let i = $index) {
              <div class="article">
                <label class="champ nom">
                  <span>Article</span>
                  <input [(ngModel)]="l.article" [name]="'article' + i" placeholder="Ex : PC portable HP reconditionné" />
                </label>
                <label class="champ">
                  <span>Quantité</span>
                  <input type="number" inputmode="numeric" min="1" step="1" [(ngModel)]="l.quantite" [name]="'qte' + i" />
                </label>
                <label class="champ">
                  <span>Prix de vente</span>
                  <input type="number" inputmode="decimal" min="0" step="0.01" [(ngModel)]="l.prix" [name]="'prix' + i" placeholder="0,00" />
                </label>
                <label class="champ">
                  <span>Acheté à <small>(facultatif)</small></span>
                  <input type="number" inputmode="decimal" min="0" step="0.01" [(ngModel)]="l.cout" [name]="'cout' + i" placeholder="0,00" />
                </label>
                @if (lignes().length > 1) {
                  <button type="button" class="btn btn-icone btn-neutre retirer" (click)="retirer(i)" aria-label="Retirer l'article" title="Retirer">
                    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" /></svg>
                  </button>
                }
              </div>
            }
          </div>
          <button type="button" class="btn btn-petit" (click)="ajouter()">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 6v12M6 12h12" /></svg>
            Autre article
          </button>
        </section>

        <!-- Le ticket, comme sur une caisse : ce qui est vendu, le total, le paiement -->
        <aside class="carte ticket">
          <p class="surtitre">Ticket</p>
          <ul class="lignes-ticket">
            @for (l of lignes(); track $index) {
              @if (l.article.trim() || l.prix) {
                <li>
                  <span>{{ (l.quantite || 1) > 1 ? l.quantite + ' × ' : '' }}{{ l.article.trim() || 'Article' }}</span>
                  <span>{{ (Number(l.prix) || 0) * (Number(l.quantite) || 0) | euros }}</span>
                </li>
              }
            }
          </ul>
          @if (!aDesArticles()) {
            <p class="doux petit">Ajoutez un article à gauche.</p>
          }
          <div class="total">
            <span>Total</span>
            <span class="montant">{{ total() | euros }}</span>
          </div>

          <p class="surtitre">Payé par</p>
          <div class="choix moyens">
            @for (m of moyens; track m) {
              <label>
                <input type="radio" name="moyen" [value]="m" [(ngModel)]="moyen" />
                {{ libellesMoyen[m] }}
              </label>
            }
          </div>

          <button class="btn btn-ok btn-grand btn-bloc" [disabled]="envoi() || !valide()" (click)="encaisser()">
            Encaisser {{ total() | euros }}
          </button>
          @if (!valide() && aEssaye()) {
            <p class="erreur-champ">Chaque article doit avoir un nom, une quantité et un prix.</p>
          }
        </aside>
      </div>
    </div>
  `,
  styles: `
    .caisse {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 360px;
      gap: 1.25rem;
      align-items: start;
      max-width: 1180px;
    }
    .rapides {
      display: flex;
      flex-wrap: wrap;
      gap: 0.4rem;
      margin-bottom: 1.25rem;
    }
    .rapides .btn svg {
      width: 15px;
      height: 15px;
    }
    .articles {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }
    .article {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 5.5rem 7.5rem 8.5rem auto;
      gap: 0.6rem;
      align-items: end;
    }
    .retirer {
      margin-bottom: 4px;
    }
    .ticket {
      position: sticky;
      top: 1.5rem;
    }
    .lignes-ticket {
      list-style: none;
      margin: 0 0 0.5rem;
      padding: 0;
    }
    .lignes-ticket li {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      padding: 0.4rem 0;
      border-bottom: 1px dashed var(--bordure-forte);
      font-variant-numeric: tabular-nums;
    }
    .total {
      display: flex;
      align-items: baseline;
      justify-content: space-between;
      margin: 0.75rem 0 1.25rem;
      font-weight: 600;
    }
    .moyens {
      margin-bottom: 1.25rem;
    }
    .moyens label {
      flex: 1 1 40%;
      justify-content: center;
    }
    .erreur-champ {
      margin: 0.6rem 0 0;
    }
    @media (max-width: 1100px) {
      .caisse {
        grid-template-columns: minmax(0, 1fr);
        max-width: 860px;
      }
      .ticket {
        position: static;
      }
    }
    @media (max-width: 720px) {
      .article {
        grid-template-columns: 1fr 1fr;
        padding-bottom: 0.75rem;
        border-bottom: 1px solid var(--bordure);
      }
      .nom {
        grid-column: 1 / -1;
      }
      .retirer {
        grid-column: 1 / -1;
        width: 100%;
        border-radius: 999px;
      }
    }
  `,
})
export class VentePage {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);
  protected readonly auth = inject(AuthService);

  protected readonly articlesCourants = ARTICLES_COURANTS;
  protected readonly libellesMoyen = MOYEN_PAIEMENT;
  protected readonly moyens = Object.keys(MOYEN_PAIEMENT) as MoyenPaiement[];

  protected readonly lignes = signal<Ligne[]>([nouvelleLigne()]);
  protected moyen: MoyenPaiement = 'BANCONTACT';
  protected readonly envoi = signal(false);
  protected readonly aEssaye = signal(false);
  protected readonly derniere = signal<Vente | null>(null);

  protected readonly Number = Number;

  protected aDesArticles(): boolean {
    return this.lignes().some((l) => l.article.trim() || l.prix);
  }

  protected total(): number {
    return this.lignes().reduce((t, l) => t + (Number(l.prix) || 0) * (Number(l.quantite) || 0), 0);
  }

  protected valide(): boolean {
    return this.lignes().every((l) => l.article.trim() && l.quantite >= 1 && l.prix !== null && l.prix >= 0);
  }

  /** Un article courant remplit la première ligne vide, sinon en ajoute une. */
  protected ajouter(article = ''): void {
    const vide = this.lignes().find((l) => !l.article.trim());
    if (article && vide) {
      vide.article = article;
      this.lignes.update((l) => [...l]);
    } else {
      this.lignes.update((l) => [...l, nouvelleLigne(article)]);
    }
  }

  protected retirer(i: number): void {
    this.lignes.update((l) => l.filter((_, j) => j !== i));
  }

  protected encaisser(): void {
    this.aEssaye.set(true);
    if (!this.valide()) {
      return;
    }
    this.envoi.set(true);
    this.api
      .enregistrerVente({
        moyen: this.moyen,
        lignes: this.lignes().map((l) => ({
          article: l.article.trim(),
          quantite: Number(l.quantite),
          prixUnitaire: Number(l.prix),
          coutUnitaire: l.cout === null || (l.cout as unknown) === '' ? null : Number(l.cout),
        })),
      })
      .subscribe({
        next: (v) => {
          this.envoi.set(false);
          this.aEssaye.set(false);
          this.derniere.set(v);
          this.lignes.set([nouvelleLigne()]);
          this.notifications.ok('Vente enregistrée');
        },
        error: (err) => {
          this.envoi.set(false);
          this.notifications.erreur(err);
        },
      });
  }
}
