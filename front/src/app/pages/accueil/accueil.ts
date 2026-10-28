import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { DossierResume, Stats } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { ListeDossiers } from '../../shared/liste-dossiers';

/** Accueil : le nombre de réparations en cours, les 4 actions de tous les jours, puis ce qui demande de l'attention. */
@Component({
  selector: 'app-accueil',
  imports: [FormsModule, RouterLink, ListeDossiers],
  templateUrl: './accueil.html',
  styleUrl: './accueil.css',
})
export class Accueil {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  protected readonly auth = inject(AuthService);

  protected readonly stats = signal<Stats | null>(null);
  protected readonly aSurveiller = signal<DossierResume[]>([]);
  protected readonly mesDossiers = signal<DossierResume[]>([]);
  protected recherche = '';
  /** « Mardi 28 octobre » */
  protected readonly dateDuJour = new Intl.DateTimeFormat('fr-BE', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date());

  constructor() {
    forkJoin({
      stats: this.api.stats(),
      nonRecuperes: this.api.dossiers({ nonRecuperes: true, size: 10, sort: 'dateEstimee,asc' }),
      enRetard: this.api.dossiers({ enRetard: true, size: 10, sort: 'dateEstimee,asc' }),
      mes: this.api.dossiers({ mesDossiers: true, actifs: true, size: 10, sort: 'dateEntree,asc' }),
    }).subscribe({
      next: (r) => {
        this.stats.set(r.stats);
        this.compterJusqua(r.stats.actifs);
        const vus = new Set<number>();
        this.aSurveiller.set(
          [...r.nonRecuperes.content, ...r.enRetard.content].filter((d) => !vus.has(d.id) && vus.add(d.id)),
        );
        this.mesDossiers.set(r.mes.content);
      },
      error: (err) => this.notifications.erreur(err),
    });
  }

  /** Le grand chiffre défile de 0 à sa valeur, comme un solde qui s'affiche. */
  protected readonly compteur = signal(0);

  private compterJusqua(cible: number): void {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      this.compteur.set(cible);
      return;
    }
    const debut = performance.now();
    const duree = 700;
    const etape = (t: number) => {
      const p = Math.min(1, (t - debut) / duree);
      this.compteur.set(Math.round(cible * (1 - Math.pow(1 - p, 3))));
      if (p < 1) {
        requestAnimationFrame(etape);
      }
    };
    requestAnimationFrame(etape);
  }

  protected chercher(): void {
    const q = this.recherche.trim();
    this.router.navigate(['/dossiers'], { queryParams: q ? { q, vue: 'tous' } : {} });
  }
}
