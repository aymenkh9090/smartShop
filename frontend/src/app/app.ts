import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';

import { ROLE_LABELS, Role } from './core/models/auth.models';
import { AuthService } from './core/services/auth.service';
import { ThemeMode, ThemeService } from './core/services/theme.service';

interface NavItem {
  path: string;
  label: string;
  icon: string;
  roles?: Role[];
}

interface NavGroup {
  label: string | null;
  items: NavItem[];
}

const NAV_GROUPS: NavGroup[] = [
  { label: null, items: [{ path: '/dashboard', label: 'Tableau de bord', icon: 'space_dashboard' }] },
  {
    label: 'Vente',
    items: [
      { path: '/sales', label: 'Ventes', icon: 'point_of_sale', roles: ['SELLER'] },
      { path: '/after-sales', label: 'SAV', icon: 'build', roles: ['SELLER', 'TECHNICIAN'] },
      { path: '/customer-credits', label: 'Crédits clients', icon: 'account_balance_wallet', roles: ['SELLER'] },
      { path: '/customers', label: 'Clients', icon: 'groups' },
    ],
  },
  {
    label: 'Stock',
    items: [
      { path: '/products', label: 'Produits', icon: 'inventory_2' },
      { path: '/categories', label: 'Catégories', icon: 'category' },
      { path: '/stock', label: 'Mouvements', icon: 'warehouse' },
    ],
  },
  {
    label: 'Achats',
    items: [
      { path: '/purchases', label: 'Achats', icon: 'shopping_cart', roles: ['MANAGER'] },
      { path: '/suppliers', label: 'Fournisseurs', icon: 'local_shipping', roles: ['MANAGER'] },
      { path: '/supplier-credits', label: 'Crédits fournisseurs', icon: 'request_quote', roles: ['MANAGER'] },
    ],
  },
  {
    label: 'Administration',
    items: [{ path: '/users', label: 'Utilisateurs', icon: 'manage_accounts', roles: ['ADMIN'] }],
  },
];

const THEME_OPTIONS: { mode: ThemeMode; label: string; icon: string }[] = [
  { mode: 'light', label: 'Clair', icon: 'light_mode' },
  { mode: 'dark', label: 'Sombre', icon: 'dark_mode' },
  { mode: 'system', label: 'Système', icon: 'contrast' },
];

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatDividerModule,
    MatTooltipModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly auth = inject(AuthService);
  protected readonly theme = inject(ThemeService);
  private readonly router = inject(Router);

  protected readonly appName = 'SmartShop';
  protected readonly roleLabels = ROLE_LABELS;
  protected readonly themeOptions = THEME_OPTIONS;

  /** Menu escamotable en dessous de 960px (tablette, mobile). */
  protected readonly compact = toSignal(
    inject(BreakpointObserver)
      .observe('(max-width: 959.98px)')
      .pipe(map((state) => state.matches)),
    { initialValue: false },
  );
  protected readonly menuOpen = signal(false);

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  /** Menu filtre selon le role (confort UX : le backend reste la reference) ; groupes vides masques. */
  protected readonly navGroups = computed(() => {
    if (!this.auth.currentUser()) {
      return [];
    }
    return NAV_GROUPS.map((group) => ({
      ...group,
      items: group.items.filter((item) => !item.roles || this.auth.hasAnyRole(item.roles)),
    })).filter((group) => group.items.length > 0);
  });

  /** Section courante (groupe + page), affichee dans l'en-tete. */
  protected readonly currentSection = computed(() => {
    const path = this.url().split('?')[0];
    for (const group of this.navGroups()) {
      const item = group.items.find((candidate) => path === candidate.path || path.startsWith(candidate.path + '/'));
      if (item) {
        return { group: group.label, label: item.label, icon: item.icon };
      }
    }
    return null;
  });

  protected readonly initials = computed(() => {
    const user = this.auth.currentUser();
    return user ? `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase() : '';
  });

  protected readonly themeIcon = computed(
    () => THEME_OPTIONS.find((option) => option.mode === this.theme.mode())?.icon ?? 'contrast',
  );

  protected onNavigate(): void {
    if (this.compact()) {
      this.menuOpen.set(false);
    }
  }
}
