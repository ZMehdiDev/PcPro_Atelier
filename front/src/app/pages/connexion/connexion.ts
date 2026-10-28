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
      <!-- Panneau de marque (ordinateur) -->
      <aside class="marque-panneau">
        <div class="logo">
          <span class="marque" aria-hidden="true">PC</span>
          <span>Pc Pro <em>Atelier</em></span>
        </div>
        <div class="accroche">
          <h1>Chaque appareil, suivi du dépôt jusqu'au retour.</h1>
          <p>Réparations, devis, caisse et suivi client, au même endroit. À l'atelier, à la maison ou sur le téléphone.</p>
        </div>
        <p class="pied">Outil interne Pc Pro</p>
      </aside>

      <main class="cote-formulaire">
        <form (ngSubmit)="valider()">
          <div class="logo logo-form">
            <span class="marque" aria-hidden="true">PC</span>
            <span>Pc Pro <em>Atelier</em></span>
          </div>
          <h2>Connexion</h2>
          <p class="doux">Bon retour parmi nous.</p>
          @if (erreur()) {
            <div class="bandeau bandeau-danger">{{ erreur() }}</div>
          }
          <div class="pile">
            <label class="champ">
              <span>Identifiant</span>
              <input name="login" [(ngModel)]="login" autocomplete="username" autocapitalize="none" required autofocus />
            </label>
            <label class="champ">
              <span>Mot de passe</span>
              <span class="mot-de-passe">
                <input
                  name="motDePasse"
                  [type]="voir() ? 'text' : 'password'"
                  [(ngModel)]="motDePasse"
                  autocomplete="current-password"
                  required
                />
                <button type="button" class="voir" (click)="voir.set(!voir())" [attr.aria-label]="voir() ? 'Cacher le mot de passe' : 'Afficher le mot de passe'">
                  {{ voir() ? 'Cacher' : 'Afficher' }}
                </button>
              </span>
            </label>
            <button class="btn btn-principal btn-grand btn-bloc" [disabled]="envoi() || !login || !motDePasse">
              {{ envoi() ? 'Connexion…' : 'Se connecter' }}
            </button>
          </div>
          <p class="doux petit aide">Vous restez connecté 30 jours sur cet appareil.</p>
        </form>
      </main>
    </div>
  `,
  styles: `
    .connexion {
      min-height: 100dvh;
      display: grid;
      grid-template-columns: minmax(380px, 46%) 1fr;
    }
    .marque-panneau {
      position: relative;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      padding: 2.5rem 3rem;
      overflow: hidden;
      color: #fff;
      background:
        radial-gradient(110% 90% at 100% 0%, rgba(120, 170, 255, 0.4) 0%, transparent 55%),
        radial-gradient(90% 80% at 0% 100%, rgba(5, 15, 35, 0.6) 0%, transparent 60%),
        linear-gradient(150deg, #12346b 0%, #1d5fc9 100%);
    }
    .marque-panneau::after {
      content: '';
      position: absolute;
      right: -180px;
      bottom: -220px;
      width: 560px;
      height: 560px;
      border-radius: 50%;
      border: 1px solid rgba(255, 255, 255, 0.14);
      box-shadow:
        0 0 0 70px rgba(255, 255, 255, 0.03),
        0 0 0 140px rgba(255, 255, 255, 0.025);
    }
    .logo {
      display: flex;
      align-items: center;
      gap: 0.7rem;
      font-weight: 750;
      font-size: 1.15rem;
      letter-spacing: -0.02em;
    }
    .logo em {
      font-family: var(--serif);
      font-weight: 500;
      opacity: 0.8;
    }
    .marque {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 40px;
      height: 40px;
      border-radius: 12px;
      background: rgba(255, 255, 255, 0.16);
      box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.25);
      color: #fff;
      font-size: 0.85rem;
      font-weight: 800;
    }
    .accroche {
      position: relative;
      z-index: 1;
      max-width: 30rem;
    }
    .accroche h1 {
      font-size: 2.9rem;
      line-height: 1.08;
      margin-bottom: 1rem;
    }
    .accroche p {
      font-size: 1.08rem;
      color: rgba(255, 255, 255, 0.8);
    }
    .pied {
      margin: 0;
      font-size: 0.85rem;
      color: rgba(255, 255, 255, 0.6);
    }
    .cote-formulaire {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2rem 1.25rem;
    }
    form {
      width: min(380px, 100%);
      animation: entree 0.45s var(--ressort) both;
    }
    form h2 {
      font-family: var(--serif);
      font-size: 2.2rem;
      font-weight: 500;
      margin-bottom: 0.25rem;
    }
    form > .doux {
      margin-bottom: 1.75rem;
    }
    .mot-de-passe {
      position: relative;
      display: block;
    }
    .mot-de-passe input {
      padding-right: 5.5rem;
    }
    .voir {
      position: absolute;
      right: 0.4rem;
      top: 50%;
      transform: translateY(-50%);
      padding: 0.4rem 0.7rem;
      border: 0;
      border-radius: 8px;
      background: transparent;
      color: var(--accent);
      font: inherit;
      font-size: 0.85rem;
      font-weight: 600;
      cursor: pointer;
    }
    .voir:hover {
      background: var(--accent-clair);
    }
    .logo-form {
      display: none;
    }
    .aide {
      margin: 1.25rem 0 0;
      text-align: center;
    }
    @media (max-width: 860px) {
      .connexion {
        grid-template-columns: 1fr;
      }
      .marque-panneau {
        display: none;
      }
      form .logo {
        display: flex;
        margin-bottom: 2.5rem;
        color: var(--texte);
      }
      form .marque {
        background: linear-gradient(145deg, #2a6ee0, #163f86);
      }
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
  protected readonly voir = signal(false);
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
