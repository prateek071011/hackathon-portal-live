// Phase 1 start page: calls GET /api/health and shows whether the backend is reachable.
import { useEffect, useState } from 'react';
import { api } from '../api.js';
import MessageBar from '../components/MessageBar.jsx';

export default function StatusPage() {
  const [health, setHealth] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get('/health')
      .then((res) => {
        setHealth(res.data);
        setMessage(res.message);
      })
      .catch((err) => setError(err.message));
  }, []);

  return (
    <>
      <MessageBar type="success" text={message} />
      <MessageBar type="error" text={error} />
      <div className="card">
        <h2>Backend status</h2>
        {health === null && !error && <p>Checking...</p>}
        {health !== null && (
          <table className="table">
            <tbody>
              <tr>
                <th>Status</th>
                <td>{health.status}</td>
              </tr>
              <tr>
                <th>User-code prefix</th>
                <td>{health.userCodePrefix}</td>
              </tr>
              <tr>
                <th>Server time</th>
                <td>{health.serverTime}</td>
              </tr>
            </tbody>
          </table>
        )}
      </div>
    </>
  );
}
