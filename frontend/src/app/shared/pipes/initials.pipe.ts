import { Pipe, PipeTransform } from '@angular/core';

/** Initiales d'un nom (2 lettres max) : "Ben Salah Ali" -> "BS", "Tunisie Telecom" -> "TT". */
@Pipe({ name: 'initials' })
export class InitialsPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    const words = (value ?? '').trim().split(/\s+/).filter(Boolean);
    return words
      .slice(0, 2)
      .map((word) => word.charAt(0))
      .join('')
      .toUpperCase();
  }
}
