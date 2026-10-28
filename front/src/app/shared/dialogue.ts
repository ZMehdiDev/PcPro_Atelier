import { Component, ElementRef, effect, input, output, viewChild } from '@angular/core';

/** Fenêtre modale (élément <dialog> natif) : Échap, la croix ou un clic à côté pour fermer. Sur téléphone, elle monte du bas. */
@Component({
  selector: 'app-dialogue',
  template: `
    <dialog #dialog (close)="fermer.emit()" (click)="clicFond($event)" [attr.aria-label]="titre()">
      <div class="dialogue-entete">
        <h2>{{ titre() }}</h2>
        <button type="button" class="fermer" (click)="dialog.close()" aria-label="Fermer">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" /></svg>
        </button>
      </div>
      <div class="dialogue-corps">
        <ng-content />
      </div>
    </dialog>
  `,
})
export class Dialogue {
  readonly titre = input.required<string>();
  readonly ouvert = input(false);
  readonly fermer = output<void>();

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    effect(() => {
      const d = this.dialog().nativeElement;
      if (this.ouvert() && !d.open) {
        d.showModal();
      } else if (!this.ouvert() && d.open) {
        d.close();
      }
    });
  }

  protected clicFond(event: MouseEvent): void {
    if (event.target === this.dialog().nativeElement) {
      this.dialog().nativeElement.close();
    }
  }
}
