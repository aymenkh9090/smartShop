/**
 * Transform d'input pour un identifiant facultatif (ex. `?customerId=`).
 * `numberAttribute` renvoie NaN quand le parametre est absent (son fallback `undefined` retombe sur NaN) :
 * NaN serait alors envoye a l'API.
 */
export function optionalNumberAttribute(value: unknown): number | undefined {
  if (value === null || value === undefined || value === '') {
    return undefined;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}
