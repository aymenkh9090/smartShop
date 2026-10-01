import { fromIsoDate, toIsoDate } from './reschedule-dialog';

describe('ISO local dates', () => {
  it('round-trips without timezone shift', () => {
    const date = fromIsoDate('2026-12-31');
    expect(date.getFullYear()).toBe(2026);
    expect(date.getMonth()).toBe(11);
    expect(date.getDate()).toBe(31);
    expect(toIsoDate(date)).toBe('2026-12-31');
    expect(toIsoDate(new Date(2026, 0, 5))).toBe('2026-01-05');
  });
});
