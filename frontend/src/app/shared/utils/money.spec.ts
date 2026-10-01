import { fromMillimes, lineAmount, sumAmounts, toMillimes } from './money';

describe('money', () => {
  it('adds amounts without floating point drift', () => {
    expect(0.1 + 0.2).not.toBe(0.3);
    expect(sumAmounts([0.1, 0.2])).toBe(0.3);
    expect(sumAmounts([19.99, 19.99, 19.99])).toBe(59.97);
    expect(sumAmounts([null, undefined, 1.5])).toBe(1.5);
  });

  it('multiplies unit prices exactly to the millime', () => {
    expect(lineAmount(1299.5, 3)).toBe(3898.5);
    expect(lineAmount(0.333, 3)).toBe(0.999);
    expect(fromMillimes(toMillimes(12.3456))).toBe(12.346);
  });
});
