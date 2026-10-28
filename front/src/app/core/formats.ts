import { Pipe, PipeTransform } from '@angular/core';

const DATE = new Intl.DateTimeFormat('fr-BE', { day: '2-digit', month: '2-digit', year: 'numeric' });
const DATE_HEURE = new Intl.DateTimeFormat('fr-BE', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});
const EUROS = new Intl.NumberFormat('fr-BE', { style: 'currency', currency: 'EUR' });

/** "2026-10-23" (date seule) est lu comme une date locale, pas comme minuit UTC. */
export function lireDate(valeur: string): Date {
  return /^\d{4}-\d{2}-\d{2}$/.test(valeur) ? new Date(valeur + 'T00:00:00') : new Date(valeur);
}

/** Date du jour au format AAAA-MM-JJ (heure locale). */
export function aujourdhui(decalageJours = 0): string {
  const d = new Date();
  d.setDate(d.getDate() + decalageJours);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Nombre de jours de calendrier entre cette date et aujourd'hui. */
export function joursDepuis(valeur: string): number {
  const d = lireDate(valeur);
  const jour = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  const auj = new Date();
  auj.setHours(0, 0, 0, 0);
  return Math.round((auj.getTime() - jour.getTime()) / 86_400_000);
}

@Pipe({ name: 'date' })
export class DatePipe implements PipeTransform {
  transform(valeur: string | null | undefined, heure = false): string {
    if (!valeur) {
      return '';
    }
    return (heure ? DATE_HEURE : DATE).format(lireDate(valeur));
  }
}

@Pipe({ name: 'euros' })
export class EurosPipe implements PipeTransform {
  transform(valeur: number | null | undefined): string {
    return valeur === null || valeur === undefined ? '' : EUROS.format(valeur);
  }
}

/** "aujourd'hui", "hier", "il y a 5 jours". */
@Pipe({ name: 'depuis' })
export class DepuisPipe implements PipeTransform {
  transform(valeur: string | null | undefined): string {
    if (!valeur) {
      return '';
    }
    const jours = joursDepuis(valeur);
    if (jours <= 0) {
      return 'aujourd’hui';
    }
    return jours === 1 ? 'hier' : `il y a ${jours} jours`;
  }
}
