import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { EurosPipe } from '../../core/formats';
import { MOYEN_PAIEMENT } from '../../core/libelles';
import { MoyenPaiement, Vente } from '../../core/models';
import { NotificationService } from '../../core/notification.service';

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
  imports: [FormsModule, RouterLink, EurosPipe],
  template: `
    <div class="page page-etroite">
      <div class="entete-page">
        <h1>Vente au comptoir</h1>
        @if (auth.estAdmin()) {
          <a routerLink="/caisse" class="btn btn-petit btn-blanc">Voir la caisse</a>
        }
      </div>

      @if (derniere(); as v) {
        <div class="bandeau bandeau-ok ligne ligne-espace">
          <span><strong>Vente enregistrée :</strong> {{ v.total | euros }} en {{ libellesMoyen[v.moyen].toLowerCase() }}.</span>
          <button class="btn btn-petit btn-lien" (click)="derniere.set(null)">Fermer</button>
        </div>
      }

      <section class="carte">
        <h2>Articles</h2>
        <div class="rapides">
          @for (a of articlesCourants; track a) {
            <button type="button" class="btn btn-petit" (click)="ajouter(a)">+ {{ a }}</button>
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
                <button type="button" class="btn btn-petit btn-lien retirer" (click)="retirer(i)">Retirer</button>
              }
            </div>
          }
        </div>
        <button type="button" class="btn btn-petit" (click)="ajouter()">+ Autre article</button>
      </section>

      <section class="carte">
        <h2>Payé par</h2>
        <div class="choix">
          @for (m of moyens; track m) {
            <label>
              <input type="radio" name="moyen" [value]="m" [(ngModel)]="moyen" />
              {{ libellesMoyen[m] }}
            </label>
          }
        </div>
      </section>

      <section class="carte total">
        <div>
          <span class="doux">Total</span>
          <div class="montant">{{ total() | euros }}</div>
        </div>
        <button class="btn btn-ok btn-grand" [disabled]="envoi() || !valide()" (click)="encaisser()">
          Encaisser {{ total() | euros }}
        </button>
      </section>
      @if (!valide() && aEssaye()) {
        <p class="erreur-champ">Chaque article doit avoir un nom, une quantité et un prix.</p>
      }
    </div>
  `,
  styles: `
    .rapides {
      display: flex;
      flex-wrap: wrap;
      gap: 0.4rem;
      margin-bottom: 1rem;
    }
    .articles {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }
    .article {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 6rem 8rem 8.5rem auto;
      gap: 0.6rem;
      align-items: end;
    }
    .retirer {
      min-height: 48px;
    }
    .total {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
    }
    .total .btn {
      min-width: 240px;
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
      .total .btn {
        width: 100%;
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
