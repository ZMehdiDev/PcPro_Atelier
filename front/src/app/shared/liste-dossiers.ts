import { Component, inject, input } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { appareil, PRIORITE } from '../core/libelles';
import { DossierResume } from '../core/models';
import { DatePipe, DepuisPipe } from '../core/formats';
import { Statut } from './statut';

/** Liste de dossiers : tableau sur ordinateur, cartes sur téléphone. */
@Component({
  selector: 'app-liste-dossiers',
  imports: [RouterLink, Statut, DatePipe, DepuisPipe],
  templateUrl: './liste-dossiers.html',
  styleUrl: './liste-dossiers.css',
})
export class ListeDossiers {
  readonly dossiers = input.required<DossierResume[]>();
  readonly masquerClient = input(false);
  readonly vide = input('Aucun dossier.');
  /** Cartes même sur ordinateur (colonnes étroites). */
  readonly compact = input(false);

  protected readonly router = inject(Router);
  protected readonly appareil = appareil;
  protected readonly priorite = PRIORITE;
}
