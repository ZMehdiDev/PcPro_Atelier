import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { DatePipe, EurosPipe } from '../../core/formats';
import { STATUT_DEVIS, TYPE_LIGNE } from '../../core/libelles';
import { DossierDetail, LigneDevis, TypeLigneDevis } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { ouvrirPdf } from '../../core/fichiers';
import { Dialogue } from '../../shared/dialogue';

interface LigneSaisie {
  type: TypeLigneDevis;
  libelle: string;
  quantite: number | null;
  prixUnitaire: number | null;
}

/** Devis (prix TTC) : saisie, envoi au client, réponse du client. */
@Component({
  selector: 'app-devis-carte',
  imports: [FormsModule, EurosPipe, DatePipe, Dialogue],
  templateUrl: './devis-carte.html',
})
export class DevisCarte {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);

  readonly dossier = input.required<DossierDetail>();
  readonly modifie = output<void>();

  protected readonly libellesType = TYPE_LIGNE;
  protected readonly types = Object.keys(TYPE_LIGNE) as TypeLigneDevis[];
  protected readonly libellesStatut = STATUT_DEVIS;

  protected readonly edition = signal(false);
  protected readonly envoi = signal(false);
  protected readonly lignes = signal<LigneSaisie[]>([]);
  protected commentaire = '';
  protected readonly refus = signal(false);
  protected commentaireRefus = '';

  protected readonly devis = computed(() => this.dossier().devis);
  protected readonly modifiable = computed(
    () => this.dossier().statut !== 'CLOTURE' && this.devis()?.statut !== 'ACCEPTE',
  );

  protected total(): number {
    return this.lignes().reduce((t, l) => t + (l.quantite ?? 1) * (l.prixUnitaire ?? 0), 0);
  }

  protected ouvrirEdition(): void {
    const d = this.devis();
    if (d) {
      this.lignes.set(d.lignes.map((l) => ({ ...l })));
      this.commentaire = d.commentaire ?? '';
    } else {
      // Les pièces du dossier sont proposées comme lignes de départ
      const pieces = this.dossier()
        .pieces.filter((p) => p.etat !== 'ANNULEE')
        .map((p): LigneSaisie => ({ type: 'PIECE', libelle: p.reference, quantite: 1, prixUnitaire: null }));
      this.lignes.set([...pieces, { type: 'MAIN_OEUVRE', libelle: 'Main-d’œuvre', quantite: 1, prixUnitaire: null }]);
      this.commentaire = '';
    }
    this.edition.set(true);
  }

  protected ajouterLigne(type: TypeLigneDevis): void {
    const libelle = type === 'MAIN_OEUVRE' ? 'Main-d’œuvre' : '';
    this.lignes.update((l) => [...l, { type, libelle, quantite: 1, prixUnitaire: null }]);
  }

  protected retirerLigne(index: number): void {
    this.lignes.update((l) => l.filter((_, i) => i !== index));
  }

  protected enregistrer(): void {
    const lignes = this.lignes().filter((l) => l.libelle.trim() || l.prixUnitaire);
    if (!lignes.length) {
      this.notifications.erreur('Ajoutez au moins une ligne.');
      return;
    }
    if (lignes.some((l) => !l.libelle.trim() || l.prixUnitaire === null)) {
      this.notifications.erreur('Chaque ligne doit avoir un libellé et un prix.');
      return;
    }
    const requete: LigneDevis[] = lignes.map((l) => ({
      type: l.type,
      libelle: l.libelle.trim(),
      quantite: l.quantite ?? 1,
      prixUnitaire: l.prixUnitaire!,
    }));
    this.executer(
      this.api.enregistrerDevis(this.dossier().id, requete, this.commentaire.trim() || null),
      'Devis enregistré, le dossier attend la réponse du client',
      () => this.edition.set(false),
    );
  }

  protected accepter(): void {
    this.executer(this.api.deciderDevis(this.dossier().id, true, null), 'Devis accepté, place à la réparation');
  }

  protected refuser(): void {
    this.executer(
      this.api.deciderDevis(this.dossier().id, false, this.commentaireRefus.trim() || null),
      'Refus enregistré',
      () => this.refus.set(false),
    );
  }

  protected envoyer(): void {
    this.executer(this.api.envoyerDevis(this.dossier().id), `Devis envoyé à ${this.dossier().client.email}`);
  }

  /** Devis en PDF, à imprimer ou à remettre au client. */
  protected pdf(): void {
    const d = this.dossier();
    ouvrirPdf(this.api.devisPdf(d.id), `devis-${d.reference}.pdf`, (err) => this.notifications.erreur(err));
  }

  protected supprimer(): void {
    if (confirm('Supprimer ce devis ?')) {
      this.executer(this.api.supprimerDevis(this.dossier().id), 'Devis supprimé');
    }
  }

  private executer(appel: Observable<unknown>, message: string, ensuite?: () => void): void {
    this.envoi.set(true);
    appel.subscribe({
      next: () => {
        this.envoi.set(false);
        this.notifications.ok(message);
        ensuite?.();
        this.modifie.emit();
      },
      error: (err) => {
        this.envoi.set(false);
        this.notifications.erreur(err);
      },
    });
  }
}
