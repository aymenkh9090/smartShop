import { HttpParams } from '@angular/common/http';

type ParamValue = string | number | boolean | null | undefined;

/** Construit des HttpParams en ignorant les valeurs vides (null, undefined, ''). */
export function toHttpParams(values: Record<string, ParamValue>): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(values)) {
    if (value !== null && value !== undefined && value !== '') {
      params = params.set(key, String(value));
    }
  }
  return params;
}
