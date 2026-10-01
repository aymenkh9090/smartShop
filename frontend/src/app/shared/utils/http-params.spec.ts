import { toHttpParams } from './http-params';

describe('toHttpParams', () => {
  it('keeps meaningful values and skips empty ones', () => {
    const params = toHttpParams({ q: 'galaxy', page: 0, active: false, categoryId: null, type: undefined, sort: '' });

    expect(params.keys()).toEqual(['q', 'page', 'active']);
    expect(params.get('page')).toBe('0');
    expect(params.get('active')).toBe('false');
  });
});
