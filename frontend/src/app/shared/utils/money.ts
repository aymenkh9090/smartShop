/**
 * Calculs d'apercu en millimes (entiers) pour eviter les erreurs d'arrondi des nombres flottants.
 * Le backend reste la reference pour les montants enregistres.
 */
export function toMillimes(amount: number | null | undefined): number {
  return Math.round((amount ?? 0) * 1000);
}

export function fromMillimes(millimes: number): number {
  return millimes / 1000;
}

/** Somme exacte de montants en dinars (3 decimales). */
export function sumAmounts(amounts: (number | null | undefined)[]): number {
  return fromMillimes(amounts.reduce<number>((total, amount) => total + toMillimes(amount), 0));
}

/** Prix unitaire x quantite, exact a 3 decimales. */
export function lineAmount(unitPrice: number, quantity: number): number {
  return fromMillimes(toMillimes(unitPrice) * quantity);
}
