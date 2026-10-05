import { TestBed } from '@angular/core/testing';

import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  afterEach(() => {
    localStorage.removeItem('smartshop.theme');
    document.documentElement.style.colorScheme = '';
    delete document.documentElement.dataset['theme'];
  });

  it('defaults to the system theme', () => {
    const service = TestBed.inject(ThemeService);
    service.apply();

    expect(service.mode()).toBe('system');
    expect(document.documentElement.style.colorScheme).toBe('light dark');
  });

  it('applies and remembers an explicit choice', () => {
    TestBed.inject(ThemeService).setMode('dark');

    expect(document.documentElement.style.colorScheme).toBe('dark');
    expect(document.documentElement.dataset['theme']).toBe('dark');
    expect(localStorage.getItem('smartshop.theme')).toBe('dark');
  });

  it('restores the stored choice and reports the effective theme', () => {
    localStorage.setItem('smartshop.theme', 'dark');
    const service = TestBed.inject(ThemeService);

    expect(service.mode()).toBe('dark');
    expect(service.isDark()).toBeTrue();
    service.setMode('light');
    expect(service.isDark()).toBeFalse();
  });

  it('ignores an invalid stored value', () => {
    localStorage.setItem('smartshop.theme', 'pink');
    expect(TestBed.inject(ThemeService).mode()).toBe('system');
  });
});
