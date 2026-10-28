import { Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { switchMap, tap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { ouvrirPdf } from '../../core/fichiers';
import { NotificationService } from '../../core/notification.service';
import { Dialogue } from '../../shared/dialogue';

/**
 * Bouton « Facture » d'une vente au comptoir : demande le nom (et l'adresse) du client, émet la facture et
 * l'ouvre pour l'imprimer. Si la vente est déjà facturée, rouvre simplement sa facture.
 */
@Component({
  selector: 'app-facture-vente',
  imports: [FormsModule, Dialogue],
  template: `
    <button type="button" class="btn btn-petit" [class.btn-lien]="discret()" [disabled]="envoi()" (click)="ouvrir()">
      Facture
    </button>

    <app-dialogue titre="Facture de la vente" [ouvert]="dialogue()" (fermer)="dialogue.set(false)">
      <div class="pile">
        <label class="champ">
          <span>Au nom de</span>
          <input [(ngModel)]="nom" placeholder="Nom du client ou de la société" autocomplete="off" />
        </label>
        <label class="champ">
          <span>Adresse</span>
          <input [(ngModel)]="adresse" placeholder="Rue, numéro, code postal, commune" autocomplete="off" />
        </label>
        <label class="champ">
          <span>N° de TVA (client professionnel, facultatif)</span>
          <input [(ngModel)]="tva" placeholder="BE 0123.456.789" autocomplete="off" />
        </label>
        <label class="champ">
          <span>Email (facultatif)</span>
          <input type="email" [(ngModel)]="email" autocomplete="off" />
        </label>
        <p class="doux petit">Une facture émise ne se modifie plus : vérifiez le nom et l'adresse.</p>
      </div>
      <div class="dialogue-actions">
        <button class="btn btn-neutre" (click)="dialogue.set(false)">Annuler</button>
        <button class="btn btn-principal" [disabled]="envoi() || !nom.trim()" (click)="facturer()">Émettre et imprimer</button>
      </div>
    </app-dialogue>
  `,
})
export class FactureVente {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);

  readonly venteId = input.required<number>();
  /** Bouton en lien (dans un tableau) plutôt qu'en bouton. */
  readonly discret = input(false);

  protected readonly dialogue = signal(false);
  protected readonly envoi = signal(false);
  protected nom = '';
  protected adresse = '';
  protected tva = '';
  protected email = '';

  protected ouvrir(): void {
    this.envoi.set(true);
    this.api.factureDeLaVente(this.venteId()).subscribe({
      next: (facture) => {
        this.envoi.set(false);
        if (facture) {
          // Déjà facturée : on rouvre la facture (pas de doublon)
          ouvrirPdf(this.api.facturePdf(facture.id), `facture-${facture.numero}.pdf`, (err) => this.notifications.erreur(err));
        } else {
          this.nom = this.adresse = this.tva = this.email = '';
          this.dialogue.set(true);
        }
      },
      error: (err) => {
        this.envoi.set(false);
        this.notifications.erreur(err);
      },
    });
  }

  protected facturer(): void {
    this.envoi.set(true);
    const demande = this.api
      .facturerVente(this.venteId(), {
        clientNom: this.nom.trim(),
        clientAdresse: this.adresse.trim() || null,
        clientTva: this.tva.trim() || null,
        clientEmail: this.email.trim() || null,
      })
      .pipe(
        tap((f) => {
          this.envoi.set(false);
          this.dialogue.set(false);
          this.notifications.ok(`Facture ${f.numero} émise`);
        }),
        switchMap((f) => this.api.facturePdf(f.id)),
      );
    ouvrirPdf(demande, 'facture.pdf', (err) => {
      this.envoi.set(false);
      this.notifications.erreur(err);
    });
  }
}
