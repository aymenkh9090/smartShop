/** Page de resultats renvoyee par l'API (PageResponse cote backend). */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Pagination et tri envoyes a l'API (`sort` au format "champ,asc|desc"). */
export interface PageRequest {
  page: number;
  size: number;
  sort: string;
}

export function emptyPage<T>(): PageResponse<T> {
  return { content: [], page: 0, size: 0, totalElements: 0, totalPages: 0 };
}
