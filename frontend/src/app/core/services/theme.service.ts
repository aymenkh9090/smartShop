import { DOCUMENT } from '@angular/common';
import { Injectable, computed, inject, signal } from '@angular/core';

export type ThemeMode = 'light' | 'dark' | 'system';

const STORAGE_KEY = 'smartshop.theme';
const MODES: readonly ThemeMode[] = ['light', 'dark', 'system'];

/**
 * Theme clair / sombre / systeme. Le choix est memorise (confort par poste) et applique via
 * `color-scheme` sur <html> : les couleurs CSS (light-dark()) et Material suivent automatiquement.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly media = this.document.defaultView?.matchMedia?.('(prefers-color-scheme: dark)') ?? null;
  private readonly systemDark = signal(this.media?.matches ?? false);
  private readonly selected = signal<ThemeMode>(this.read());

  readonly mode = this.selected.asReadonly();
  /** Theme effectivement affiche (utile pour les graphiques, dessines en canvas). */
  readonly isDark = computed(() => this.selected() === 'dark' || (this.selected() === 'system' && this.systemDark()));

  constructor() {
    this.media?.addEventListener('change', (event) => this.systemDark.set(event.matches));
  }

  setMode(mode: ThemeMode): void {
    this.selected.set(mode);
    try {
      localStorage.setItem(STORAGE_KEY, mode);
    } catch {
      // stockage indisponible (navigation privee...) : le choix vaut pour la session
    }
    this.apply();
  }

  apply(): void {
    const root = this.document.documentElement;
    const mode = this.selected();
    root.style.colorScheme = mode === 'system' ? 'light dark' : mode;
    root.dataset['theme'] = mode;
  }

  private read(): ThemeMode {
    try {
      const stored = localStorage.getItem(STORAGE_KEY) as ThemeMode | null;
      return stored && MODES.includes(stored) ? stored : 'system';
    } catch {
      return 'system';
    }
  }
}
