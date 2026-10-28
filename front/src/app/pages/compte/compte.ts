import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { NotificationService } from '../../core/notification.service';

/** Mon compte, et sur téléphone le menu « Plus » (caisse, employés, déconnexion). */
@Component({
  selector: 'app-compte',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="page page-etroite">
      <div class="entete-page">
        <h1>{{ auth.employe()?.prenom }}</h1>
        <span class="badge">{{ auth.estAdmin() ? 'Admin' : 'Technicien' }}</span>
      </div>

      <nav class="carte menu">
        <a routerLink="/dossiers">Réparations</a>
        <a routerLink="/clients">Clients</a>
        @if (auth.estAdmin()) {
          <a routerLink="/caisse">Caisse du jour</a>
          <a routerLink="/chiffre-affaires">Chiffre d’affaires</a>
          <a routerLink="/employes">Employés</a>
        }
        <a routerLink="/suivi" target="_blank">Page de suivi des clients</a>
      </nav>

      <form class="carte pile" (ngSubmit)="changer()">
        <h2>Changer mon mot de passe</h2>
        <label class="champ">
          <span>Mot de passe actuel</span>
          <input type="password" name="ancien" [(ngModel)]="ancien" autocomplete="current-password" />
        </label>
        <label class="champ">
          <span>Nouveau mot de passe (8 caractères minimum)</span>
          <input type="password" name="nouveau" [(ngModel)]="nouveau" autocomplete="new-password" />
        </label>
        <p class="doux petit">Vos autres appareils seront déconnectés.</p>
        <button class="btn btn-principal" [disabled]="envoi() || !ancien || nouveau.length < 8">Changer</button>
      </form>

      <div class="carte pile">
        <h2>Mes appareils</h2>
        <p class="doux petit">Téléphone perdu, ordinateur prêté : seul cet appareil restera connecté.</p>
        <button class="btn btn-neutre" [disabled]="envoi()" (click)="deconnecterAutres()">
          Déconnecter mes autres appareils
        </button>
      </div>

      <button class="btn btn-danger btn-bloc" (click)="auth.logout()">Se déconnecter</button>
    </div>
  `,
  styles: `
    .menu {
      padding: 0;
      display: flex;
      flex-direction: column;
    }
    .menu a {
      padding: 0.9rem 1rem;
      border-bottom: 1px solid var(--bordure);
      color: var(--texte);
      text-decoration: none;
      font-weight: 500;
    }
    .menu a:last-child {
      border-bottom: 0;
    }
    .menu a::after {
      content: '›';
      float: right;
      color: var(--texte-doux);
    }
  `,
})
export class Compte {
  protected readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  protected ancien = '';
  protected nouveau = '';
  protected readonly envoi = signal(false);

  protected deconnecterAutres(): void {
    this.envoi.set(true);
    this.auth.deconnecterAutresAppareils().subscribe({
      next: () => {
        this.envoi.set(false);
        this.notifications.ok('Vos autres appareils sont déconnectés');
      },
      error: (err) => {
        this.envoi.set(false);
        this.notifications.erreur(err);
      },
    });
  }

  protected changer(): void {
    this.envoi.set(true);
    this.auth.changerMotDePasse(this.ancien, this.nouveau).subscribe({
      next: () => {
        this.envoi.set(false);
        this.ancien = '';
        this.nouveau = '';
        this.notifications.ok('Mot de passe changé');
      },
      error: (err) => {
        this.envoi.set(false);
        this.notifications.erreur(err);
      },
    });
  }
}
