import { ChangeDetectionStrategy, Component, input, output, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { catchError, debounceTime, distinctUntilChanged, filter, map, of, switchMap } from 'rxjs';

import { Supplier } from '../supplier.models';
import { SupplierService } from '../supplier.service';

/**
 * Champ de recherche de fournisseur avec autocompletion sur les suppliers actifs.
 */
@Component({
  selector: 'app-supplier-picker',
  imports: [ReactiveFormsModule, MatAutocompleteModule, MatFormFieldModule, MatInputModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-form-field class="picker">
      <mat-label>{{ label() }}</mat-label>
      <input matInput [formControl]="search" [matAutocomplete]="auto" placeholder="Raison sociale..." />
      <mat-autocomplete #auto="matAutocomplete" [displayWith]="display" (optionSelected)="onSelected($event)">
        @for (supplier of options(); track supplier.id) {
          <mat-option [value]="supplier">{{ supplier.name }}</mat-option>
        }
      </mat-autocomplete>
    </mat-form-field>
  `,
  styles: `
    .picker {
      width: 100%;
    }
  `,
})
export class SupplierPicker {
  private readonly supplierService = inject(SupplierService);

  readonly label = input('Fournisseur');
  readonly selected = output<Supplier>();

  protected readonly search = new FormControl<string | Supplier>('', { nonNullable: true });

  protected readonly options = toSignal(
    this.search.valueChanges.pipe(
      filter((value): value is string => typeof value === 'string'),
      map((value) => value.trim()),
      debounceTime(250),
      distinctUntilChanged(),
      switchMap((q) =>
        q.length < 2
          ? of([])
          : this.supplierService.search({ q, active: true }, { page: 0, size: 10, sort: 'name,asc' }).pipe(
              map((page) => page.content),
              catchError(() => of([])),
            ),
      ),
    ),
    { initialValue: [] as Supplier[] },
  );

  protected readonly display = (value: Supplier | string | null): string =>
    value && typeof value === 'object' ? value.name : (value ?? '');

  protected onSelected(event: MatAutocompleteSelectedEvent): void {
    this.selected.emit(event.option.value as Supplier);
  }

  /** Vide le champ (ex. apres ajout d'une ligne d'inventaire). */
  clear(): void {
    this.search.setValue('');
  }
}
