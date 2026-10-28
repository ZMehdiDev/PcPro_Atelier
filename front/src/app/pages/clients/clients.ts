import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, startWith, switchMap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { initiales, teinte } from '../../core/libelles';
import { Client } from '../../core/models';
import { NotificationService } from '../../core/notification.service';

/** Clients : une recherche, puis une ligne par client comme un carnet de contacts. */
@Component({
  selector: 'app-clients',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="page page-etroite">
      <div class="entete-page">
        <div>
          <h1>Clients</h1>
          <p class="sous-titre-page">{{ total() }} client{{ total() > 1 ? 's' : '' }}</p>
        </div>
      </div>

      <input
        class="recherche avec-loupe"
        type="search"
        [(ngModel)]="q"
        (ngModelChange)="recherche.next($event)"
        placeholder="Chercher un nom ou un téléphone"
        aria-label="Chercher un client"
        enterkeyhint="search"
      />

      <ul class="contacts">
        @for (c of clients(); track c.id) {
          <li>
            <a [routerLink]="['/clients', c.id]" class="contact">
              <span class="avatar-initiales" [style.--teinte]="teinte(c.nom)" aria-hidden="true">{{ initiales(c.nom) }}</span>
              <span class="qui">
                <strong>{{ c.nom }}</strong>
                <span class="doux">{{ c.telephone }}</span>
              </span>
              @if (c.email) {
                <span class="doux petit email masquer-mobile">{{ c.email }}</span>
              }
              <span class="fleche" aria-hidden="true"></span>
            </a>
          </li>
        } @empty {
          <li class="vide">Aucun client trouvé.</li>
        }
      </ul>

      @if (clients().length < total()) {
        <div class="ligne" style="justify-content: center; margin-top: 1rem">
          <button class="btn" (click)="plus()">Afficher plus</button>
        </div>
      }
    </div>
  `,
  styles: `
    .recherche {
      min-height: 54px;
      margin-bottom: 1rem;
      border-radius: 16px;
      font-size: 1.05rem;
    }
    .contacts {
      list-style: none;
      margin: 0;
      padding: 0.35rem;
      background: var(--surface);
      border: 1px solid var(--bordure);
      border-radius: var(--rayon-carte);
    }
    .contact {
      display: flex;
      align-items: center;
      gap: 1rem;
      min-height: 72px;
      padding: 0.7rem 1rem;
      border-radius: 16px;
      color: var(--texte);
      text-decoration: none;
    }
    .contact:hover {
      background: var(--surface-douce);
    }
    .qui {
      display: flex;
      flex-direction: column;
      flex: 1;
      min-width: 0;
    }
    .qui strong {
      font-size: 1.05rem;
      font-weight: 650;
    }
    .email {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      max-width: 40%;
    }
    .contact {
      transition: background 0.12s;
    }
    .fleche {
      width: 8px;
      height: 8px;
      border-top: 2px solid var(--texte-pale);
      border-right: 2px solid var(--texte-pale);
      transform: rotate(45deg);
      transition: transform 0.25s var(--ressort);
    }
    .contact:hover .fleche {
      transform: translateX(4px) rotate(45deg);
      border-color: var(--texte);
    }
  `,
})
export class Clients {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);

  protected readonly initiales = initiales;
  protected readonly teinte = teinte;
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
