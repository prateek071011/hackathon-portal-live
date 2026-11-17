// One team. Everyone sees the members. Then, depending on who is looking:
//  - LEADER: invite by user code, answer applications, edit details, remove members, delete the team
//  - MEMBER: leave the team
//  - VISITOR: apply to join
// Disabled buttons show the reason the backend gave (e.g. "The team is full").
import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { formatDateTime } from '../format.js';
import MessageBar from '../components/MessageBar.jsx';
import StatusBadge from '../components/StatusBadge.jsx';
import SkillTags from '../components/SkillTags.jsx';
import TeamForm from '../components/TeamForm.jsx';

// The team's latest submission to the college. Once approved, it also shows the snapshot
// the backend read back from the .ser file (the leader's email is missing on purpose: it is a transient field).
function RegistrationCard({ registration }) {
  const r = registration;
  return (
    <div className="card highlight">
      <div className="card-title-row">
        <h3>Registration with {r.collegeName}</h3>
        <StatusBadge status={r.status} />
      </div>
      <p className="muted small">
        Submitted {formatDateTime(r.submittedAt)}
        {r.decidedAt && ' · decided ' + formatDateTime(r.decidedAt)}
      </p>
      {r.status === 'PENDING' && <p>Waiting for your college to review the team.</p>}
      {r.status === 'REJECTED' && (
        <p>
          Not approved. Reason: <strong>{r.rejectReason}</strong>. You can make changes and submit again.
        </p>
      )}
      {r.status === 'APPROVED' && (
        <p>
          Registration code: <span className="user-code">{r.registrationCode}</span>
        </p>
      )}
      {r.snapshot && (
        <div className="snapshot">
          <strong>Saved snapshot</strong> <span className="muted small">(read back from {r.snapshot.fileName})</span>
          <table className="table facts">
            <tbody>
              <tr>
                <th>Team</th>
                <td>{r.snapshot.teamName}</td>
              </tr>
              <tr>
                <th>Hackathon</th>
                <td>{r.snapshot.hackathonTitle}</td>
              </tr>
              <tr>
                <th>Members</th>
                <td>{r.snapshot.memberNames.join(', ')}</td>
              </tr>
              <tr>
                <th>Approved at</th>
                <td>{formatDateTime(r.snapshot.approvedAt)}</td>
              </tr>
              <tr>
                <th>Leader email</th>
                <td className="muted">{r.snapshot.leaderEmail || 'not stored (transient field)'}</td>
              </tr>
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default function TeamPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const [team, setTeam] = useState(null);
  const [formKey, setFormKey] = useState(0);
  const [inviteCodes, setInviteCodes] = useState('');
  const [inviteResult, setInviteResult] = useState(null);
  const [seekers, setSeekers] = useState([]);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(location.state && location.state.message ? location.state.message : '');
  const [error, setError] = useState('');

  // Loads the team; for the leader also the "Find teammates" list (students looking for a team).
  function loadTeam() {
    api
      .get('/teams/' + id)
      .then((res) => {
        setTeam(res.data);
        if (res.data.viewerRole === 'LEADER') {
          return api.get('/teams/' + id + '/seekers').then((seekerRes) => setSeekers(seekerRes.data));
        }
        return null;
      })
      .catch((err) => setError(err.message));
  }

  // One-click invite from the "Find teammates" list: the same invite endpoint, with one user code.
  function handleInviteSeeker(seeker) {
    setInviteResult(null);
    run(() => api.post('/teams/' + id + '/invites', { userCodes: seeker.userCode }));
  }

  useEffect(() => {
    loadTeam();
  }, [id]);

  // Runs one backend action, shows its message, and reloads the team.
  async function run(action) {
    setMessage('');
    setError('');
    setBusy(true);
    try {
      const res = await action();
      setMessage(res.message);
      loadTeam();
      return res;
    } catch (err) {
      setError(err.message);
      return null;
    } finally {
      setBusy(false);
    }
  }

  async function handleInvite(event) {
    event.preventDefault();
    setInviteResult(null);
    const res = await run(() => api.post('/teams/' + id + '/invites', { userCodes: inviteCodes }));
    if (res !== null) {
      setInviteResult(res.data);
      setInviteCodes('');
    }
  }

  async function handleSaveDetails(values) {
    const res = await run(() => api.put('/teams/' + id, values));
    if (res !== null) {
      setFormKey(formKey + 1);
    }
  }

  function handleRemove(member) {
    if (window.confirm('Remove ' + member.name + ' from the team?')) {
      run(() => api.del('/teams/' + id + '/members/' + member.userId));
    }
  }

  async function handleLeave() {
    if (!window.confirm('Leave this team?')) {
      return;
    }
    const res = await run(() => api.post('/teams/' + id + '/leave'));
    if (res !== null) {
      navigate('/hackathons/' + team.hackathonId);
    }
  }

  function handleSubmit() {
    if (window.confirm('Submit "' + team.name + '" to your college? The roster will be locked.')) {
      run(() => api.post('/teams/' + id + '/submit'));
    }
  }

  async function handleDelete() {
    if (!window.confirm('Delete team "' + team.name + '"? Members will be told. This cannot be undone.')) {
      return;
    }
    const res = await run(() => api.del('/teams/' + id));
    if (res !== null) {
      navigate('/hackathons/' + team.hackathonId);
    }
  }

  if (team === null) {
    return (
      <>
        <MessageBar type="error" text={error} />
        {!error && <p className="muted">Loading team...</p>}
      </>
    );
  }

  const isLeader = team.viewerRole === 'LEADER';
  const isMember = team.viewerRole === 'MEMBER';
  const isVisitor = team.viewerRole === 'VISITOR';

  return (
    <>
      <p>
        <Link to={'/hackathons/' + team.hackathonId}>← {team.hackathonTitle}</Link>
      </p>
      <MessageBar type="success" text={message} />
      <MessageBar type="error" text={error} />
      {team.lockedReason && <MessageBar type="info" text={team.lockedReason} />}

      <div className="card">
        <div className="card-title-row">
          <h2>{team.name}</h2>
          <StatusBadge status={team.status} />
        </div>
        <p className="muted">
          {team.members.length} of {team.maxTeamSize} members (needs at least {team.minTeamSize}) · Registration: {team.deadlineText}
        </p>
        {team.description && <p className="description">{team.description}</p>}
        <p>
          <span className="muted small">Looking for: </span>
          <SkillTags skills={team.lookingFor} />
          {isVisitor && team.lookingFor && <span className="match">{team.matchPercent}% match for you</span>}
        </p>

        {isVisitor && (
          <div className="actions">
            <button className="btn" type="button" disabled={busy || Boolean(team.applyBlockedReason)} onClick={() => run(() => api.post('/teams/' + id + '/apply'))}>
              Apply to join
            </button>
            {team.applyBlockedReason && <span className="hint">{team.applyBlockedReason}</span>}
          </div>
        )}
        {isMember && (
          <div className="actions">
            <button className="btn btn-secondary" type="button" disabled={busy || Boolean(team.lockedReason)} onClick={handleLeave}>
              Leave team
            </button>
            {team.lockedReason && <span className="hint">{team.lockedReason}</span>}
          </div>
        )}
      </div>

      {team.registration && <RegistrationCard registration={team.registration} />}

      <div className="card">
        <h3>Members</h3>
        <ul className="member-list">
          {team.members.map((m) => (
            <li key={m.userId}>
              <div className="avatar-small">{m.name.charAt(0).toUpperCase()}</div>
              <div className="member-info">
                <strong>{m.name}</strong> {m.leader && <span className="badge OPEN">LEADER</span>}
                <div className="muted small">
                  {m.userCode} · {m.branch}, year {m.year}
                </div>
                <SkillTags skills={m.skills} />
              </div>
              {isLeader && !m.leader && (
                <button className="btn btn-secondary btn-small" type="button" disabled={busy || Boolean(team.lockedReason)} title={team.lockedReason || ''} onClick={() => handleRemove(m)}>
                  Remove
                </button>
              )}
            </li>
          ))}
        </ul>
      </div>

      {isLeader && (
        <>
          <div className="card">
            <h3>Invite by user code</h3>
            <p className="muted small">Ask your friends for the code on their profile, e.g. HTF-7K3Q9M. You can paste several, separated by commas.</p>
            <form className="actions" onSubmit={handleInvite}>
              <input className="grow" placeholder="HTF-XXXXXX, HTF-YYYYYY" maxLength={500} disabled={Boolean(team.inviteBlockedReason)} value={inviteCodes} onChange={(e) => setInviteCodes(e.target.value)} />
              <button className="btn" type="submit" disabled={busy || Boolean(team.inviteBlockedReason) || inviteCodes.trim() === ''}>
                Send invites
              </button>
            </form>
            {team.inviteBlockedReason && <p className="hint">{team.inviteBlockedReason}</p>}
            {!team.inviteBlockedReason && inviteCodes.trim() === '' && <p className="hint">Type at least one code to send invites.</p>}
            {inviteResult !== null && inviteResult.sent.length > 0 && <p className="success-text">Invited: {inviteResult.sent.join(', ')}</p>}
            {inviteResult !== null && inviteResult.skipped.length > 0 && (
              <div className="error-list">
                <strong>Not invited:</strong>
                <ul>
                  {inviteResult.skipped.map((line) => (
                    <li key={line}>{line}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>

          <div className="card">
            <h3>Find teammates ({seekers.length})</h3>
            <p className="muted small">
              Students in this hackathon who said "I'm looking for a team". Best match with what you are looking for comes first.
            </p>
            {seekers.length === 0 && <p className="muted">Nobody is on the list right now.</p>}
            <ul className="member-list">
              {seekers.map((s) => {
                let reason = team.inviteBlockedReason;
                if (!reason && s.invitePending) {
                  reason = 'Request pending';
                }
                return (
                  <li key={s.userId}>
                    <div className="avatar-small">{s.name.charAt(0).toUpperCase()}</div>
                    <div className="member-info">
                      <strong>{s.name}</strong> {team.lookingFor && <span className="match">{s.matchPercent}% match</span>}
                      <div className="muted small">
                        {s.userCode} · {s.collegeName} · {s.branch}, year {s.year}
                      </div>
                      <SkillTags skills={s.skills} />
                    </div>
                    <div>
                      <button className="btn btn-small" type="button" disabled={busy || Boolean(reason)} onClick={() => handleInviteSeeker(s)}>
                        Invite
                      </button>
                      {reason && <div className="hint">{reason}</div>}
                    </div>
                  </li>
                );
              })}
            </ul>
          </div>

          <div className="card">
            <h3>Pending requests ({team.pendingRequests.length})</h3>
            {team.pendingRequests.length === 0 && <p className="muted">Nothing waiting.</p>}
            <ul className="request-list">
              {team.pendingRequests.map((r) => (
                <li key={r.id}>
                  <div>
                    {r.type === 'APPLICATION' ? (
                      <>
                        <strong>{r.studentName}</strong> ({r.studentUserCode}) wants to join
                      </>
                    ) : (
                      <>
                        You invited <strong>{r.studentName}</strong> ({r.studentUserCode})
                      </>
                    )}
                    <div className="muted small">{formatDateTime(r.createdAt)}</div>
                  </div>
                  <div className="row-actions">
                    {r.incoming ? (
                      <>
                        <button className="btn btn-small" type="button" disabled={busy || Boolean(team.inviteBlockedReason)} title={team.inviteBlockedReason || ''} onClick={() => run(() => api.post('/requests/' + r.id + '/accept'))}>
                          Accept
                        </button>
                        <button className="btn btn-secondary btn-small" type="button" disabled={busy} onClick={() => run(() => api.post('/requests/' + r.id + '/reject'))}>
                          Reject
                        </button>
                      </>
                    ) : (
                      <button className="btn btn-secondary btn-small" type="button" disabled={busy} onClick={() => run(() => api.post('/requests/' + r.id + '/cancel'))}>
                        Cancel invite
                      </button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
            {team.inviteBlockedReason && team.pendingRequests.length > 0 && <p className="hint">Accept is off: {team.inviteBlockedReason}</p>}
          </div>

          <div className="card">
            <h3>Edit team details</h3>
            <TeamForm
              key={formKey}
              initial={{ name: team.name, description: team.description || '', lookingFor: team.lookingFor || '' }}
              submitLabel="Save details"
              busy={busy}
              disabledReason={team.lockedReason}
              onSave={handleSaveDetails}
            />
          </div>

          <div className="card">
            <h3>Submit to your college</h3>
            <p className="muted small">
              Your college checks the team and gives you a registration code. After submitting, nobody can join or leave.
            </p>
            <button className="btn" type="button" disabled={busy || Boolean(team.submitBlockedReason)} onClick={handleSubmit}>
              Submit team
            </button>
            {team.submitBlockedReason && <span className="hint">{team.submitBlockedReason}</span>}
          </div>

          <div className="card danger-zone">
            <h3>Delete team</h3>
            <p className="muted small">Removes the team and all its requests. Members are notified.</p>
            <button className="btn btn-danger" type="button" disabled={busy || Boolean(team.lockedReason)} onClick={handleDelete}>
              Delete team
            </button>
            {team.lockedReason && <span className="hint">{team.lockedReason}</span>}
          </div>
        </>
      )}
    </>
  );
}
