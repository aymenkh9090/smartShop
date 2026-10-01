import { ChangeDetectionStrategy, Component, input, output, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { catchError, debounceTime, distinctUntilChanged, filter, map, of, switchMap } from 'rxjs';

import { Product } from '../product.models';
import { ProductService } from '../product.service';

/**
 * Champ de recherche de produit (nom, SKU, code-barres) avec autocompletion sur les produits actifs.
 */
@Component({
  selector: 'app-product-picker',
  imports: [ReactiveFormsModule, MatAutocompleteModule, MatFormFieldModule, MatInputModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-form-field class="picker">
      <mat-label>{{ label() }}</mat-label>
      <input matInput [formControl]="search" [matAutocomplete]="auto" placeholder="Nom, SKU ou code-barres" />
      <mat-autocomplete #auto="matAutocomplete" [displayWith]="display" (optionSelected)="onSelected($event)">
        @for (product of options(); track product.id) {
          <mat-option [value]="product">
            {{ product.sku }} - {{ product.name }}
            <small>(stock : {{ product.quantity }})</small>
          </mat-option>
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
export class ProductPicker {
  private readonly productService = inject(ProductService);

  readonly label = input('Produit');
  readonly selected = output<Product>();

  protected readonly search = new FormControl<string | Product>('', { nonNullable: true });

  protected readonly options = toSignal(
    this.search.valueChanges.pipe(
      filter((value): value is string => typeof value === 'string'),
      map((value) => value.trim()),
      debounceTime(250),
      distinctUntilChanged(),
      switchMap((q) =>
        q.length < 2
          ? of([])
          : this.productService.search({ q, active: true }, { page: 0, size: 10, sort: 'name,asc' }).pipe(
              map((page) => page.content),
              catchError(() => of([])),
            ),
      ),
    ),
    { initialValue: [] as Product[] },
  );

  protected readonly display = (value: Product | string | null): string =>
    value && typeof value === 'object' ? `${value.sku} - ${value.name}` : (value ?? '');

  protected onSelected(event: MatAutocompleteSelectedEvent): void {
    this.selected.emit(event.option.value as Product);
  }

  /** Vide le champ (ex. apres ajout d'une ligne d'inventaire). */
  clear(): void {
    this.search.setValue('');
  }
}
