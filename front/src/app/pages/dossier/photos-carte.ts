import { Component, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { DatePipe } from '../../core/formats';
import { DossierDetail, Photo } from '../../core/models';
import { ConfirmationService } from '../../core/confirmation.service';
import { NotificationService } from '../../core/notification.service';
import { Dialogue } from '../../shared/dialogue';

const COTE_MAX = 2000;

/** Réduit une photo de téléphone (souvent 4 à 8 Mo) avant l'envoi : plus rapide en 4G, et sous les 10 Mo. */
async function reduire(fichier: File): Promise<Blob> {
  if (fichier.size < 1_500_000 && fichier.type === 'image/jpeg') {
    return fichier;
  }
  try {
    const image = await createImageBitmap(fichier);
    const echelle = Math.min(1, COTE_MAX / Math.max(image.width, image.height));
    const canvas = document.createElement('canvas');
    canvas.width = Math.round(image.width * echelle);
    canvas.height = Math.round(image.height * echelle);
    canvas.getContext('2d')!.drawImage(image, 0, 0, canvas.width, canvas.height);
    const blob = await new Promise<Blob | null>((ok) => canvas.toBlob(ok, 'image/jpeg', 0.85));
    return blob && blob.size < fichier.size ? blob : fichier;
  } catch {
    return fichier;
  }
}

/** Photos de l'appareil (état au dépôt, dégâts, réparation). Les images demandent le token : chargées en blob. */
@Component({
  selector: 'app-photos-carte',
  imports: [DatePipe, Dialogue],
  template: `
    <section class="carte">
      <h2>Photos</h2>
      @if (dossier().photos.length) {
        <div class="vignettes">
          @for (p of dossier().photos; track p.id) {
            <button class="vignette" (click)="agrandie.set(p)" [attr.aria-label]="p.description ?? 'Photo'">
              @if (urls()[p.id]; as url) {
                <img [src]="url" [alt]="p.description ?? 'Photo'" loading="lazy" />
              }
            </button>
          }
        </div>
      } @else {
        <p class="doux">Aucune photo.</p>
      }
      <label class="btn btn-petit" [class.desactive]="envoi()">
        {{ envoi() ? 'Envoi…' : 'Ajouter des photos' }}
        <input type="file" accept="image/jpeg,image/png,image/webp,image/*" multiple (change)="ajouter($event)" [disabled]="envoi()" hidden />
      </label>
    </section>

    <app-dialogue titre="Photo" [ouvert]="agrandie() !== null" (fermer)="agrandie.set(null)">
      @if (agrandie(); as p) {
        <img class="grande" [src]="urls()[p.id]" [alt]="p.description ?? 'Photo'" />
        <p class="doux petit">
          {{ p.description }}
          Ajoutée le {{ p.createdAt | date: true }}
        </p>
        <div class="dialogue-actions">
          <button class="btn btn-danger" (click)="supprimer(p)">Supprimer</button>
          <button class="btn btn-neutre" (click)="agrandie.set(null)">Fermer</button>
        </div>
      }
    </app-dialogue>
  `,
  styles: `
    .vignettes {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
      gap: 0.5rem;
      margin-bottom: 0.75rem;
    }
    .vignette {
      aspect-ratio: 1;
      padding: 0;
      border: 1px solid var(--bordure);
      border-radius: 6px;
      background: var(--champ);
      overflow: hidden;
      cursor: zoom-in;
    }
    .vignette img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
    .grande {
      display: block;
      width: 100%;
      max-height: 65dvh;
      object-fit: contain;
      background: var(--champ);
      border-radius: 6px;
      margin-bottom: 0.5rem;
    }
    .desactive {
      opacity: 0.55;
      pointer-events: none;
    }
  `,
})
export class PhotosCarte {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);
  private readonly confirmation = inject(ConfirmationService);

  readonly dossier = input.required<DossierDetail>();
  readonly modifie = output<void>();

  protected readonly urls = signal<Record<number, string>>({});
  protected readonly agrandie = signal<Photo | null>(null);
  protected readonly envoi = signal(false);

  private readonly enCours = new Set<number>();

  constructor() {
    inject(DestroyRef).onDestroy(() => Object.values(this.urls()).forEach((u) => URL.revokeObjectURL(u)));
    // Charge les nouvelles photos (le détail du dossier est rechargé après chaque action)
    effect(() => {
      const d = this.dossier();
      for (const p of d.photos) {
        if (!this.enCours.has(p.id)) {
          this.enCours.add(p.id);
          this.api.contenuPhoto(d.id, p.id).subscribe({
            next: (blob) => this.urls.update((u) => ({ ...u, [p.id]: URL.createObjectURL(blob) })),
            error: () => this.enCours.delete(p.id),
          });
        }
      }
    });
  }

  protected async ajouter(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const fichiers = Array.from(input.files ?? []);
    input.value = '';
    if (!fichiers.length) {
      return;
    }
    this.envoi.set(true);
    let envoyees = 0;
    for (const fichier of fichiers) {
      try {
        const image = await reduire(fichier);
        const nom = fichier.name.replace(/\.\w+$/, '') + (image === fichier ? fichier.name.match(/\.\w+$/)?.[0] ?? '' : '.jpg');
        await new Promise<void>((ok, ko) =>
          this.api.ajouterPhoto(this.dossier().id, image, nom, null).subscribe({ next: () => ok(), error: ko }),
        );
        envoyees++;
      } catch (err) {
        this.notifications.erreur(err);
      }
    }
    this.envoi.set(false);
    if (envoyees) {
      this.notifications.ok(envoyees > 1 ? `${envoyees} photos ajoutées` : 'Photo ajoutée');
      this.modifie.emit();
    }
  }

  protected async supprimer(p: Photo): Promise<void> {
    this.agrandie.set(null);
    if (!(await this.confirmation.demander({ titre: 'Supprimer cette photo ?', bouton: 'Supprimer', danger: true }))) {
      return;
    }
    this.api.supprimerPhoto(this.dossier().id, p.id).subscribe({
      next: () => {
        this.agrandie.set(null);
        this.modifie.emit();
      },
      error: (err) => this.notifications.erreur(err),
    });
  }
}
