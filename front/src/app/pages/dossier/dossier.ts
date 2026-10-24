import { LowerCasePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ouvrirPdf } from '../../core/fichiers';
import { DatePipe, DepuisPipe, EurosPipe, aujourdhui } from '../../core/formats';
import { ETAPE_SUIVANTE, MOYEN_PAIEMENT, PRIORITE, STATUT_DEVIS, TYPE_APPAREIL, actionStatut } from '../../core/libelles';
import {
  DossierDetail,
  Employe,
  MoyenPaiement,
  Priorite,
  StatutDossier,
  TypeAppareil,
} from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { Dialogue } from '../../shared/dialogue';
import { DevisCarte } from './devis-carte';
import { Historique } from './historique';
import { NotesCarte } from './notes-carte';
import { PaiementsCarte } from './paiements-carte';
import { PhotosCarte } from './photos-carte';
import { PiecesCarte } from './pieces-carte';

type Onglet = 'resume' | 'argent' | 'pieces' | 'notes' | 'historique';

/** Fiche d'un dossier : tout ce qu'on fait sur un appareil, de l'entrée à la restitution. */
@Component({
  selector: 'app-dossier',
  imports: [
    FormsModule,
    RouterLink,
    Dialogue,
    DatePipe,
    DepuisPipe,
    EurosPipe,
    LowerCasePipe,
    DevisCarte,
    PaiementsCarte,
    PiecesCarte,
    NotesCarte,
    PhotosCarte,
    Historique,
  ],
  templateUrl: './dossier.html',
  styleUrl: './dossier.css',
})
export class DossierPage {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  protected readonly auth = inject(AuthService);

  readonly id = input.required<string>();
  readonly nouveau = input<string>();

  protected readonly dossier = signal<DossierDetail | null>(null);
  protected readonly techniciens = signal<Employe[]>([]);
  protected readonly envoi = signal(false);

  protected readonly libellesPriorite = PRIORITE;
  protected readonly priorites: Priorite[] = ['NORMALE', 'HAUTE', 'BASSE'];
  protected readonly libellesMoyen = MOYEN_PAIEMENT;
  protected readonly moyens = Object.keys(MOYEN_PAIEMENT) as MoyenPaiement[];
  protected readonly typesAppareil = Object.keys(TYPE_APPAREIL) as TypeAppareil[];
  protected readonly libellesType = TYPE_APPAREIL;
  protected readonly garantiesMois = [0, 1, 3, 6, 12, 24];
  protected readonly libellesDevis = STATUT_DEVIS;

  /** Les 5 grandes étapes montrées en haut de la fiche ; les attentes restent dans leur étape. */
  protected readonly parcours: { statut: StatutDossier; libelle: string }[] = [
    { statut: 'RECU', libelle: 'Reçu' },
    { statut: 'DIAGNOSTIC', libelle: 'Diagnostic' },
    { statut: 'REPARATION', libelle: 'Réparation' },
    { statut: 'PRET', libelle: 'Prêt' },
    { statut: 'CLOTURE', libelle: 'Rendu' },
  ];
  private readonly rangEtape: Record<StatutDossier, number> = {
    RECU: 0,
    DIAGNOSTIC: 1,
    ATTENTE_CLIENT: 1,
    ATTENTE_PIECE: 2,
    REPARATION: 2,
    PRET: 3,
    CLOTURE: 4,
  };
  protected readonly etapeActuelle = computed(() => {
    const d = this.dossier();
    return d ? this.rangEtape[d.statut] : 0;
  });

  protected readonly onglets: { cle: Onglet; libelle: string }[] = [
    { cle: 'resume', libelle: 'Résumé' },
    { cle: 'argent', libelle: 'Devis et paiement' },
    { cle: 'pieces', libelle: 'Pièces' },
    { cle: 'notes', libelle: 'Notes et photos' },
    { cle: 'historique', libelle: 'Historique' },
  ];
  protected readonly onglet = signal<Onglet>('resume');

  /** Étape suivante mise en avant, puis les autres changements de statut possibles. */
  protected readonly etapes = computed(() => {
    const d = this.dossier();
    if (!d) {
      return { principale: null, autres: [] as StatutDossier[] };
    }
    const possibles = d.statutsPossibles.filter((s) => d.statut !== 'CLOTURE' || this.auth.estAdmin());
    const suivante = ETAPE_SUIVANTE[d.statut];
    const principale = suivante && possibles.includes(suivante) ? suivante : null;
    return { principale, autres: possibles.filter((s) => s !== principale) };
  });

  protected readonly actionStatut = actionStatut;

  // Dialogues
  protected readonly dialogue = signal<'pret' | 'cloture' | 'infos' | 'rouvrir' | null>(null);
  protected commentaire = '';
  protected notifierClient = true;
  protected montant: number | null = null;
  protected moyen: MoyenPaiement = 'BANCONTACT';
  protected garantieMois = 3;
  protected infos = {
    type: 'PORTABLE' as TypeAppareil,
    marque: '',
    modele: '',
    numeroSerie: '',
    descriptionProbleme: '',
    accessoires: '',
    dateEstimee: '',
  };

  constructor() {
    this.api.techniciens().subscribe((t) => this.techniciens.set(t));
    effect(() => {
      this.charger(this.id());
      this.onglet.set('resume');
    });
  }

  /** Petit nombre sur l'onglet, pour voir d'un coup d'œil s'il y a quelque chose dedans. */
  protected compteOnglet(cle: Onglet, d: DossierDetail): number | null {
    switch (cle) {
      case 'pieces':
        return d.pieces.length || null;
      case 'notes':
        return d.notes.length + d.photos.length || null;
      default:
        return null;
    }
  }

  protected charger(id = this.id()): void {
    this.api.dossier(Number(id)).subscribe({
      next: (d) => this.dossier.set(d),
      error: (err) => {
        this.notifications.erreur(err);
        if (err.status === 404) {
          this.router.navigate(['/dossiers']);
        }
      },
    });
  }

  protected bonDepot(): void {
    const d = this.dossier()!;
    ouvrirPdf(this.api.bonDepot(d.id), `bon-depot-${d.reference}.pdf`, (err) => this.notifications.erreur(err));
  }

  protected fermerBandeauNouveau(): void {
    this.router.navigate([], { queryParams: {}, replaceUrl: true });
  }

  // Statut

  protected changerStatut(cible: StatutDossier): void {
    const d = this.dossier()!;
    this.commentaire = '';
    if (cible === 'PRET') {
      this.notifierClient = !!d.client.email;
      this.dialogue.set('pret');
    } else if (cible === 'CLOTURE') {
      this.montant = d.resteAPayer && d.resteAPayer > 0 ? d.resteAPayer : null;
      this.moyen = 'BANCONTACT';
      this.garantieMois = 3;
      this.dialogue.set('cloture');
    } else if (d.statut === 'CLOTURE') {
      this.dialogue.set('rouvrir');
    } else {
      this.envoyerStatut({ statut: cible });
    }
  }

  protected confirmerPret(): void {
    this.envoyerStatut({
      statut: 'PRET',
      notifierClient: this.notifierClient,
      commentaire: this.commentaire.trim() || null,
    });
  }

  protected confirmerCloture(): void {
    this.envoyerStatut({
      statut: 'CLOTURE',
      garantieMois: this.garantieMois,
      paiement: this.montant && this.montant > 0 ? { montant: this.montant, moyen: this.moyen } : null,
      commentaire: this.commentaire.trim() || null,
    });
  }

  protected confirmerReouverture(): void {
    this.envoyerStatut({ statut: 'DIAGNOSTIC', commentaire: this.commentaire.trim() || null });
  }

  private envoyerStatut(req: Parameters<ApiService['changerStatut']>[1]): void {
    this.envoi.set(true);
    this.api.changerStatut(this.dossier()!.id, req).subscribe({
      next: (r) => {
        this.envoi.set(false);
        this.dialogue.set(null);
        if (this.nouveau()) {
          this.fermerBandeauNouveau();
        }
        this.notifications.ok(
          r.statut === 'PRET' && req.notifierClient ? 'Prêt : le client va recevoir un email' : 'Statut mis à jour',
        );
        this.charger();
      },
      error: (err) => {
        this.envoi.set(false);
        this.notifications.erreur(err);
      },
    });
  }

  // Technicien, priorité, relance

  protected changerTechnicien(id: number | null): void {
    this.api.changerTechnicien(this.dossier()!.id, id).subscribe({
      next: () => {
        this.notifications.ok(id ? 'Technicien assigné' : 'Dossier désassigné');
        this.charger();
      },
      error: (err) => this.notifications.erreur(err),
    });
  }

  protected changerPriorite(p: Priorite): void {
    this.api.changerPriorite(this.dossier()!.id, p).subscribe({
      next: () => this.charger(),
      error: (err) => this.notifications.erreur(err),
    });
  }

  protected relancer(): void {
    this.api.relancer(this.dossier()!.id).subscribe({
      next: () => {
        this.notifications.ok('Rappel envoyé au client');
        this.charger();
      },
      error: (err) => this.notifications.erreur(err),
    });
  }

  protected copierLien(): void {
    const lien = this.dossier()?.lienSuivi;
    if (lien) {
      navigator.clipboard?.writeText(lien).then(
        () => this.notifications.ok('Lien copié'),
        () => this.notifications.erreur('Impossible de copier le lien'),
      );
    }
  }

  // Modification des informations

  protected modifierInfos(): void {
    const d = this.dossier()!;
    this.infos = {
      type: d.appareil.type,
      marque: d.appareil.marque ?? '',
      modele: d.appareil.modele ?? '',
      numeroSerie: d.appareil.numeroSerie ?? '',
      descriptionProbleme: d.descriptionProbleme ?? '',
      accessoires: d.accessoires ?? '',
      dateEstimee: d.dateEstimee ?? '',
    };
    this.dialogue.set('infos');
  }

  protected enregistrerInfos(): void {
    const i = this.infos;
    this.envoi.set(true);
    this.api
      .modifierDossier(this.dossier()!.id, {
        appareil: {
          type: i.type,
          marque: i.marque.trim() || null,
          modele: i.modele.trim() || null,
          numeroSerie: i.numeroSerie.trim() || null,
        },
        descriptionProbleme: i.descriptionProbleme.trim() || null,
        accessoires: i.accessoires.trim() || null,
        dateEstimee: i.dateEstimee || null,
      })
      .subscribe({
        next: () => {
          this.envoi.set(false);
          this.dialogue.set(null);
          this.notifications.ok('Dossier modifié');
          this.charger();
        },
        error: (err) => {
          this.envoi.set(false);
          this.notifications.erreur(err);
        },
      });
  }

  protected readonly aujourdhui = aujourdhui();
}
