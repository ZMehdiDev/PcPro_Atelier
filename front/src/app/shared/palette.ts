import { Component, ElementRef, Injectable, computed, effect, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, catchError, debounceTime, forkJoin, map, of, switchMap } from 'rxjs';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { appareil, initiales, teinte } from '../core/libelles';
import { StatutDossier } from '../core/models';
import { RecentsService } from '../core/recents.service';
import { ThemeService } from '../core/theme.service';
import { Statut } from './statut';

@Injectable({ providedIn: 'root' })
export class PaletteService {
  readonly ouverte = signal(false);

  ouvrir(): void {
    this.ouverte.set(true);
  }

  basculer(): void {
    this.ouverte.update((o) => !o);
  }
}

interface Element {
  genre: 'action' | 'dossier' | 'client';
  titre: string;
  detail?: string;
  statut?: StatutDossier;
  lien?: unknown[];
  params?: Record<string, string>;
  executer?: () => void;
}

interface Groupe {
  titre: string;
  elements: (Element & { rang: number })[];
}

/** « Rhône » → « rhone » : on cherche sans se soucier des accents ni des majuscules. */
const normaliser = (s: string) =>
  s
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase();

/**
 * Recherche rapide (Ctrl+K ou « / ») : un client, un téléphone, une référence, ou une page
 * de l'appli, depuis n'importe quel écran. Flèches pour choisir, Entrée pour ouvrir.
 */
@Component({
  selector: 'app-palette',
  imports: [FormsModule, Statut],
  template: `
    <dialog #dialog class="palette" (close)="palette.ouverte.set(false)" (click)="clicFond($event)" aria-label="Recherche rapide">
      <div class="saisie">
        <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7" /><path d="M20 20l-3.5-3.5" /></svg>
        <input
          #champ
          type="search"
          [ngModel]="q()"
          (ngModelChange)="taper($event)"
          (keydown)="clavier($event)"
          placeholder="Client, téléphone, référence, page…"
          aria-label="Rechercher"
          enterkeyhint="go"
          autocomplete="off"
        />
        <button type="button" class="fermer" (click)="dialog.close()" aria-label="Fermer">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" /></svg>
        </button>
      </div>

      <div class="resultats" role="listbox">
        @for (g of groupes(); track g.titre) {
          <p class="surtitre">{{ g.titre }}</p>
          @for (e of g.elements; track e.rang) {
            <button
              type="button"
              role="option"
              class="resultat"
              [class.actif]="actif() === e.rang"
              [attr.aria-selected]="actif() === e.rang"
              (mousemove)="actif.set(e.rang)"
              (click)="choisir(e)"
            >
              @if (e.genre === 'action') {
                <span class="icone" aria-hidden="true">
                  <svg viewBox="0 0 24 24"><path d="M5 12h14M13 6l6 6-6 6" /></svg>
                </span>
              } @else {
                <span class="avatar-initiales" [style.--teinte]="teinte(e.titre)" aria-hidden="true">{{ initiales(e.titre) }}</span>
              }
              <span class="textes">
                <span class="titre">{{ e.titre }}</span>
                @if (e.detail) {
                  <span class="detail">{{ e.detail }}</span>
                }
              </span>
              @if (e.statut) {
                <app-statut [statut]="e.statut" />
              }
              <span class="entree" aria-hidden="true">Entrée</span>
            </button>
          }
        }
        @if (cherche() && !groupes().length) {
          <p class="vide">Rien trouvé pour « {{ q() }} ».</p>
        }
      </div>

      <div class="pied masquer-mobile">
        <span><kbd>↑</kbd> <kbd>↓</kbd> choisir</span>
        <span><kbd>Entrée</kbd> ouvrir</span>
        <span><kbd>Échap</kbd> fermer</span>
      </div>
    </dialog>
  `,
  styles: `
    .palette {
      width: min(660px, calc(100vw - 1.5rem));
      margin: 12vh auto auto;
      max-height: 72dvh;
      padding: 0;
      overflow: hidden;
      display: none;
      flex-direction: column;
    }
    .palette[open] {
      display: flex;
    }
    .saisie {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.6rem 0.75rem 0.6rem 1.25rem;
      border-bottom: 1px solid var(--bordure);
    }
    .saisie > svg {
      width: 22px;
      height: 22px;
      flex: none;
      fill: none;
      stroke: var(--texte-doux);
      stroke-width: 2;
      stroke-linecap: round;
    }
    .saisie input {
      border: 0;
      box-shadow: none;
      padding: 0;
      min-height: 52px;
      font-size: 1.15rem;
      background: transparent;
    }
    .saisie input:focus {
      box-shadow: none;
    }
    .saisie input::-webkit-search-cancel-button {
      display: none;
    }
    .resultats {
      overflow-y: auto;
      padding: 0.5rem;
    }
    .surtitre {
      margin: 0.65rem 0.75rem 0.35rem;
    }
    .resultat {
      display: flex;
      align-items: center;
      gap: 0.85rem;
      width: 100%;
      min-height: 56px;
      padding: 0.45rem 0.75rem;
      border: 0;
      border-radius: 14px;
      background: transparent;
      color: var(--texte);
      font: inherit;
      text-align: left;
      cursor: pointer;
    }
    .resultat.actif {
      background: var(--champ);
    }
    .resultat .avatar-initiales {
      width: 38px;
      height: 38px;
      font-size: 0.8rem;
    }
    .icone {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex: none;
      width: 38px;
      height: 38px;
      border-radius: 50%;
      background: var(--accent-clair);
      color: var(--accent);
    }
    .icone svg {
      width: 18px;
      height: 18px;
      fill: none;
      stroke: currentColor;
      stroke-width: 2.2;
      stroke-linecap: round;
      stroke-linejoin: round;
    }
    .textes {
      display: flex;
      flex-direction: column;
      flex: 1;
      min-width: 0;
    }
    .titre {
      font-weight: 600;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .detail {
      color: var(--texte-doux);
      font-size: 0.875rem;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .entree {
      visibility: hidden;
      font-size: 0.75rem;
      color: var(--texte-pale);
    }
    .resultat.actif .entree {
      visibility: visible;
    }
    .vide {
      padding: 2rem 1rem;
    }
    .pied {
      display: flex;
      gap: 1.25rem;
      padding: 0.65rem 1.25rem;
      border-top: 1px solid var(--bordure);
      background: var(--surface-douce);
      color: var(--texte-doux);
      font-size: 0.8rem;
    }
    kbd {
      display: inline-block;
      min-width: 1.4rem;
      padding: 0.05rem 0.35rem;
      border: 1px solid var(--bordure-forte);
      border-bottom-width: 2px;
      border-radius: 6px;
      background: var(--surface);
      font: inherit;
      font-size: 0.72rem;
      text-align: center;
    }
    @media (max-width: 720px) {
      .palette {
        margin: 0;
        width: 100vw;
        max-width: 100vw;
        height: 100dvh;
        max-height: 100dvh;
        border-radius: 0;
        border: 0;
      }
      .palette[open] {
        animation: none;
      }
      .entree {
        display: none;
      }
    }
  `,
})
export class Palette {
  protected readonly palette = inject(PaletteService);
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly theme = inject(ThemeService);
  private readonly recents = inject(RecentsService);

  protected readonly initiales = initiales;
  protected readonly teinte = teinte;

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private readonly champ = viewChild.required<ElementRef<HTMLInputElement>>('champ');

  protected readonly q = signal('');
  protected readonly actif = signal(0);
  protected readonly cherche = signal(false);
  private readonly trouves = signal<{ dossiers: Element[]; clients: Element[] }>({ dossiers: [], clients: [] });
  private readonly recherche = new Subject<string>();

  private readonly actions = computed<Element[]>(() => {
    const admin = this.auth.estAdmin();
    const liste: (Element | false)[] = [
      { genre: 'action', titre: 'Nouveau dépôt', detail: 'Enregistrer un appareil qui arrive', lien: ['/depot'] },
      { genre: 'action', titre: 'Vente au comptoir', detail: 'Accessoires, PC...', lien: ['/vente'] },
      { genre: 'action', titre: 'Prêts à rendre', lien: ['/dossiers'], params: { vue: 'prets' } },
      { genre: 'action', titre: 'Mes réparations', lien: ['/dossiers'], params: { vue: 'mes' } },
      { genre: 'action', titre: 'Réparations en retard', lien: ['/dossiers'], params: { vue: 'retard' } },
      { genre: 'action', titre: 'Toutes les réparations', lien: ['/dossiers'], params: { vue: 'tous' } },
      { genre: 'action', titre: 'Clients', lien: ['/clients'] },
      admin && { genre: 'action', titre: 'Caisse du jour', lien: ['/caisse'] },
      admin && { genre: 'action', titre: "Chiffre d'affaires", lien: ['/chiffre-affaires'] },
      admin && { genre: 'action', titre: 'Employés', lien: ['/employes'] },
      { genre: 'action', titre: 'Mon compte', detail: 'Mot de passe, déconnexion', lien: ['/compte'] },
      {
        genre: 'action',
        titre: this.theme.theme() === 'dark' ? 'Passer en mode clair' : 'Passer en mode sombre',
        executer: () => this.theme.basculer(),
      },
    ];
    return liste.filter((e): e is Element => !!e);
  });

  protected readonly groupes = computed<Groupe[]>(() => {
    const q = normaliser(this.q().trim());
    const groupes: { titre: string; elements: Element[] }[] = [];
    if (!q) {
      const recents = this.recents.dossiers().map(
        (r): Element => ({ genre: 'dossier', titre: r.titre, detail: r.detail, lien: ['/dossiers', r.id] }),
      );
      groupes.push({ titre: 'Ouverts récemment', elements: recents });
      groupes.push({ titre: 'Aller à', elements: this.actions().slice(0, 7) });
    } else {
      groupes.push({ titre: 'Réparations', elements: this.trouves().dossiers });
      groupes.push({ titre: 'Clients', elements: this.trouves().clients });
      groupes.push({
        titre: 'Aller à',
        elements: this.actions().filter((a) => normaliser(a.titre + ' ' + (a.detail ?? '')).includes(q)),
      });
    }
    let rang = 0;
    return groupes
      .filter((g) => g.elements.length)
      .map((g) => ({ titre: g.titre, elements: g.elements.map((e) => ({ ...e, rang: rang++ })) }));
  });

  private readonly total = computed(() => this.groupes().reduce((n, g) => n + g.elements.length, 0));

  constructor() {
    effect(() => {
      const d = this.dialog().nativeElement;
      if (this.palette.ouverte() && !d.open) {
        this.q.set('');
        this.trouves.set({ dossiers: [], clients: [] });
        this.cherche.set(false);
        this.actif.set(0);
        d.showModal();
        setTimeout(() => this.champ().nativeElement.focus());
      } else if (!this.palette.ouverte() && d.open) {
        d.close();
      }
    });

    this.recherche
      .pipe(
        debounceTime(180),
        switchMap((q) =>
          q.length < 2
            ? of(null)
            : forkJoin({
                dossiers: this.api.dossiers({ q, size: 5, sort: 'dateEntree,desc' }),
                clients: this.api.clients(q, 0, 4),
              }).pipe(catchError(() => of(null))),
        ),
        map((r) => ({
          dossiers: (r?.dossiers.content ?? []).map(
            (d): Element => ({
              genre: 'dossier',
              titre: d.clientNom,
              detail: `${appareil(d)} · ${d.reference}`,
              statut: d.statut,
              lien: ['/dossiers', d.id],
            }),
          ),
          clients: (r?.clients.content ?? []).map(
            (c): Element => ({
              genre: 'client',
              titre: c.nom,
              detail: c.telephone + (c.email ? ` · ${c.email}` : ''),
              lien: ['/clients', c.id],
            }),
          ),
        })),
        takeUntilDestroyed(),
      )
      .subscribe((r) => {
        this.trouves.set(r);
        this.cherche.set(this.q().trim().length >= 2);
        this.actif.set(0);
      });
  }

  protected taper(q: string): void {
    this.q.set(q);
    this.actif.set(0);
    this.recherche.next(q.trim());
  }

  protected clavier(event: KeyboardEvent): void {
    const n = this.total();
    if (event.key === 'ArrowDown' && n) {
      event.preventDefault();
      this.actif.update((i) => (i + 1) % n);
      this.defiler();
    } else if (event.key === 'ArrowUp' && n) {
      event.preventDefault();
      this.actif.update((i) => (i - 1 + n) % n);
      this.defiler();
    } else if (event.key === 'Enter') {
      event.preventDefault();
      const e = this.groupes()
        .flatMap((g) => g.elements)
        .find((x) => x.rang === this.actif());
      if (e) {
        this.choisir(e);
      } else if (this.q().trim()) {
        // Rien de proposé encore : on ouvre la liste filtrée
        this.palette.ouverte.set(false);
        this.router.navigate(['/dossiers'], { queryParams: { q: this.q().trim(), vue: 'tous' } });
      }
    }
  }

  protected choisir(e: Element): void {
    this.palette.ouverte.set(false);
    if (e.executer) {
      e.executer();
    } else if (e.lien) {
      this.router.navigate(e.lien, { queryParams: e.params ?? {} });
    }
  }

  protected clicFond(event: MouseEvent): void {
    if (event.target === this.dialog().nativeElement) {
      this.palette.ouverte.set(false);
    }
  }

  private defiler(): void {
    setTimeout(() => this.dialog().nativeElement.querySelector('.resultat.actif')?.scrollIntoView({ block: 'nearest' }));
  }
}
