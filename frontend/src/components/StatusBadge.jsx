// A small coloured label for a status such as OPEN, CLOSED, FULL or REGISTERED.
// The colour comes from styles.css (.badge.OPEN, .badge.CLOSED, ...).
export default function StatusBadge({ status }) {
  return <span className={'badge ' + status}>{status}</span>;
}
