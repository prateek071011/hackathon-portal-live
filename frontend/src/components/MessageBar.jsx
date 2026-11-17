// The message bar shown at the top of a page.
// Usage: <MessageBar type="error" text={error} />. type is "success", "error" or "info".
// Shows nothing when text is empty.
export default function MessageBar({ type, text }) {
  if (!text) {
    return null;
  }
  return <div className={'message-bar ' + type}>{text}</div>;
}
