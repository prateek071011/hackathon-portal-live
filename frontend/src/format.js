// Small display helpers shared by several pages.

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

// "2026-12-01" -> "1 Dec 2026". Split by hand so the browser's time zone can never shift the day.
export function formatDate(isoDate) {
  if (!isoDate) {
    return '';
  }
  const parts = isoDate.split('-');
  const year = parts[0];
  const month = MONTHS[Number(parts[1]) - 1];
  const day = Number(parts[2]);
  return day + ' ' + month + ' ' + year;
}

// "2026-11-17T09:05:30" -> "17 Nov 2026, 09:05"
export function formatDateTime(isoDateTime) {
  if (!isoDateTime) {
    return '';
  }
  const parts = isoDateTime.split('T');
  return formatDate(parts[0]) + ', ' + parts[1].substring(0, 5);
}

// "Teams of 2–4", or "Teams of 3" when min and max are the same.
export function teamSizeText(min, max) {
  if (min === max) {
    return 'Teams of ' + min;
  }
  return 'Teams of ' + min + '–' + max;
}
