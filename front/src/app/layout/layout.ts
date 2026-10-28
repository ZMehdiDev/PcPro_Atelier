import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { ThemeService } from '../core/theme.service';

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
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './layout.html',
  styleUrl: './layout.css',
  host: { '[class.replie]': 'replie()' },
})
export class Layout {
  protected readonly auth = inject(AuthService);
  protected readonly theme = inject(ThemeService);
  protected readonly sombre = computed(() => this.theme.theme() === 'dark');
  protected readonly replie = signal(lireMenuReplie());

  protected basculer(): void {
    this.replie.update((r) => !r);
    try {
      localStorage.setItem(CLE_MENU, this.replie() ? '1' : '0');
    } catch {
      // Préférence non retenue (navigation privée) : sans importance.
    }
  }
}
