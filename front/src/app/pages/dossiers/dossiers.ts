import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { STATUT, STATUTS } from '../../core/libelles';
import { DossierFiltres, DossierResume, Employe, Stats, StatutDossier } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { ListeDossiers } from '../../shared/liste-dossiers';

interface Vue {
  cle: string;
  libelle: string;
  filtres: DossierFiltres;
  /** Nombre affiché sur l'onglet, lu dans les statistiques. */
  nombre?: (s: Stats) => number;
}

const VUES: Vue[] = [
  { cle: 'actifs', libelle: 'En cours', filtres: { actifs: true }, nombre: (s) => s.actifs },
  { cle: 'mes', libelle: 'Les miennes', filtres: { mesDossiers: true, actifs: true }, nombre: (s) => s.mesDossiers },
  { cle: 'prets', libelle: 'Prêts à rendre', filtres: { statut: 'PRET', sort: 'dateEstimee,asc' }, nombre: (s) => s.parStatut.PRET },
  { cle: 'nonrecuperes', libelle: 'Pas récupérés', filtres: { nonRecuperes: true }, nombre: (s) => s.nonRecuperes },
  { cle: 'retard', libelle: 'En retard', filtres: { enRetard: true, sort: 'dateEstimee,asc' }, nombre: (s) => s.enRetard },
  { cle: 'tous', libelle: 'Tous', filtres: {} },
];

const TAILLE = 25;

@Component({
  selector: 'app-dossiers',
  imports: [FormsModule, RouterLink, ListeDossiers],
  templateUrl: './dossiers.html',
})
export class Dossiers {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);

  protected readonly vues = VUES;
  protected readonly statuts = STATUTS;
  protected readonly libellesStatut = STATUT;

  protected readonly vue = signal('actifs');
  protected q = '';
  protected statut: StatutDossier | '' = '';
  protected technicienId: number | '' = '';
  protected readonly techniciens = signal<Employe[]>([]);
  protected readonly stats = signal<Stats | null>(null);

  protected readonly dossiers = signal<DossierResume[]>([]);
  protected readonly total = signal(0);
  protected readonly chargement = signal(true);
  private page = 0;
  private readonly recherche = new Subject<string>();

  constructor() {
    this.api.techniciens().subscribe((t) => this.techniciens.set(t));
    this.api.stats().subscribe((s) => this.stats.set(s));

    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((p) => {
      this.vue.set(VUES.some((v) => v.cle === p.get('vue')) ? p.get('vue')! : p.get('q') ? 'tous' : 'actifs');
      this.q = p.get('q') ?? '';
      this.statut = (p.get('statut') as StatutDossier) ?? '';
      this.technicienId = p.get('technicien') ? Number(p.get('technicien')) : '';
      this.charger(true);
    });

    this.recherche
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => this.appliquer());
  }

  protected tape(): void {
    this.recherche.next(this.q);
  }

  /** Met les filtres dans l'adresse (retour arrière et lien partageable). */
  protected appliquer(vue = this.vue()): void {
    this.router.navigate([], {
      queryParams: {
        vue,
        q: this.q.trim() || null,
        statut: this.statut || null,
        technicien: this.technicienId || null,
      },
      replaceUrl: true,
    });
  }

  /** Clic sur une étape : affiche tous les dossiers à cette étape (un 2e clic enlève le filtre). */
  protected filtrerEtape(s: StatutDossier): void {
    this.statut = this.statut === s ? '' : s;
    this.appliquer(this.statut ? 'tous' : this.vue());
  }

  protected plus(): void {
    this.page++;
    this.charger(false);
  }

  private charger(depuisDebut: boolean): void {
    if (depuisDebut) {
      this.page = 0;
    }
    const vue = VUES.find((v) => v.cle === this.vue())!;
    const filtres: DossierFiltres = {
      ...vue.filtres,
      q: this.q.trim() || undefined,
      technicienId: this.technicienId || undefined,
      page: this.page,
      size: TAILLE,
    };
    if (this.statut) {
      filtres.statut = this.statut;
      filtres.actifs = undefined;
    }
    this.chargement.set(true);
    this.api.dossiers(filtres).subscribe({
      next: (r) => {
        this.dossiers.set(depuisDebut ? r.content : [...this.dossiers(), ...r.content]);
        this.total.set(r.page.totalElements);
        this.chargement.set(false);
      },
      error: (err) => {
        this.chargement.set(false);
        this.notifications.erreur(err);
      },
    });
  }
}
