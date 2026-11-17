// Admin: every team (with delete), how many teams each hackathon has, and the last 20 actions.
import { useEffect, useState } from 'react';
import { api } from '../../api.js';
import { formatDateTime } from '../../format.js';
import MessageBar from '../../components/MessageBar.jsx';
import StatusBadge from '../../components/StatusBadge.jsx';

export default function AdminTeamsPage() {
  const [overview, setOverview] = useState(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  function loadOverview() {
    api
      .get('/admin/teams')
      .then((res) => setOverview(res.data))
      .catch((err) => setError(err.message));
  }

  useEffect(() => {
    loadOverview();
  }, []);

  async function handleDelete(team) {
    if (!window.confirm('Delete team "' + team.name + '"? Its members will be notified. This cannot be undone.')) {
      return;
    }
    setMessage('');
    setError('');
    setBusy(true);
    try {
      const res = await api.del('/admin/teams/' + team.id);
      setMessage(res.message);
      loadOverview();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <h2>Manage teams</h2>
      <MessageBar type="success" text={message} />
      <MessageBar type="error" text={error} />
      {overview === null && !error && <p className="muted">Loading...</p>}

      {overview !== null && (
        <>
          <div className="two-columns">
            <div className="card">
              <h3>Teams per hackathon</h3>
              {overview.teamsPerHackathon.length === 0 && <p className="muted">No teams yet.</p>}
              <table className="table">
                <tbody>
                  {overview.teamsPerHackathon.map((c) => (
                    <tr key={c.hackathonTitle}>
                      <td>{c.hackathonTitle}</td>
                      <td className="number">{c.teamCount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="card">
              <h3>Last 20 actions</h3>
              {overview.recentActions.length === 0 && <p className="muted">Nothing since the server started.</p>}
              <ul className="activity-list">
                {overview.recentActions.map((line, index) => (
                  <li key={index}>{line}</li>
                ))}
              </ul>
            </div>
          </div>

          <div className="card">
            <h3>All teams ({overview.teams.length})</h3>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Team</th>
                    <th>Hackathon</th>
                    <th>Leader</th>
                    <th>Members</th>
                    <th>Status</th>
                    <th>Created</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {overview.teams.map((t) => (
                    <tr key={t.id}>
                      <td>
                        <strong>{t.name}</strong>
                      </td>
                      <td>{t.hackathonTitle}</td>
                      <td>{t.leaderName}</td>
                      <td>
                        {t.memberCount} / {t.maxTeamSize}
                      </td>
                      <td>
                        <StatusBadge status={t.status} />
                      </td>
                      <td className="small">{formatDateTime(t.createdAt)}</td>
                      <td>
                        <button className="btn btn-danger btn-small" type="button" disabled={busy} onClick={() => handleDelete(t)}>
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </>
  );
}
