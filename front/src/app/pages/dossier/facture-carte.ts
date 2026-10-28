import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { switchMap, tap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { DatePipe, EurosPipe } from '../../core/formats';
import { ouvrirPdf } from '../../core/fichiers';
import { DossierDetail, Facture } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { Dialogue } from '../../shared/dialogue';

/**
 * Facture de la réparation : émise d'après le devis accepté (ou le montant encaissé), elle sert aussi de bon
 * de garantie. Une erreur se corrige par une note de crédit (admin), puis une nouvelle facture.
 */
@Component({
  selector: 'app-facture-carte',
  imports: [FormsModule, EurosPipe, DatePipe, Dialogue],
  template: `
    <section class="carte">
      <h2>
        Facture
        @if (facture(); as f) {
          <span class="badge badge-ok">N° {{ f.numero }}</span>
        }
      </h2>
      @if (facture(); as f) {
        <p>
          <strong>{{ f.total | euros }}</strong> · émise le {{ f.dateEmission | date }} au nom de {{ f.clientNom }}
        </p>
        <div class="ligne" style="margin-top: 0.75rem">
          <button class="btn btn-principal btn-petit" (click)="imprimer(f)">Imprimer la facture</button>
          <button class="btn btn-petit" [disabled]="envoi()" (click)="ouvrirEnvoi(f)">Envoyer par email</button>
          @if (auth.estAdmin()) {
            <button class="btn btn-petit btn-lien" [disabled]="envoi()" (click)="annuler(f)">Annuler par une note de crédit</button>
          }
        </div>
      } @else if (charge()) {
        @if (facturable()) {
          <p class="doux">La facture reprend le devis accepté et sert aussi de bon de garantie au client.</p>
          <button class="btn" style="margin-top: 0.5rem" (click)="ouvrir()">Faire la facture</button>
        } @else {
          <p class="doux">La facture se fait une fois le devis accepté ou la réparation encaissée.</p>
        }
      }
    </section>

    <app-dialogue titre="Faire la facture" [ouvert]="dialogue()" (fermer)="dialogue.set(false)">
      <div class="pile">
        <label class="champ">
          <span>Au nom de</span>
          <input [(ngModel)]="nom" autocomplete="off" />
        </label>
        <label class="champ">
          <span>Adresse du client</span>
          <input [(ngModel)]="adresse" placeholder="Rue, numéro, code postal, commune" autocomplete="off" />
        </label>
        <label class="champ">
          <span>N° de TVA (client professionnel, facultatif)</span>
          <input [(ngModel)]="tva" placeholder="BE 0123.456.789" autocomplete="off" />
        </label>
        <p class="doux petit">Une facture émise ne se modifie plus : vérifiez le nom et l'adresse.</p>
      </div>
      <div class="dialogue-actions">
        <button class="btn btn-neutre" (click)="dialogue.set(false)">Annuler</button>
        <button class="btn btn-principal" [disabled]="envoi() || !nom.trim()" (click)="facturer()">Émettre et imprimer</button>
      </div>
    </app-dialogue>

    <app-dialogue titre="Envoyer la facture" [ouvert]="dialogueEnvoi()" (fermer)="dialogueEnvoi.set(false)">
      <label class="champ">
        <span>Email du client</span>
        <input type="email" [(ngModel)]="email" autocomplete="off" />
      </label>
      <div class="dialogue-actions">
        <button class="btn btn-neutre" (click)="dialogueEnvoi.set(false)">Annuler</button>
        <button class="btn btn-principal" [disabled]="envoi() || !email.trim()" (click)="envoyer()">Envoyer</button>
      </div>
    </app-dialogue>
  `,
})
export class FactureCarte {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);
  protected readonly auth = inject(AuthService);

  readonly dossier = input.required<DossierDetail>();
  readonly modifie = output<void>();

  protected readonly facture = signal<Facture | null>(null);
  protected readonly charge = signal(false);
  protected readonly dialogue = signal(false);
  protected readonly dialogueEnvoi = signal(false);
  protected readonly envoi = signal(false);
  protected nom = '';
  protected adresse = '';
  protected tva = '';
  protected email = '';

  protected readonly facturable = computed(
    () => this.dossier().devis?.statut === 'ACCEPTE' || this.dossier().totalPaye > 0,
  );

  constructor() {
    // Rechargée à chaque mise à jour du dossier (paiement, devis, note de crédit...)
    effect(() => {
      const id = this.dossier().id;
      this.api.factureDuDossier(id).subscribe({
        next: (f) => {
          this.facture.set(f);
          this.charge.set(true);
        },
        error: () => this.charge.set(true),
      });
    });
  }

  protected ouvrir(): void {
    this.nom = this.dossier().client.nom;
    this.adresse = '';
    this.tva = '';
    this.dialogue.set(true);
  }

  protected facturer(): void {
    const d = this.dossier();
    this.envoi.set(true);
    const demande = this.api
      .facturerDossier(d.id, {
        clientNom: this.nom.trim(),
        clientAdresse: this.adresse.trim() || null,
        clientTva: this.tva.trim() || null,
      })
      .pipe(
        tap((f) => {
          this.envoi.set(false);
          this.dialogue.set(false);
          this.facture.set(f);
          this.notifications.ok(`Facture ${f.numero} émise`);
          this.modifie.emit();
        }),
        switchMap((f) => this.api.facturePdf(f.id)),
      );
    ouvrirPdf(demande, `facture-${d.reference}.pdf`, (err) => {
      this.envoi.set(false);
      this.notifications.erreur(err);
    });
  }

  protected imprimer(f: Facture): void {
    ouvrirPdf(this.api.facturePdf(f.id), `facture-${f.numero}.pdf`, (err) => this.notifications.erreur(err));
  }

  protected ouvrirEnvoi(f: Facture): void {
    this.email = f.clientEmail ?? this.dossier().client.email ?? '';
    this.dialogueEnvoi.set(true);
  }

  protected envoyer(): void {
    const f = this.facture();
    if (!f) {
      return;
    }
    this.envoi.set(true);
    this.api.envoyerFacture(f.id, this.email.trim()).subscribe({
      next: () => {
        this.envoi.set(false);
        this.dialogueEnvoi.set(false);
        this.notifications.ok(`Facture envoyée à ${this.email.trim()}`);
        this.modifie.emit();
      },
      error: (err) => {
        this.envoi.set(false);
        this.notifications.erreur(err);
      },
    });
  }

  protected annuler(f: Facture): void {
    if (!confirm(`Annuler la facture ${f.numero} ? Une note de crédit du même montant sera émise, puis vous pourrez refaire la facture.`)) {
      return;
    }
    this.envoi.set(true);
    this.api.annulerFacture(f.id).subscribe({
      next: (note) => {
        this.envoi.set(false);
        this.facture.set(null);
        this.notifications.ok(`Note de crédit ${note.numero} émise : vous pouvez refaire la facture`);
        this.modifie.emit();
      },
      error: (err) => {
        this.envoi.set(false);
        this.notifications.erreur(err);
      },
    });
  }
}
