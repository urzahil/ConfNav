/** Today's date as YYYY-MM-DD in the user's local time zone (toISOString would give the UTC date). */
export function todayLocal(date: Date = new Date()): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}
