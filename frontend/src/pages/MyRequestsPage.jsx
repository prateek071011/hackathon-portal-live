// "My Requests": invites and applications waiting for my answer (incoming) and the ones I sent (outgoing).
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { formatDateTime } from '../format.js';
import MessageBar from '../components/MessageBar.jsx';
import StatusBadge from '../components/StatusBadge.jsx';

// One plain sentence per request, from my point of view.
function describe(r) {
  if (r.incoming && r.type === 'INVITE') {
    return r.leaderName + ' invited you to join ' + r.teamName;
  }
  if (r.incoming) {
    return r.studentName + ' (' + r.studentUserCode + ') wants to join ' + r.teamName;
  }
  if (r.type === 'INVITE') {
    return 'You invited ' + r.studentName + ' to ' + r.teamName;
  }
  return 'You applied to ' + r.teamName;
}

function RequestTable({ requests, busy, onAction }) {
  if (requests.length === 0) {
    return <p className="muted">Nothing here.</p>;
  }
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th>Request</th>
            <th>Hackathon</th>
            <th>Sent</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {requests.map((r) => (
            <tr key={r.id}>
              <td>
                {describe(r)}
                <div className="small">
                  <Link to={'/teams/' + r.teamId}>View team</Link>
                </div>
              </td>
              <td>{r.hackathonTitle}</td>
              <td className="small">{formatDateTime(r.createdAt)}</td>
              <td>
                <StatusBadge status={r.status} />
              </td>
              <td className="row-actions">
                {r.status === 'PENDING' && r.incoming && (
                  <>
                    <button className="btn btn-small" type="button" disabled={busy} onClick={() => onAction(r, 'accept')}>
                      Accept
                    </button>
                    <button className="btn btn-secondary btn-small" type="button" disabled={busy} onClick={() => onAction(r, 'reject')}>
                      Reject
                    </button>
                  </>
                )}
                {r.status === 'PENDING' && !r.incoming && (
                  <button className="btn btn-secondary btn-small" type="button" disabled={busy} onClick={() => onAction(r, 'cancel')}>
                    Cancel
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function MyRequestsPage() {
  const [requests, setRequests] = useState(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  function loadRequests() {
    api
      .get('/requests/mine')
      .then((res) => setRequests(res.data))
      .catch((err) => setError(err.message));
  }

  useEffect(() => {
    loadRequests();
  }, []);

  async function handleAction(request, action) {
    setMessage('');
    setError('');
    setBusy(true);
    try {
      const res = await api.post('/requests/' + request.id + '/' + action);
      setMessage(res.message);
      loadRequests();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <h2>My requests</h2>
      <MessageBar type="success" text={message} />
      <MessageBar type="error" text={error} />
      {requests === null && !error && <p className="muted">Loading...</p>}
      {requests !== null && (
        <>
          <div className="card">
            <h3>Waiting for your answer</h3>
            <RequestTable requests={requests.incoming} busy={busy} onAction={handleAction} />
          </div>
          <div className="card">
            <h3>Sent by you</h3>
            <RequestTable requests={requests.outgoing} busy={busy} onAction={handleAction} />
          </div>
        </>
      )}
    </>
  );
}
