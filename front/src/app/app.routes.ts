import { Routes } from '@angular/router';
import { adminGuard, connecteGuard } from './core/guards';
import { Layout } from './layout/layout';

export const routes: Routes = [
  {
    path: 'connexion',
    title: 'Connexion · Pc Pro Atelier',
    loadComponent: () => import('./pages/connexion/connexion').then((m) => m.Connexion),
  },
  {
    path: 'suivi',
    title: 'Suivi de réparation · Pc Pro',
    loadComponent: () => import('./pages/suivi/suivi').then((m) => m.SuiviPage),
  },
  {
    path: 'suivi/:reference',
    title: 'Suivi de réparation · Pc Pro',
    loadComponent: () => import('./pages/suivi/suivi').then((m) => m.SuiviPage),
  },
  {
    path: '',
    component: Layout,
    canActivate: [connecteGuard],
    children: [
      {
        path: '',
        title: 'Accueil · Pc Pro Atelier',
        loadComponent: () => import('./pages/accueil/accueil').then((m) => m.Accueil),
      },
      {
        path: 'dossiers',
        title: 'Dossiers · Pc Pro Atelier',
        loadComponent: () => import('./pages/dossiers/dossiers').then((m) => m.Dossiers),
      },
      {
        path: 'depot',
        title: 'Nouveau dépôt · Pc Pro Atelier',
        loadComponent: () => import('./pages/depot/depot').then((m) => m.Depot),
      },
      {
        path: 'dossiers/:id',
        title: 'Dossier · Pc Pro Atelier',
        loadComponent: () => import('./pages/dossier/dossier').then((m) => m.DossierPage),
      },
      {
        path: 'clients',
        title: 'Clients · Pc Pro Atelier',
        loadComponent: () => import('./pages/clients/clients').then((m) => m.Clients),
      },
      {
        path: 'clients/:id',
        title: 'Client · Pc Pro Atelier',
        loadComponent: () => import('./pages/clients/client').then((m) => m.ClientPage),
      },
      {
        path: 'caisse',
        title: 'Caisse · Pc Pro Atelier',
        canActivate: [adminGuard],
        loadComponent: () => import('./pages/caisse/caisse').then((m) => m.CaissePage),
      },
      {
        path: 'chiffre-affaires',
        title: 'Chiffre d’affaires · Pc Pro Atelier',
        canActivate: [adminGuard],
        loadComponent: () => import('./pages/chiffre-affaires/chiffre-affaires').then((m) => m.ChiffreAffairesPage),
      },
      {
        path: 'employes',
        title: 'Employés · Pc Pro Atelier',
        canActivate: [adminGuard],
        loadComponent: () => import('./pages/employes/employes').then((m) => m.Employes),
      },
      {
        path: 'compte',
        title: 'Mon compte · Pc Pro Atelier',
        loadComponent: () => import('./pages/compte/compte').then((m) => m.Compte),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
