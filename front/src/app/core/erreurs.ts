import { HttpErrorResponse } from '@angular/common/http';

/** Message lisible à partir d'une erreur de l'API (format ErrorResponse du backend). */
export function messageErreur(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return 'Pas de connexion au serveur. Vérifiez internet et réessayez.';
    }
    const corps = err.error as { message?: string; fieldErrors?: { message: string }[] } | null;
    const champs = corps?.fieldErrors?.map((f) => f.message).filter(Boolean) ?? [];
    if (champs.length) {
      return champs.join('. ');
    }
    if (corps?.message) {
      return corps.message;
    }
    if (err.status === 403) {
      return 'Action réservée aux administrateurs.';
    }
    if (err.status >= 500) {
      return 'Erreur du serveur. Réessayez dans un instant.';
    }
  }
  return 'Une erreur est survenue.';
}
