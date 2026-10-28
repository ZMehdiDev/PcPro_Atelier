import { Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { messageErreur } from '../../core/erreurs';

@Component({
  selector: 'app-connexion',
  imports: [FormsModule],
  template: `
    <div class="connexion">
      <form class="carte" (ngSubmit)="valider()">
        <h1>Pc Pro <span>Atelier</span></h1>
        <p class="doux">Connectez-vous pour accéder aux dossiers.</p>
        @if (erreur()) {
          <div class="bandeau bandeau-danger">{{ erreur() }}</div>
        }
        <div class="pile">
          <label class="champ">
            <span>Identifiant</span>
            <input name="login" [(ngModel)]="login" autocomplete="username" autocapitalize="none" required />
          </label>
          <label class="champ">
            <span>Mot de passe</span>
            <input name="motDePasse" type="password" [(ngModel)]="motDePasse" autocomplete="current-password" required />
          </label>
          <button class="btn btn-principal btn-grand btn-bloc" [disabled]="envoi() || !login || !motDePasse">
            {{ envoi() ? 'Connexion…' : 'Se connecter' }}
          </button>
        </div>
        <p class="doux petit aide">Vous restez connecté 30 jours sur cet appareil.</p>
      </form>
    </div>
  `,
  styles: `
    .connexion {
      min-height: 100dvh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem;
    }
    form {
      width: min(400px, 100%);
      padding: 1.75rem 1.5rem;
    }
    h1 span {
      color: var(--accent);
    }
    .aide {
      margin: 1rem 0 0;
      text-align: center;
    }
  `,
})
export class Connexion {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly retour = input<string>();

  protected login = '';
  protected motDePasse = '';
  protected readonly envoi = signal(false);
  protected readonly erreur = signal<string | null>(null);

  constructor() {
    if (this.auth.connecte()) {
      this.router.navigateByUrl('/');
    }
  }

  protected valider(): void {
    this.envoi.set(true);
    this.erreur.set(null);
    this.auth.login(this.login.trim(), this.motDePasse).subscribe({
      next: () => this.router.navigateByUrl(this.retour()?.startsWith('/') ? this.retour()! : '/'),
      error: (err) => {
        this.envoi.set(false);
        this.erreur.set(messageErreur(err));
      },
    });
  }
}
