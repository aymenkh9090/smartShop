import { optionalNumberAttribute } from './optional-number';

describe('optionalNumberAttribute', () => {
  it('parses numeric values', () => {
    expect(optionalNumberAttribute('42')).toBe(42);
    expect(optionalNumberAttribute(7)).toBe(7);
  });

  it('returns undefined instead of NaN for missing or invalid values', () => {
    expect(optionalNumberAttribute(undefined)).toBeUndefined();
    expect(optionalNumberAttribute('')).toBeUndefined();
    expect(optionalNumberAttribute('abc')).toBeUndefined();
  });
});
