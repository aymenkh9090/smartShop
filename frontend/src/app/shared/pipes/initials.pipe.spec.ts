import { InitialsPipe } from './initials.pipe';

describe('InitialsPipe', () => {
  const pipe = new InitialsPipe();

  it('keeps the first letter of the first two words, uppercased', () => {
    expect(pipe.transform('ben salah ali')).toBe('BS');
  });

  it('handles a single word and extra spaces', () => {
    expect(pipe.transform('  Orange  ')).toBe('O');
  });

  it('returns an empty string for null or blank values', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform('   ')).toBe('');
  });
});
