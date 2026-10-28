import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, delay, finalize, firstValueFrom, map, of, shareReplay, switchMap } from 'rxjs';
import { Employe, LoginResponse } from './models';

/**
 * Connexion de l'employé.
 *
 * Le token d'accès (15 min) reste en mémoire. Le cookie de session (HttpOnly, 30 jours) permet d'en
 * obtenir un nouveau via /api/auth/refresh : au chargement de l'appli et quand l'API répond 401.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _token = signal<string | null>(null);
  private readonly _employe = signal<Employe | null>(null);
  private refreshEnCours: Observable<string> | null = null;

  readonly employe = this._employe.asReadonly();
  readonly connecte = computed(() => this._employe() !== null);
  readonly estAdmin = computed(() => this._employe()?.role === 'ADMIN');

  token(): string | null {
    return this._token();
  }

  /** Au démarrage : reprend la session du cookie s'il y en a une. */
  restaurer(): Promise<unknown> {
    return firstValueFrom(this.renouveler().pipe(catchError(() => of(null))));
  }

  login(login: string, motDePasse: string): Observable<Employe> {
    return this.http
      .post<LoginResponse>('/api/auth/login', { login, motDePasse })
      .pipe(map((r) => this.ouvrir(r)));
  }

  /**
   * Nouveau token grâce au cookie. Un seul appel à la fois : chaque refresh change le cookie, et
   * l'ancien rejoué serait pris pour une copie. Un second essai couvre le cas où un autre onglet vient
   * de faire le refresh avec le même cookie.
   */
  renouveler(): Observable<string> {
    if (!this.refreshEnCours) {
      const appel = () => this.http.post<LoginResponse>('/api/auth/refresh', {});
      this.refreshEnCours = appel().pipe(
        catchError(() => of(null).pipe(delay(800), switchMap(appel))),
        map((r) => {
          this.ouvrir(r);
          return r.token;
        }),
        finalize(() => (this.refreshEnCours = null)),
        shareReplay(1),
      );
    }
    return this.refreshEnCours;
  }

  changerMotDePasse(ancienMotDePasse: string, nouveauMotDePasse: string): Observable<Employe> {
    return this.http
      .post<LoginResponse>('/api/auth/mot-de-passe', { ancienMotDePasse, nouveauMotDePasse })
      .pipe(map((r) => this.ouvrir(r)));
  }

  /** Déconnecte tous ses autres appareils ; celui-ci reçoit une nouvelle session. */
  deconnecterAutresAppareils(): Observable<Employe> {
    return this.http
      .post<LoginResponse>('/api/auth/deconnecter-autres-appareils', {})
      .pipe(map((r) => this.ouvrir(r)));
  }

  logout(): void {
    this.http
      .post('/api/auth/logout', {})
      .pipe(catchError(() => of(null)))
      .subscribe(() => this.sessionTerminee(false));
  }

  /** Session perdue (expirée, révoquée) : retour à l'écran de connexion. */
  sessionTerminee(garderPage = true): void {
    const page = this.router.url;
    this._token.set(null);
    this._employe.set(null);
    this.router.navigate(['/connexion'], {
      queryParams: garderPage && page && !page.startsWith('/connexion') ? { retour: page } : {},
    });
  }

  private ouvrir(r: LoginResponse): Employe {
    this._token.set(r.token);
    this._employe.set(r.employe);
    return r.employe;
  }
}

