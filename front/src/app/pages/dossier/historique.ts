import { Component, input } from '@angular/core';
import { DatePipe } from '../../core/formats';
import { EVENEMENT, traduireValeur } from '../../core/libelles';
import { Evenement } from '../../core/models';

/** Qui a fait quoi sur le dossier, replié par défaut. */
@Component({
  selector: 'app-historique',
  imports: [DatePipe],
  template: `
    <section class="carte">
      <details>
        <summary class="gras">Historique ({{ evenements().length }})</summary>
        <ul class="liste" style="margin-top: 0.5rem">
          @for (e of evenements(); track e.id) {
            <li class="petit">
              <div class="doux">{{ e.createdAt | date: true }} · {{ e.employePrenom ?? 'Client (en ligne)' }}</div>
              <div>
                <strong>{{ libelles[e.typeAction] ?? e.typeAction }}</strong>
                @if (e.ancienneValeur || e.nouvelleValeur) {
                  : {{ traduire(e.ancienneValeur) }}
                  @if (e.ancienneValeur && e.nouvelleValeur) {
                    →
                  }
                  {{ traduire(e.nouvelleValeur) }}
                }
              </div>
              @if (e.commentaire) {
                <div class="doux pre">{{ e.commentaire }}</div>
              }
            </li>
          }
        </ul>
      </details>
    </section>
  `,
  styles: `
    summary {
      cursor: pointer;
      min-height: 32px;
      display: list-item;
    }
  `,
})
export class Historique {
  readonly evenements = input.required<Evenement[]>();
  protected readonly libelles = EVENEMENT;
  protected readonly traduire = traduireValeur;
}
