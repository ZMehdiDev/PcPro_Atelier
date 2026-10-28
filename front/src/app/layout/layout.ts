import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { ThemeService } from '../core/theme.service';
import { Palette, PaletteService } from '../shared/palette';

const CLE_MENU = 'atelier.menuReplie';

function lireMenuReplie(): boolean {
  try {
    return localStorage.getItem(CLE_MENU) === '1';
  } catch {
    return false;
  }
}

/** Cadre des écrans de l'atelier : menu à gauche, repliable (ordinateur), barres en haut et en bas (téléphone). */
@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Palette],
  templateUrl: './layout.html',
  styleUrl: './layout.css',
  host: { '[class.replie]': 'replie()', '(document:keydown)': 'raccourci($event)' },
})
export class Layout {
  protected readonly auth = inject(AuthService);
  protected readonly theme = inject(ThemeService);
  protected readonly sombre = computed(() => this.theme.theme() === 'dark');
  protected readonly replie = signal(lireMenuReplie());
  protected readonly palette = inject(PaletteService);

  /** Ctrl+K (ou Cmd+K) et « / » ouvrent la recherche rapide, d'où qu'on soit. */
  protected raccourci(event: KeyboardEvent): void {
    const cible = event.target as HTMLElement | null;
    const enSaisie = !!cible?.closest('input, textarea, select, [contenteditable]');
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      this.palette.basculer();
    } else if (event.key === '/' && !enSaisie && !document.querySelector('dialog[open]')) {
      event.preventDefault();
      this.palette.ouvrir();
    }
  }

  protected basculer(): void {
    this.replie.update((r) => !r);
    try {
      localStorage.setItem(CLE_MENU, this.replie() ? '1' : '0');
    } catch {
      // Préférence non retenue (navigation privée) : sans importance.
    }
  }
}
