import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NotificationService } from './core/notification.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: `
    <router-outlet />
    <div class="toasts" aria-live="polite">
      @for (t of notifications.toasts(); track t.id) {
        <div class="toast" [class.toast-erreur]="t.erreur" (click)="notifications.fermer(t.id)">
          {{ t.texte }}
        </div>
      }
    </div>
  `,
  styles: `
    .toasts {
      position: fixed;
      left: 50%;
      bottom: 5.5rem;
      transform: translateX(-50%);
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      width: min(480px, calc(100vw - 1.5rem));
      z-index: 1000;
    }
    .toast {
      background: #1f2a36;
      color: #fff;
      padding: 0.8rem 1rem;
      border-radius: 8px;
      box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
      cursor: pointer;
    }
    .toast-erreur {
      background: #9b1c12;
    }
  `,
})
export class App {
  protected readonly notifications = inject(NotificationService);
}
