import { Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { EurosPipe } from '../../core/formats';
import { ACTION_PIECE, ETAT_PIECE } from '../../core/libelles';
import { DossierDetail, EtatPiece, Piece } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { Dialogue } from '../../shared/dialogue';

/**
 * Pièces du dossier. Une pièce commandée met le dossier en attente de pièce ; quand tout est reçu,
 * il repasse en réparation (fait par le backend).
 */
@Component({
  selector: 'app-pieces-carte',
  imports: [FormsModule, EurosPipe, Dialogue],
  template: `
    <section class="carte">
      <h2>
        Pièces
        @if (dossier().coutPieces > 0) {
          <span class="doux petit">Coût : {{ dossier().coutPieces | euros }}</span>
        }
      </h2>
      @if (dossier().pieces.length) {
        <ul class="liste">
          @for (p of dossier().pieces; track p.id) {
            <li>
              <div class="ligne ligne-espace">
                <span [class.barre]="p.etat === 'ANNULEE'">
                  <strong>{{ p.reference }}</strong>
                  @if (p.fournisseur) {
                    <span class="doux"> · {{ p.fournisseur }}</span>
                  }
                  @if (p.cout !== null) {
                    <span class="doux"> · {{ p.cout | euros }}</span>
                  }
                </span>
                <span class="badge" [class.badge-alerte]="p.etat === 'A_COMMANDER' || p.etat === 'COMMANDEE'" [class.badge-ok]="p.etat === 'RECUE' || p.etat === 'MONTEE'">
                  {{ etats[p.etat] }}
                </span>
              </div>
              @if (fermable() && (p.etatsPossibles.length || p.etat !== 'ANNULEE')) {
                <div class="ligne" style="margin-top: 0.4rem">
                  @for (e of p.etatsPossibles; track e) {
                    <button
                      class="btn btn-petit"
                      [class.btn-lien]="e === 'ANNULEE'"
                      [disabled]="envoi()"
                      (click)="changerEtat(p, e)"
                    >
                      {{ e === 'ANNULEE' ? 'Annuler' : '→ ' + actions[e] }}
                    </button>
                  }
                  @if (p.etat !== 'ANNULEE') {
                    <button class="btn btn-lien btn-petit" (click)="modifier(p)">Modifier</button>
                  }
                </div>
              }
            </li>
          }
        </ul>
      } @else {
        <p class="doux">Aucune pièce.</p>
      }
      @if (fermable()) {
        <button class="btn btn-petit" style="margin-top: 0.5rem" (click)="ajouter()">Ajouter une pièce</button>
      }
    </section>

    <app-dialogue [titre]="enEdition() ? 'Modifier la pièce' : 'Ajouter une pièce'" [ouvert]="dialogue()" (fermer)="dialogue.set(false)">
      <div class="pile">
        <label class="champ">
          <span>Pièce *</span>
          <input [(ngModel)]="reference" placeholder="Ex : SSD 500 Go Samsung 870" />
        </label>
        <div class="grille-2">
          <label class="champ">
            <span>Fournisseur</span>
            <input [(ngModel)]="fournisseur" />
          </label>
          <label class="champ">
            <span>Coût d'achat</span>
            <input type="number" inputmode="decimal" min="0" step="0.01" [(ngModel)]="cout" />
          </label>
        </div>
        @if (!enEdition()) {
          <div class="choix">
            <label><input type="radio" name="etatInitial" value="A_COMMANDER" [(ngModel)]="etat" /> À commander</label>
            <label><input type="radio" name="etatInitial" value="COMMANDEE" [(ngModel)]="etat" /> Déjà commandée</label>
            <label><input type="radio" name="etatInitial" value="MONTEE" [(ngModel)]="etat" /> Prise en stock</label>
          </div>
        }
      </div>
      <div class="dialogue-actions">
        <button class="btn" (click)="dialogue.set(false)">Annuler</button>
        <button class="btn btn-principal" [disabled]="envoi() || !reference.trim()" (click)="enregistrer()">Enregistrer</button>
      </div>
    </app-dialogue>
  `,
  styles: `
    .barre {
      text-decoration: line-through;
      color: var(--texte-doux);
    }
  `,
})
export class PiecesCarte {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);

  readonly dossier = input.required<DossierDetail>();
  readonly modifie = output<void>();

  protected readonly etats = ETAT_PIECE;
  protected readonly actions = ACTION_PIECE;
  protected readonly dialogue = signal(false);
  protected readonly enEdition = signal<Piece | null>(null);
  protected readonly envoi = signal(false);

  protected reference = '';
  protected fournisseur = '';
  protected cout: number | null = null;
  protected etat: EtatPiece = 'A_COMMANDER';

  protected fermable(): boolean {
    return this.dossier().statut !== 'CLOTURE';
  }

  protected ajouter(): void {
    this.enEdition.set(null);
    this.reference = '';
    this.fournisseur = '';
    this.cout = null;
    this.etat = 'A_COMMANDER';
    this.dialogue.set(true);
  }

  protected modifier(p: Piece): void {
    this.enEdition.set(p);
    this.reference = p.reference;
    this.fournisseur = p.fournisseur ?? '';
    this.cout = p.cout;
    this.dialogue.set(true);
  }

  protected enregistrer(): void {
    const req = { reference: this.reference.trim(), fournisseur: this.fournisseur.trim() || null, cout: this.cout };
    const piece = this.enEdition();
    const appel = piece
      ? this.api.modifierPiece(this.dossier().id, piece.id, req)
      : this.api.ajouterPiece(this.dossier().id, { ...req, etat: this.etat });
    this.envoi.set(true);
    appel.subscribe({
      next: () => {
        this.envoi.set(false);
        this.dialogue.set(false);
        this.modifie.emit();
      },
      error: (err) => {
        this.envoi.set(false);
        this.notifications.erreur(err);
      },
    });
  }

  protected changerEtat(p: Piece, e: EtatPiece): void {
    if (e === 'ANNULEE' && !confirm(`Annuler la pièce « ${p.reference} » ?`)) {
      return;
    }
    this.envoi.set(true);
    this.api.changerEtatPiece(this.dossier().id, p.id, e).subscribe({
      next: () => {
        this.envoi.set(false);
        this.modifie.emit();
      },
      error: (err) => {
        this.envoi.set(false);
        this.notifications.erreur(err);
      },
    });
  }
}
