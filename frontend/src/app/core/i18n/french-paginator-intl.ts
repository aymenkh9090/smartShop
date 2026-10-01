import { Injectable } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';

@Injectable()
export class FrenchPaginatorIntl extends MatPaginatorIntl {
  override itemsPerPageLabel = 'Elements par page';
  override nextPageLabel = 'Page suivante';
  override previousPageLabel = 'Page precedente';
  override firstPageLabel = 'Premiere page';
  override lastPageLabel = 'Derniere page';

  override getRangeLabel = (page: number, pageSize: number, length: number): string => {
    if (length === 0) {
      return '0 sur 0';
    }
    const start = page * pageSize;
    const end = Math.min(start + pageSize, length);
    return `${start + 1} - ${end} sur ${length}`;
  };
}
