import { Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { DatePipe } from '../../core/formats';
import { initiales } from '../../core/libelles';
import { ClientDetail } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { Dialogue } from '../../shared/dialogue';
import { ListeDossiers } from '../../shared/liste-dossiers';

@Component({
  selector: 'app-client',
  imports: [FormsModule, RouterLink, DatePipe, Dialogue, ListeDossiers],
  template: `
    @if (detail(); as d) {
      <div class="page page-etroite">
        <a routerLink="/clients" class="retour">‹ Clients</a>

        <section class="carte profil">
          <span class="avatar-initiales grand" aria-hidden="true">{{ initiales(d.client.nom) }}</span>
          <h1>{{ d.client.nom }}</h1>
          <p class="doux">
            {{ d.client.telephone }}{{ d.client.email ? ' · ' + d.client.email : '' }}
          </p>
          <div class="actions">
            <a [href]="'tel:' + d.client.telephone" class="btn btn-principal">Appeler</a>
            @if (d.client.email) {
              <a [href]="'mailto:' + d.client.email" class="btn">Email</a>
            }
            <a routerLink="/depot" [queryParams]="{ client: d.client.id }" class="btn">Nouveau dépôt</a>
            <button class="btn" (click)="modifier()">Modifier</button>
          </div>
          <p class="doux petit depuis">Client depuis le {{ d.client.createdAt | date }}</p>
        </section>

        <h2 class="titre-liste">
          {{ d.dossiers.length }} réparation{{ d.dossiers.length > 1 ? 's' : '' }}
        </h2>
        <app-liste-dossiers [dossiers]="d.dossiers" [masquerClient]="true" vide="Aucune réparation pour ce client." />
      </div>

      <app-dialogue titre="Modifier le client" [ouvert]="edition()" (fermer)="edition.set(false)">
        <div class="pile">
          <label class="champ"><span>Nom</span><input [(ngModel)]="nom" /></label>
          <label class="champ"><span>Téléphone</span><input type="tel" [(ngModel)]="telephone" /></label>
          <label class="champ"><span>Email</span><input type="email" [(ngModel)]="email" /></label>
        </div>
        <div class="dialogue-actions">
          <button class="btn btn-neutre" (click)="edition.set(false)">Annuler</button>
          <button class="btn btn-principal" (click)="enregistrer()">Enregistrer</button>
        </div>
      </app-dialogue>
    } @else {
      <div class="page"><p class="vide">Chargement…</p></div>
    }
  `,
  styles: `
    .retour {
      display: inline-block;
      margin-bottom: 1rem;
      font-weight: 600;
      text-decoration: none;
    }
    .profil {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      padding: 2rem 1.5rem 1.5rem;
    }
    .grand {
      width: 76px;
      height: 76px;
      font-size: 1.6rem;
      margin-bottom: 0.9rem;
    }
    .profil h1 {
      margin-bottom: 0.25rem;
    }
    .actions {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 0.5rem;
      margin-top: 0.75rem;
    }
    .depuis {
      margin: 1rem 0 0;
    }
    .titre-liste {
      margin: 1.75rem 0 0.75rem 0.25rem;
    }
  `,
})
export class ClientPage {
  protected readonly initiales = initiales;
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);

  readonly id = input.required<string>();
  protected readonly detail = signal<ClientDetail | null>(null);
  protected readonly edition = signal(false);
  protected nom = '';
  protected telephone = '';
  protected email = '';

  constructor() {
    effect(() => this.charger(this.id()));
  }

  private charger(id: string): void {
    this.api.client(Number(id)).subscribe({
      next: (d) => this.detail.set(d),
      error: (err) => this.notifications.erreur(err),
    });
  }

  protected modifier(): void {
    const c = this.detail()!.client;
    this.nom = c.nom;
    this.telephone = c.telephone;
    this.email = c.email ?? '';
    this.edition.set(true);
  }

  protected enregistrer(): void {
    this.api
      .modifierClient(this.detail()!.client.id, {
        nom: this.nom.trim(),
        telephone: this.telephone.trim(),
        email: this.email.trim() || null,
      })
      .subscribe({
        next: () => {
          this.edition.set(false);
          this.notifications.ok('Client modifié');
          this.charger(this.id());
        },
        error: (err) => this.notifications.erreur(err),
      });
  }
}
