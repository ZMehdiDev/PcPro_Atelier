import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ConfirmationService } from './core/confirmation.service';
import { NotificationService } from './core/notification.service';
import { Dialogue } from './shared/dialogue';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Dialogue],
  template: `
    <router-outlet />

    <div class="toasts" aria-live="polite">
      @for (t of notifications.toasts(); track t.id) {
        <div class="toast" [class.toast-erreur]="t.erreur" (click)="notifications.fermer(t.id)" role="status">
          <span class="pastille" aria-hidden="true">
            @if (t.erreur) {
              <svg viewBox="0 0 24 24"><path d="M12 7v6M12 17h.01" /></svg>
            } @else {
              <svg viewBox="0 0 24 24"><path d="M6 12.5l4 4L18 8.5" /></svg>
            }
          </span>
          <span>{{ t.texte }}</span>
        </div>
      }
    </div>

    <app-dialogue
      [titre]="confirmation.demande()?.titre ?? ''"
      [ouvert]="!!confirmation.demande()"
      (fermer)="confirmation.repondre(false)"
    >
      @if (confirmation.demande(); as d) {
        @if (d.message) {
          <p class="doux">{{ d.message }}</p>
        }
        <div class="dialogue-actions">
          <button class="btn btn-neutre" (click)="confirmation.repondre(false)">Retour</button>
          <button class="btn" [class.btn-danger]="d.danger" [class.btn-principal]="!d.danger" (click)="confirmation.repondre(true)">
            {{ d.bouton }}
          </button>
        </div>
      }
    </app-dialogue>
  `,
  styles: `
    .toasts {
      position: fixed;
      left: 50%;
      bottom: 1.5rem;
      transform: translateX(-50%);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;
      width: min(480px, calc(100vw - 1.5rem));
      z-index: 1000;
      pointer-events: none;
    }
    .toast {
      display: flex;
      align-items: center;
      gap: 0.7rem;
      max-width: 100%;
      padding: 0.7rem 1.15rem 0.7rem 0.75rem;
      border-radius: 999px;
      background: var(--noir);
      color: var(--sur-noir);
      font-weight: 550;
      box-shadow: 0 12px 32px rgba(0, 0, 0, 0.22);
      cursor: pointer;
      pointer-events: auto;
      animation: toast 0.38s var(--ressort);
    }
    .pastille {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex: none;
      width: 26px;
      height: 26px;
      border-radius: 50%;
      background: var(--ok);
      color: #fff;
    }
    .toast-erreur .pastille {
      background: var(--danger);
    }
    .pastille svg {
      width: 16px;
      height: 16px;
      fill: none;
      stroke: currentColor;
      stroke-width: 2.6;
      stroke-linecap: round;
      stroke-linejoin: round;
    }
    @keyframes toast {
      from {
        opacity: 0;
        transform: translateY(16px) scale(0.94);
      }
    }
    @media (max-width: 720px) {
      .toasts {
        bottom: calc(5.5rem + env(safe-area-inset-bottom));
      }
      .toast {
        border-radius: 18px;
      }
    }
  `,
})
export class App {
  protected readonly notifications = inject(NotificationService);
  protected readonly confirmation = inject(ConfirmationService);
}
