import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

const SANS_TOKEN = ['/api/auth/login', '/api/auth/refresh', '/api/auth/logout', '/api/public/'];

/** Ajoute le token aux appels de l'API et le renouvelle une fois si l'API répond 401. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api/') || SANS_TOKEN.some((u) => req.url.startsWith(u))) {
    return next(req);
  }
  const auth = inject(AuthService);
  const avecToken = (r: HttpRequest<unknown>, token: string | null) =>
    token ? r.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : r;

  return next(avecToken(req, auth.token())).pipe(
    catchError((err) => {
      if (!(err instanceof HttpErrorResponse) || err.status !== 401) {
        return throwError(() => err);
      }
      return auth.renouveler().pipe(
        catchError(() => {
          auth.sessionTerminee();
          return throwError(() => err);
        }),
        switchMap((token) => next(avecToken(req, token))),
      );
    }),
  );
};
