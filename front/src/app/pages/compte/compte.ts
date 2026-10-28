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
        <div class="ligne profil">
          <span class="avatar">{{ auth.employe()?.prenom?.charAt(0) }}</span>
          <div>
            <h1>{{ auth.employe()?.prenom }}</h1>
            <span class="badge" [class.badge-ok]="auth.estAdmin()">{{ auth.estAdmin() ? 'Admin' : 'Technicien' }}</span>
          </div>
        </div>
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
      padding: 1rem 1.25rem;
      border-bottom: 1px solid var(--bordure);
      color: var(--texte);
      text-decoration: none;
      font-weight: 500;
    }
    .menu a:last-child {
      border-bottom: 0;
    }
    .menu a {
      display: flex;
      justify-content: space-between;
      align-items: center;
      transition: background 0.12s;
    }
    .menu a:hover {
      background: var(--surface-douce);
    }
    .menu a::after {
      content: '';
      width: 8px;
      height: 8px;
      border-top: 2px solid var(--texte-pale);
      border-right: 2px solid var(--texte-pale);
      transform: rotate(45deg);
    }
    .menu {
      overflow: hidden;
    }
    .profil {
      gap: 1rem;
    }
    .profil h1 {
      margin: 0 0 0.2rem;
    }
    .avatar {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 64px;
      height: 64px;
      border-radius: 50%;
      background: var(--noir);
      color: var(--sur-noir);
      font-size: 1.6rem;
      font-weight: 700;
      text-transform: uppercase;
    }
  `,
})
export class Compte {
  protected readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  protected ancien = '';
  protected nouveau = '';
  protected readonly envoi = signal(false);

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
