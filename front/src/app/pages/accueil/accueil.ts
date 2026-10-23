import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { DossierResume, Stats } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { ListeDossiers } from '../../shared/liste-dossiers';

/** Accueil : les 4 choses qu'on fait tout le temps, puis ce qui demande de l'attention. */
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
  protected readonly aujourdhui = new Date().toLocaleDateString('fr-BE', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  });

  constructor() {
    forkJoin({
      stats: this.api.stats(),
      nonRecuperes: this.api.dossiers({ nonRecuperes: true, size: 10, sort: 'dateEstimee,asc' }),
      enRetard: this.api.dossiers({ enRetard: true, size: 10, sort: 'dateEstimee,asc' }),
      mes: this.api.dossiers({ mesDossiers: true, actifs: true, size: 10, sort: 'dateEntree,asc' }),
    }).subscribe({
      next: (r) => {
        this.stats.set(r.stats);
        const vus = new Set<number>();
        this.aSurveiller.set(
          [...r.nonRecuperes.content, ...r.enRetard.content].filter((d) => !vus.has(d.id) && vus.add(d.id)),
        );
        this.mesDossiers.set(r.mes.content);
      },
      error: (err) => this.notifications.erreur(err),
    });
  }

  protected compte(n: number | undefined, aucun: string, un: string, plusieurs: string): string {
    if (n === undefined) {
      return '…';
    }
    return n === 0 ? aucun : `${n} ${n > 1 ? plusieurs : un}`;
  }

  protected chercher(): void {
    const q = this.recherche.trim();
    this.router.navigate(['/dossiers'], { queryParams: q ? { q, vue: 'tous' } : {} });
  }
}
