import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import { apiErrorMessage } from '../utils/api-error.utils';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.snackBar.open(message, 'OK', { duration: 3000 });
  }

  error(error: unknown, fallback?: string): void {
    this.snackBar.open(apiErrorMessage(error, fallback), 'Fermer', {
      duration: 6000,
      panelClass: 'snack-error',
    });
  }
}
