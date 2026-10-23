import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Observable, Subject, debounceTime, of, switchMap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { aujourdhui, DatePipe } from '../../core/formats';
import { PRIORITE, TYPE_APPAREIL } from '../../core/libelles';
import { Client, Employe, Garantie, Priorite, TypeAppareil } from '../../core/models';
import { NotificationService } from '../../core/notification.service';

const ACCESSOIRES = ['Chargeur', 'Sacoche', 'Souris', 'Câble', 'Boîte'];

/** Nouveau dépôt : client, appareil, problème. Une seule page, puis le bon de dépôt. */
@Component({
  selector: 'app-depot',
  imports: [FormsModule, DatePipe],
  templateUrl: './depot.html',
  styleUrl: './depot.css',
})
export class Depot {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);

  protected readonly typesAppareil = Object.keys(TYPE_APPAREIL) as TypeAppareil[];
  protected readonly libellesType = TYPE_APPAREIL;
  protected readonly priorites: Priorite[] = ['NORMALE', 'HAUTE', 'BASSE'];
  protected readonly libellesPriorite = PRIORITE;
  protected readonly accessoiresCourants = ACCESSOIRES;
  protected readonly minDate = aujourdhui();

  // Client
  protected telephone = '';
  protected nom = '';
  protected email = '';
  protected readonly clientConnu = signal<Client | null>(null);
  protected readonly suggestions = signal<Client[]>([]);

  // Appareil et dépôt
  protected type: TypeAppareil = 'PORTABLE';
  protected marque = '';
  protected modele = '';
  protected numeroSerie = '';
  protected probleme = '';
  protected accessoires = '';
  protected dateEstimee = '';
  protected priorite: Priorite = 'NORMALE';
  protected technicienId: number | null = null;
  protected readonly techniciens = signal<Employe[]>([]);

  protected readonly garanties = signal<Garantie[]>([]);
  protected readonly envoi = signal(false);
  protected readonly erreurs = signal<string[]>([]);

  private readonly rechercheClient = new Subject<string>();
  private readonly rechercheGarantie = new Subject<void>();

  constructor() {
    this.api.techniciens().subscribe((t) => {
      this.techniciens.set(t);
      this.technicienId = this.auth.employe()?.id ?? null;
    });

    this.rechercheClient
      .pipe(
        debounceTime(300),
        switchMap((q) => (q.length >= 3 ? this.api.clients(q, 0, 5) : of(null))),
        takeUntilDestroyed(),
      )
      .subscribe((r) => this.suggestions.set(r?.content ?? []));

    this.rechercheGarantie
      .pipe(
        debounceTime(400),
        switchMap((): Observable<Garantie[]> => {
          const clientId = this.clientConnu()?.id ?? null;
          const serie = this.numeroSerie.trim() || null;
          return clientId || serie ? this.api.garanties(clientId, serie) : of([]);
        }),
        takeUntilDestroyed(),
      )
      .subscribe((g) => this.garanties.set(g));
  }

  protected tapeClient(valeur: string): void {
    if (!this.clientConnu()) {
      this.rechercheClient.next(valeur.trim());
    }
  }

  protected choisirClient(c: Client): void {
    this.clientConnu.set(c);
    this.telephone = c.telephone;
    this.nom = c.nom;
    this.email = c.email ?? '';
    this.suggestions.set([]);
    this.rechercheGarantie.next();
  }

  protected autreClient(): void {
    this.clientConnu.set(null);
    this.telephone = '';
    this.nom = '';
    this.email = '';
    this.rechercheGarantie.next();
  }

  protected verifierGarantie(): void {
    this.rechercheGarantie.next();
  }

  protected ajouterAccessoire(a: string): void {
    const liste = this.accessoires.split(',').map((x) => x.trim()).filter(Boolean);
    if (!liste.includes(a)) {
      liste.push(a);
    }
    this.accessoires = liste.join(', ');
  }

  protected dansJours(jours: number): void {
    this.dateEstimee = aujourdhui(jours);
  }

  protected valider(): void {
    const erreurs: string[] = [];
    if (!this.telephone.trim()) erreurs.push('Le téléphone du client est obligatoire.');
    if (!this.nom.trim()) erreurs.push('Le nom du client est obligatoire.');
    if (!this.probleme.trim()) erreurs.push('Décrivez le problème.');
    this.erreurs.set(erreurs);
    if (erreurs.length) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }

    this.envoi.set(true);
    const connu = this.clientConnu();
    const infosClient = { nom: this.nom.trim(), telephone: this.telephone.trim(), email: this.email.trim() || null };
    // Client connu dont on a corrigé le nom ou ajouté l'email : on met sa fiche à jour d'abord
    const majClient: Observable<unknown> =
      connu && (connu.nom !== infosClient.nom || (connu.email ?? null) !== infosClient.email || connu.telephone !== infosClient.telephone)
        ? this.api.modifierClient(connu.id, infosClient)
        : of(null);

    majClient
      .pipe(
        switchMap(() =>
          this.api.creerDossier({
            clientId: connu?.id ?? null,
            client: connu ? null : infosClient,
            appareil: {
              type: this.type,
              marque: this.marque.trim() || null,
              modele: this.modele.trim() || null,
              numeroSerie: this.numeroSerie.trim() || null,
            },
            priorite: this.priorite,
            descriptionProbleme: this.probleme.trim(),
            accessoires: this.accessoires.trim() || null,
            dateEstimee: this.dateEstimee || null,
            technicienAssigneId: this.technicienId,
          }),
        ),
      )
      .subscribe({
        next: (d) => {
          this.notifications.ok(`Dossier ${d.reference} créé`);
          this.router.navigate(['/dossiers', d.id], { queryParams: { nouveau: 1 } });
        },
        error: (err) => {
          this.envoi.set(false);
          this.notifications.erreur(err);
        },
      });
  }
}
