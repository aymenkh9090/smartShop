import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { Role } from './core/models/auth.models';
import { AuthService } from './core/services/auth.service';

interface NavItem {
  path: string;
  label: string;
  icon: string;
  roles?: Role[];
}

const NAV_ITEMS: NavItem[] = [
  { path: '/dashboard', label: 'Tableau de bord', icon: 'dashboard' },
  { path: '/sales', label: 'Ventes', icon: 'point_of_sale', roles: ['SELLER'] },
  { path: '/purchases', label: 'Achats', icon: 'shopping_cart', roles: ['MANAGER'] },
  { path: '/customer-credits', label: 'Credits clients', icon: 'account_balance_wallet', roles: ['SELLER'] },
  { path: '/supplier-credits', label: 'Credits fournisseurs', icon: 'request_quote', roles: ['MANAGER'] },
  { path: '/after-sales', label: 'SAV', icon: 'build', roles: ['SELLER', 'TECHNICIAN'] },
  { path: '/products', label: 'Produits', icon: 'inventory_2' },
  { path: '/categories', label: 'Categories', icon: 'category' },
  { path: '/stock', label: 'Stock', icon: 'warehouse' },
  { path: '/customers', label: 'Clients', icon: 'people' },
  { path: '/suppliers', label: 'Fournisseurs', icon: 'local_shipping', roles: ['MANAGER'] },
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
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly auth = inject(AuthService);
  protected readonly appName = 'SmartShop';

  /** Menu filtre selon le role (confort UX : le backend reste la reference). */
  protected readonly navItems = computed(() =>
    this.auth.currentUser() ? NAV_ITEMS.filter((item) => !item.roles || this.auth.hasAnyRole(item.roles)) : [],
  );
}
