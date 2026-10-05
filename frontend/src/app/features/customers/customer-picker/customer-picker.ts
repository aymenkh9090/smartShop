import { ChangeDetectionStrategy, Component, input, output, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { catchError, debounceTime, distinctUntilChanged, filter, map, of, switchMap } from 'rxjs';

import { Customer } from '../customer.models';
import { CustomerService } from '../customer.service';

/**
 * Champ de recherche de client avec autocompletion sur les customers actifs.
 */
@Component({
  selector: 'app-customer-picker',
  imports: [ReactiveFormsModule, MatAutocompleteModule, MatFormFieldModule, MatInputModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-form-field class="picker">
      <mat-label>{{ label() }}</mat-label>
      <input matInput [formControl]="search" [matAutocomplete]="auto" placeholder="Nom, téléphone..." />
      <mat-autocomplete #auto="matAutocomplete" [displayWith]="display" (optionSelected)="onSelected($event)">
        @for (customer of options(); track customer.id) {
          <mat-option [value]="customer">{{ customer.lastName }} {{ customer.firstName }} <small>({{ customer.phone }})</small></mat-option>
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
export class CustomerPicker {
  private readonly customerService = inject(CustomerService);

  readonly label = input('Client');
  readonly selected = output<Customer>();

  protected readonly search = new FormControl<string | Customer>('', { nonNullable: true });

  protected readonly options = toSignal(
    this.search.valueChanges.pipe(
      filter((value): value is string => typeof value === 'string'),
      map((value) => value.trim()),
      debounceTime(250),
      distinctUntilChanged(),
      switchMap((q) =>
        q.length < 2
          ? of([])
          : this.customerService.search({ q, active: true }, { page: 0, size: 10, sort: 'lastName,asc' }).pipe(
              map((page) => page.content),
              catchError(() => of([])),
            ),
      ),
    ),
    { initialValue: [] as Customer[] },
  );

  protected readonly display = (value: Customer | string | null): string =>
    value && typeof value === 'object' ? `${value.lastName} ${value.firstName} (${value.phone})` : (value ?? '');

  protected onSelected(event: MatAutocompleteSelectedEvent): void {
    this.selected.emit(event.option.value as Customer);
  }

  /** Vide le champ (ex. apres ajout d'une ligne d'inventaire). */
  clear(): void {
    this.search.setValue('');
  }
}
