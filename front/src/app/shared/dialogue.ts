import { Component, ElementRef, effect, input, output, viewChild } from '@angular/core';

/** Fenêtre modale (élément <dialog> natif) : Échap ou clic à côté pour fermer. */
@Component({
  selector: 'app-dialogue',
  template: `
    <dialog #dialog (close)="fermer.emit()" (click)="clicFond($event)">
      <div class="dialogue-corps">
        <h2>{{ titre() }}</h2>
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
