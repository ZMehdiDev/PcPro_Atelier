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
      background: var(--noir);
      color: var(--sur-noir);
      padding: 0.85rem 1.1rem;
      border-radius: 16px;
      font-weight: 500;
      box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25);
      cursor: pointer;
      animation: toast 0.3s var(--ressort);
    }
    .toast-erreur {
      background: var(--danger);
      color: #fff;
    }
    @keyframes toast {
      from {
        opacity: 0;
        transform: translateY(16px) scale(0.96);
      }
    }
  `,
})
export class App {
  protected readonly notifications = inject(NotificationService);
}
