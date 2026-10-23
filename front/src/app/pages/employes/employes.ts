import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Employe, Role } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { Dialogue } from '../../shared/dialogue';

/** Comptes des employés (admin). Un admin voit la caisse, annule un paiement, rouvre un dossier. */
@Component({
  selector: 'app-employes',
  imports: [FormsModule, Dialogue],
  template: `
    <div class="page page-etroite">
      <div class="entete-page">
        <h1>Employés</h1>
        <button class="btn btn-principal" (click)="nouveau()">+ Ajouter</button>
      </div>
      <div class="carte" style="padding: 0 1rem">
        <ul class="liste">
          @for (e of employes(); track e.id) {
            <li class="ligne ligne-espace">
              <span [class.doux]="!e.actif">
                <strong>{{ e.prenom }}</strong>
                <span class="doux"> · {{ e.login }}</span><br />
                <span class="badge" [class.badge-ok]="e.role === 'ADMIN'">{{ e.role === 'ADMIN' ? 'Admin' : 'Technicien' }}</span>
                @if (!e.actif) {
                  <span class="badge">Désactivé</span>
                }
                @if (!e.motDePasseDefini) {
                  <span class="badge badge-alerte">Sans mot de passe</span>
                }
              </span>
              <span class="ligne">
                <button class="btn btn-petit" (click)="modifier(e)">Modifier</button>
                <button class="btn btn-petit" (click)="motDePasse(e)">Mot de passe</button>
              </span>
            </li>
          }
        </ul>
      </div>
      <p class="doux petit">
        Un admin voit la caisse, peut annuler un paiement, rouvrir un dossier rendu et gérer les comptes.
      </p>
    </div>

    <app-dialogue [titre]="edite() ? 'Modifier ' + edite()!.prenom : 'Nouvel employé'" [ouvert]="dialogue() === 'fiche'" (fermer)="dialogue.set(null)">
      <div class="pile">
        <label class="champ"><span>Prénom</span><input [(ngModel)]="prenom" /></label>
        @if (!edite()) {
          <label class="champ">
            <span>Identifiant de connexion</span>
            <input [(ngModel)]="login" autocapitalize="none" />
          </label>
          <label class="champ">
            <span>Mot de passe (8 caractères minimum)</span>
            <input type="text" [(ngModel)]="mdp" autocomplete="new-password" />
          </label>
        }
        <div class="choix">
          <label><input type="radio" name="role" value="TECHNICIEN" [(ngModel)]="role" /> Technicien</label>
          <label><input type="radio" name="role" value="ADMIN" [(ngModel)]="role" /> Admin</label>
        </div>
        @if (edite()) {
          <label class="case">
            <input type="checkbox" [(ngModel)]="actif" [disabled]="edite()!.id === auth.employe()?.id" />
            Compte actif (décocher pour bloquer l'accès)
          </label>
        }
      </div>
      <div class="dialogue-actions">
        <button class="btn" (click)="dialogue.set(null)">Annuler</button>
        <button class="btn btn-principal" [disabled]="envoi()" (click)="enregistrer()">Enregistrer</button>
      </div>
    </app-dialogue>

    <app-dialogue titre="Nouveau mot de passe" [ouvert]="dialogue() === 'mdp'" (fermer)="dialogue.set(null)">
      <p>Pour {{ edite()?.prenom }}. Il sera déconnecté de tous ses appareils.</p>
      <label class="champ">
        <span>Mot de passe (8 caractères minimum)</span>
        <input type="text" [(ngModel)]="mdp" autocomplete="new-password" />
      </label>
      <div class="dialogue-actions">
        <button class="btn" (click)="dialogue.set(null)">Annuler</button>
        <button class="btn btn-principal" [disabled]="envoi() || mdp.length < 8" (click)="enregistrerMotDePasse()">Enregistrer</button>
      </div>
    </app-dialogue>
  `,
})
export class Employes {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);
  protected readonly auth = inject(AuthService);

  protected readonly employes = signal<Employe[]>([]);
  protected readonly dialogue = signal<'fiche' | 'mdp' | null>(null);
  protected readonly edite = signal<Employe | null>(null);
  protected readonly envoi = signal(false);
  protected prenom = '';
  protected login = '';
  protected mdp = '';
  protected role: Role = 'TECHNICIEN';
  protected actif = true;

  constructor() {
    this.charger();
  }

  private charger(): void {
    this.api.employes().subscribe({
      next: (e) => this.employes.set(e),
      error: (err) => this.notifications.erreur(err),
    });
  }

  protected nouveau(): void {
    this.edite.set(null);
    this.prenom = '';
    this.login = '';
    this.mdp = '';
    this.role = 'TECHNICIEN';
    this.dialogue.set('fiche');
  }

  protected modifier(e: Employe): void {
    this.edite.set(e);
    this.prenom = e.prenom;
    this.role = e.role;
    this.actif = e.actif;
    this.dialogue.set('fiche');
  }

  protected motDePasse(e: Employe): void {
    this.edite.set(e);
    this.mdp = '';
    this.dialogue.set('mdp');
  }

  protected enregistrer(): void {
    const e = this.edite();
    const appel = e
      ? this.api.modifierEmploye(e.id, { prenom: this.prenom.trim(), role: this.role, actif: this.actif })
      : this.api.creerEmploye({ prenom: this.prenom.trim(), login: this.login.trim(), motDePasse: this.mdp, role: this.role });
    this.executer(appel, e ? 'Employé modifié' : 'Employé ajouté');
  }

  protected enregistrerMotDePasse(): void {
    this.executer(this.api.reinitialiserMotDePasse(this.edite()!.id, this.mdp), 'Mot de passe changé');
  }

  private executer(appel: Observable<unknown>, message: string): void {
    this.envoi.set(true);
    appel.subscribe({
      next: () => {
        this.envoi.set(false);
        this.dialogue.set(null);
        this.notifications.ok(message);
        this.charger();
      },
      error: (err) => {
        this.envoi.set(false);
        this.notifications.erreur(err);
      },
    });
  }
}
