import { Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { DatePipe } from '../../core/formats';
import { DossierDetail } from '../../core/models';
import { NotificationService } from '../../core/notification.service';

/** Notes internes. Une note sensible (mot de passe du client...) reste masquée jusqu'au clic. */
@Component({
  selector: 'app-notes-carte',
  imports: [FormsModule, DatePipe],
  template: `
    <section class="carte">
      <h2>Notes</h2>
      <form (ngSubmit)="ajouter()" class="pile">
        <textarea name="contenu" [(ngModel)]="contenu" rows="2" placeholder="Ajouter une note (diagnostic, appel au client…)"></textarea>
        <div class="ligne ligne-espace">
          <label class="case petit">
            <input type="checkbox" name="sensible" [(ngModel)]="sensible" />
            Note sensible (mot de passe…), masquée
          </label>
          <button class="btn btn-principal btn-petit" [disabled]="envoi() || !contenu.trim()">Ajouter</button>
        </div>
      </form>
      @if (dossier().notes.length) {
        <ul class="liste" style="margin-top: 0.75rem">
          @for (n of dossier().notes; track n.id) {
            <li>
              <div class="doux petit">{{ n.auteur ?? 'Atelier' }} · {{ n.createdAt | date: true }}</div>
              @if (n.sensible && !visibles().has(n.id)) {
                <button class="btn btn-petit" (click)="afficher(n.id)">Note sensible : afficher</button>
              } @else {
                <div class="pre">{{ n.contenu }}</div>
              }
            </li>
          }
        </ul>
      }
    </section>
  `,
})
export class NotesCarte {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);

  readonly dossier = input.required<DossierDetail>();
  readonly modifie = output<void>();

  protected contenu = '';
  protected sensible = false;
  protected readonly envoi = signal(false);
  protected readonly visibles = signal(new Set<number>());

  protected afficher(id: number): void {
    this.visibles.update((v) => new Set(v).add(id));
  }

  protected ajouter(): void {
    this.envoi.set(true);
    this.api.ajouterNote(this.dossier().id, this.contenu.trim(), this.sensible).subscribe({
      next: () => {
        this.envoi.set(false);
        this.contenu = '';
        this.sensible = false;
        this.modifie.emit();
      },
      error: (err) => {
        this.envoi.set(false);
        this.notifications.erreur(err);
      },
    });
  }
}
