// College: teams we approved, with their registration codes, and a "Download CSV" button.
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, downloadFile } from '../../api.js';
import { formatDateTime } from '../../format.js';
import MessageBar from '../../components/MessageBar.jsx';

export default function CollegeRegisteredPage() {
  const [rows, setRows] = useState(null);
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get('/college/registrations?status=APPROVED')
      .then((res) => setRows(res.data))
      .catch((err) => setError(err.message));
  }, []);

  async function handleDownload() {
    setError('');
    setDownloading(true);
    try {
      await downloadFile('/college/registrations/export', 'registered-teams.csv');
    } catch (err) {
      setError(err.message);
    } finally {
      setDownloading(false);
    }
  }

  const empty = rows !== null && rows.length === 0;

  return (
    <>
      <div className="card-title-row">
        <h2>Registered teams</h2>
        <div>
          <button className="btn" type="button" disabled={downloading || empty || rows === null} onClick={handleDownload}>
            {downloading ? 'Downloading...' : 'Download CSV'}
          </button>
          {empty && <div className="hint">Nothing to download yet</div>}
        </div>
      </div>
      <MessageBar type="error" text={error} />
      {rows === null && !error && <p className="muted">Loading...</p>}
      {empty && <div className="card muted">No registered teams yet.</div>}
      {rows !== null && rows.length > 0 && (
        <div className="card">
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Team</th>
                  <th>Hackathon</th>
                  <th>Leader</th>
                  <th>Members</th>
                  <th>Approved</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.id}>
                    <td>
                      <span className="user-code small">{r.registrationCode}</span>
                    </td>
                    <td>
                      <Link to={'/college/registrations/' + r.id}>{r.teamName}</Link>
                    </td>
                    <td>{r.hackathonTitle}</td>
                    <td>{r.leaderName}</td>
                    <td>{r.memberCount}</td>
                    <td className="small">{formatDateTime(r.decidedAt)}</td>
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
