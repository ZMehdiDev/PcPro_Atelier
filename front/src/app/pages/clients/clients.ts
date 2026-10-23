import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, startWith, switchMap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { DatePipe } from '../../core/formats';
import { Client } from '../../core/models';
import { NotificationService } from '../../core/notification.service';

@Component({
  selector: 'app-clients',
  imports: [FormsModule, RouterLink, DatePipe],
  template: `
    <div class="page page-etroite">
      <div class="entete-page"><h1>Clients</h1></div>
      <input
        type="search"
        [(ngModel)]="q"
        (ngModelChange)="recherche.next($event)"
        placeholder="Nom ou téléphone"
        aria-label="Chercher un client"
        enterkeyhint="search"
      />
      <p class="doux petit" style="margin-top: 0.5rem">{{ total() }} client{{ total() > 1 ? 's' : '' }}</p>
      <div class="carte" style="padding: 0 1rem">
        <ul class="liste">
          @for (c of clients(); track c.id) {
            <li>
              <a [routerLink]="['/clients', c.id]" class="client">
                <span>
                  <strong>{{ c.nom }}</strong><br />
                  <span class="doux petit">{{ c.telephone }}{{ c.email ? ' · ' + c.email : '' }}</span>
                </span>
                <span class="doux petit masquer-mobile">Client depuis le {{ c.createdAt | date }}</span>
              </a>
            </li>
          } @empty {
            <li class="vide">Aucun client trouvé.</li>
          }
        </ul>
      </div>
      @if (clients().length < total()) {
        <div class="ligne" style="justify-content: center">
          <button class="btn" (click)="plus()">Afficher plus</button>
        </div>
      }
    </div>
  `,
  styles: `
    .client {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 1rem;
      color: var(--texte);
      text-decoration: none;
      min-height: 44px;
    }
  `,
})
export class Clients {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);

  protected q = '';
  protected readonly recherche = new Subject<string>();
  protected readonly clients = signal<Client[]>([]);
  protected readonly total = signal(0);
  private page = 0;

  constructor() {
    this.recherche
      .pipe(
        startWith(''),
        debounceTime(300),
        switchMap((q) => {
          this.page = 0;
          return this.api.clients(q.trim(), 0, 30);
        }),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: (r) => {
          this.clients.set(r.content);
          this.total.set(r.page.totalElements);
        },
        error: (err) => this.notifications.erreur(err),
      });
  }

  protected plus(): void {
    this.page++;
    this.api.clients(this.q.trim(), this.page, 30).subscribe((r) => this.clients.update((c) => [...c, ...r.content]));
  }
}
