// College: teams submitted by our students that are waiting for a decision. Oldest first.
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api.js';
import { formatDateTime } from '../../format.js';
import MessageBar from '../../components/MessageBar.jsx';

export default function CollegeQueuePage() {
  const [rows, setRows] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get('/college/registrations?status=PENDING')
      .then((res) => setRows(res.data))
      .catch((err) => setError(err.message));
  }, []);

  return (
    <>
      <h2>Review queue</h2>
      <MessageBar type="error" text={error} />
      {rows === null && !error && <p className="muted">Loading...</p>}
      {rows !== null && rows.length === 0 && <div className="card muted">No teams are waiting for review.</div>}
      {rows !== null && rows.length > 0 && (
        <div className="card">
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Team</th>
                  <th>Hackathon</th>
                  <th>Leader</th>
                  <th>Members</th>
                  <th>Submitted</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.id}>
                    <td>
                      <strong>{r.teamName}</strong>
                    </td>
                    <td>{r.hackathonTitle}</td>
                    <td>{r.leaderName}</td>
                    <td>{r.memberCount}</td>
                    <td className="small">{formatDateTime(r.submittedAt)}</td>
                    <td>
                      <Link className="btn btn-small" to={'/college/registrations/' + r.id}>
                        Review
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </>
  );
}
