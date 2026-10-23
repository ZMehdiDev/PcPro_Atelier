import { Component, input } from '@angular/core';
import { STATUT } from '../core/libelles';
import { StatutDossier } from '../core/models';

@Component({
  selector: 'app-statut',
  template: `<span class="badge badge-{{ statut() }}">{{ libelles[statut()] }}</span>`,
})
export class Statut {
  readonly statut = input.required<StatutDossier>();
  protected readonly libelles = STATUT;
}
