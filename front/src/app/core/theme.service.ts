import { Injectable, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

const CLE = 'atelier.theme';

/**
 * Mode clair ou sombre. Le choix est retenu sur l'appareil ; sans choix, on suit le réglage
 * du téléphone ou de l'ordinateur. index.html l'applique avant l'affichage pour éviter un flash.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<Theme>(document.documentElement.dataset['theme'] === 'dark' ? 'dark' : 'light');

  /** Bascule, avec le nouveau thème qui s'ouvre en cercle depuis le bouton cliqué. */
  basculer(depuis?: MouseEvent): void {
    const suivant: Theme = this.theme() === 'dark' ? 'light' : 'dark';
    const appliquer = () => this.appliquer(suivant);
    const doc = document as Document & { startViewTransition?: (f: () => void) => { ready: Promise<void> } };
    const reduit = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (!doc.startViewTransition || reduit) {
      appliquer();
      return;
    }
    const x = depuis?.clientX ?? window.innerWidth / 2;
    const y = depuis?.clientY ?? 0;
    const rayon = Math.hypot(Math.max(x, window.innerWidth - x), Math.max(y, window.innerHeight - y));
    doc.startViewTransition(appliquer).ready.then(() => {
      document.documentElement.animate(
        { clipPath: [`circle(0px at ${x}px ${y}px)`, `circle(${rayon}px at ${x}px ${y}px)`] },
        { duration: 450, easing: 'cubic-bezier(0.22, 1, 0.36, 1)', pseudoElement: '::view-transition-new(root)' },
      );
    });
  }

  private appliquer(theme: Theme): void {
    this.theme.set(theme);
    document.documentElement.dataset['theme'] = theme;
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', theme === 'dark' ? '#161513' : '#f5f4ef');
    try {
      localStorage.setItem(CLE, theme);
    } catch {
      // Choix non retenu (navigation privée) : sans importance.
    }
  }
}
