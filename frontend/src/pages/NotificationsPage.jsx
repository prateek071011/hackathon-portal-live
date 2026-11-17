// Notifications: loaded when the page opens. Unread ones are highlighted, then all are marked as read.
import { useEffect, useState } from 'react';
import { api } from '../api.js';
import { formatDateTime } from '../format.js';
import MessageBar from '../components/MessageBar.jsx';

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    // In development React runs this effect twice on purpose. The flag makes the first (cancelled) run do nothing,
    // otherwise its "mark all read" could reach the server before the second run loads the list.
    let cancelled = false;
    api
      .get('/notifications')
      .then((res) => {
        if (cancelled) {
          return null;
        }
        // Show the list first (with the unread ones highlighted), then tell the backend they are read.
        setNotifications(res.data);
        return api.post('/notifications/read-all');
      })
      .catch((err) => setError(err.message));
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <>
      <h2>Notifications</h2>
      <MessageBar type="error" text={error} />
      {notifications === null && !error && <p className="muted">Loading...</p>}
      {notifications !== null && notifications.length === 0 && <div className="card muted">No notifications yet.</div>}
      {notifications !== null && notifications.length > 0 && (
        <div className="card">
          <ul className="notification-list">
            {notifications.map((n) => (
              <li key={n.id} className={n.read ? '' : 'unread'}>
                <div>{n.message}</div>
                <div className="muted small">
                  {formatDateTime(n.createdAt)}
                  {!n.read && ' · new'}
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </>
  );
}
