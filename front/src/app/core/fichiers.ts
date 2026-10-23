import { Observable } from 'rxjs';

/**
 * Ouvre un PDF de l'API (qui demande le token) dans un nouvel onglet pour l'imprimer.
 * L'onglet est ouvert tout de suite, au clic : sinon les téléphones bloquent la fenêtre.
 * Sans lecteur PDF dans le navigateur (Chrome sur Android), le fichier est téléchargé avec son nom.
 */
export function ouvrirPdf(pdf: Observable<Blob>, nomFichier: string, onErreur: (err: unknown) => void): void {
  const lecteur = navigator.pdfViewerEnabled !== false;
  const onglet = lecteur ? window.open('', '_blank') : null;
  onglet?.document.write('<p style="font-family:sans-serif;padding:2rem">Chargement du document…</p>');
  pdf.subscribe({
    next: (blob) => {
      const url = URL.createObjectURL(blob);
      if (onglet && !onglet.closed) {
        onglet.location.href = url;
      } else {
        const lien = document.createElement('a');
        lien.href = url;
        lien.download = nomFichier;
        lien.click();
      }
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    },
    error: (err) => {
      onglet?.close();
      onErreur(err);
    },
  });
}
