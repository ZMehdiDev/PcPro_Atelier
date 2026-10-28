import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { appareil, initiales, teinte } from '../core/libelles';
import { DossierResume } from '../core/models';
import { DatePipe, DepuisPipe } from '../core/formats';
import { Statut } from './statut';

/** Liste de réparations : une ligne haute par dossier, cliquable en entier. */
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
  /** Lignes grises le temps du chargement, plutôt qu'une page vide. */
  readonly chargement = input(false);

  protected readonly appareil = appareil;
  protected readonly initiales = initiales;
  protected readonly teinte = teinte;
}
