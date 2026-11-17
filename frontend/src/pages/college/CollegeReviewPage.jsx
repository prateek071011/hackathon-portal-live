// College: one submitted team with every member's details, and the Approve / Reject decision.
// Rejecting needs a reason, which the students are shown.
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../../api.js';
import { formatDate, formatDateTime } from '../../format.js';
import MessageBar from '../../components/MessageBar.jsx';
import StatusBadge from '../../components/StatusBadge.jsx';

export default function CollegeReviewPage() {
  const { id } = useParams();
  const [detail, setDetail] = useState(null);
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  function loadDetail() {
    api
      .get('/college/registrations/' + id)
      .then((res) => setDetail(res.data))
      .catch((err) => setError(err.message));
  }

  useEffect(() => {
    loadDetail();
  }, [id]);

  async function decide(action, body) {
    setMessage('');
    setError('');
    setBusy(true);
    try {
      const res = await api.post('/college/registrations/' + id + '/' + action, body);
      setMessage(res.message);
      setReason('');
      loadDetail();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  function handleApprove() {
    if (window.confirm('Approve this team? They will get a registration code.')) {
      decide('approve');
    }
  }

  if (detail === null) {
    return (
      <>
        <p>
          <Link to="/college/queue">← Review queue</Link>
        </p>
        <MessageBar type="error" text={error} />
        {!error && <p className="muted">Loading...</p>}
      </>
    );
  }

  const s = detail.summary;
  const pending = s.status === 'PENDING';
  let rejectHint = '';
  if (pending && reason.trim() === '') {
    rejectHint = 'Type a reason to reject';
  }

  return (
    <>
      <p>
        <Link to="/college/queue">← Review queue</Link>
      </p>
      <MessageBar type="success" text={message} />
      <MessageBar type="error" text={error} />

      <div className="card">
        <div className="card-title-row">
          <h2>{s.teamName}</h2>
          <StatusBadge status={s.status} />
        </div>
        <p className="muted">
          {s.hackathonTitle} · {formatDate(detail.eventStart)} – {formatDate(detail.eventEnd)} · teams of {detail.minTeamSize}–
          {detail.maxTeamSize} · submitted {formatDateTime(s.submittedAt)}
        </p>
        {detail.teamDescription && <p className="description">{detail.teamDescription}</p>}
        {s.registrationCode && (
          <p>
            Registration code: <span className="user-code">{s.registrationCode}</span>
          </p>
        )}
        {s.rejectReason && <p>Rejected because: {s.rejectReason}</p>}
      </div>

      <div className="card">
        <h3>Members ({detail.members.length})</h3>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Roll number</th>
                <th>Branch / year</th>
                <th>College</th>
                <th>Email</th>
              </tr>
            </thead>
            <tbody>
              {detail.members.map((m) => (
                <tr key={m.userCode}>
                  <td>
                    {m.name} {m.leader && <span className="badge OPEN">LEADER</span>}
                    <div className="muted small">{m.userCode}</div>
                  </td>
                  <td>{m.rollNumber}</td>
                  <td>
                    {m.branch}, year {m.year}
                  </td>
                  <td>{m.collegeName}</td>
                  <td className="small">{m.email}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {pending && (
        <div className="card">
          <h3>Decision</h3>
          <div className="actions">
            <button className="btn" type="button" disabled={busy} onClick={handleApprove}>
              Approve
            </button>
          </div>
          <div className="form-row decision-reason">
            <label htmlFor="reason">Reason for rejecting (the students will see it)</label>
            <textarea id="reason" rows={2} maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)} />
          </div>
          <button className="btn btn-danger" type="button" disabled={busy || reason.trim() === ''} onClick={() => decide('reject', { reason: reason })}>
            Reject
          </button>
          {rejectHint && <span className="hint">{rejectHint}</span>}
        </div>
      )}
    </>
  );
}
