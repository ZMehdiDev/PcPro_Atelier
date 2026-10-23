import { Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { DatePipe } from '../../core/formats';
import { ClientDetail } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { Dialogue } from '../../shared/dialogue';
import { ListeDossiers } from '../../shared/liste-dossiers';

@Component({
  selector: 'app-client',
  imports: [FormsModule, RouterLink, DatePipe, Dialogue, ListeDossiers],
  template: `
    @if (detail(); as d) {
      <div class="page">
        <a routerLink="/clients" style="text-decoration: none">← Clients</a>
        <div class="entete-page" style="margin-top: 0.75rem">
          <h1>{{ d.client.nom }}</h1>
          <button class="btn" (click)="modifier()">Modifier</button>
        </div>
        <section class="carte">
          <div class="ligne" style="gap: 1.5rem">
            <a [href]="'tel:' + d.client.telephone" class="btn">Appeler {{ d.client.telephone }}</a>
            @if (d.client.email) {
              <a [href]="'mailto:' + d.client.email">{{ d.client.email }}</a>
            } @else {
              <span class="doux">Pas d'email</span>
            }
            <span class="doux petit">Client depuis le {{ d.client.createdAt | date }}</span>
          </div>
        </section>
        <section class="carte">
          <h2>{{ d.dossiers.length }} dossier{{ d.dossiers.length > 1 ? 's' : '' }}</h2>
          <app-liste-dossiers [dossiers]="d.dossiers" [masquerClient]="true" />
        </section>
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
})
export class ClientPage {
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
