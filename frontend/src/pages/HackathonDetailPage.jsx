// One hackathon in detail, plus its teams: your own team (if any), a "Create team" form,
// and the open teams sorted by how well your skills match what they are looking for.
import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { formatDate, teamSizeText } from '../format.js';
import MessageBar from '../components/MessageBar.jsx';
import StatusBadge from '../components/StatusBadge.jsx';
import SkillTags from '../components/SkillTags.jsx';
import TeamForm, { EMPTY_TEAM } from '../components/TeamForm.jsx';

export default function HackathonDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [hackathon, setHackathon] = useState(null);
  const [teams, setTeams] = useState(null);
  const [showCreate, setShowCreate] = useState(false);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  function loadTeams() {
    api
      .get('/hackathons/' + id + '/teams')
      .then((res) => setTeams(res.data))
      .catch((err) => setError(err.message));
  }

  useEffect(() => {
    api
      .get('/hackathons/' + id)
      .then((res) => setHackathon(res.data))
      .catch((err) => setError(err.message));
    loadTeams();
  }, [id]);

  async function handleCreate(values) {
    setMessage('');
    setError('');
    setBusy(true);
    try {
      const res = await api.post('/hackathons/' + id + '/teams', values);
      navigate('/teams/' + res.data.id, { state: { message: res.message } });
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  }

  async function handleApply(team) {
    setMessage('');
    setError('');
    try {
      const res = await api.post('/teams/' + team.id + '/apply');
      setMessage(res.message);
      loadTeams();
    } catch (err) {
      setError(err.message);
    }
  }

  // "I'm looking for a team" puts me on the list team leaders can browse; "Stop looking" takes me off.
  async function handleLooking(start) {
    setMessage('');
    setError('');
    setBusy(true);
    try {
      const path = '/hackathons/' + id + '/looking-for-team';
      const res = start ? await api.post(path) : await api.del(path);
      setMessage(res.message);
      loadTeams();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <p>
        <Link to="/hackathons">← All hackathons</Link>
      </p>
      <MessageBar type="success" text={message} />
      <MessageBar type="error" text={error} />
      {hackathon === null && !error && <p className="muted">Loading...</p>}

      {hackathon !== null && (
        <div className="card">
          <div className="card-title-row">
            <h2>{hackathon.title}</h2>
            <StatusBadge status={hackathon.status} />
          </div>
          <p className="muted">by {hackathon.organizer}</p>
          {hackathon.description && <p className="description">{hackathon.description}</p>}
          <table className="table facts">
            <tbody>
              <tr>
                <th>Registration closes</th>
                <td>
                  {formatDate(hackathon.registrationDeadline)}{' '}
                  <span className={hackathon.status === 'OPEN' ? 'deadline open' : 'deadline'}>({hackathon.deadlineText})</span>
                </td>
              </tr>
              <tr>
                <th>Event dates</th>
                <td>
                  {formatDate(hackathon.startDate)} – {formatDate(hackathon.endDate)}
                </td>
              </tr>
              <tr>
                <th>Team size</th>
                <td>{teamSizeText(hackathon.minTeamSize, hackathon.maxTeamSize)}</td>
              </tr>
              <tr>
                <th>Teams so far</th>
                <td>{hackathon.teamCount}</td>
              </tr>
            </tbody>
          </table>
        </div>
      )}

      {teams !== null && (
        <>
          {teams.myTeamId !== null && (
            <div className="card highlight">
              You are in team <strong>{teams.myTeamName}</strong> for this hackathon.{' '}
              <Link to={'/teams/' + teams.myTeamId}>Open your team page →</Link>
            </div>
          )}

          {teams.myTeamId === null && (
            <div className="card">
              <div className="card-title-row">
                <h3>Start your own team</h3>
                {!showCreate && (
                  <button className="btn" type="button" disabled={Boolean(teams.joinBlockedReason)} onClick={() => setShowCreate(true)}>
                    Create team
                  </button>
                )}
              </div>
              {teams.joinBlockedReason && <p className="hint">{teams.joinBlockedReason}</p>}
              {showCreate && (
                <TeamForm initial={EMPTY_TEAM} submitLabel="Create team" busy={busy} disabledReason={teams.joinBlockedReason} onSave={handleCreate} />
              )}
            </div>
          )}

          {teams.myTeamId === null && (
            <div className="card">
              <div className="card-title-row">
                <h3>Looking for a team?</h3>
                {teams.lookingForTeam ? (
                  <button className="btn btn-secondary" type="button" disabled={busy} onClick={() => handleLooking(false)}>
                    Stop looking
                  </button>
                ) : (
                  <button className="btn" type="button" disabled={busy || Boolean(teams.joinBlockedReason)} onClick={() => handleLooking(true)}>
                    I'm looking for a team
                  </button>
                )}
              </div>
              {teams.lookingForTeam ? (
                <p className="success-text">
                  You are on the list. Team leaders can see your name, college, branch, year and skills, and invite you.
                  Invites show up in My Requests.
                </p>
              ) : (
                <p className="muted small">
                  Put yourself on a list that team leaders of this hackathon can browse. Only your name, college, branch,
                  year and skills are shown. You leave the list automatically when you join a team.
                </p>
              )}
              {!teams.lookingForTeam && teams.joinBlockedReason && <p className="hint">{teams.joinBlockedReason}</p>}
              <p className="muted small">
                {teams.seekerCount} {teams.seekerCount === 1 ? 'student is' : 'students are'} looking for a team in this hackathon.
              </p>
            </div>
          )}

          <h3>Open teams ({teams.openTeams.length})</h3>
          {teams.openTeams.length === 0 && <div className="card muted">No open teams yet. Why not start one?</div>}
          <div className="card-grid">
            {teams.openTeams.map((team) => {
              let applyReason = teams.joinBlockedReason;
              if (!applyReason && team.pendingWithMe) {
                applyReason = 'You already have a pending request with this team';
              }
              return (
                <div key={team.id} className="card">
                  <div className="card-title-row">
                    <h3>
                      <Link to={'/teams/' + team.id}>{team.name}</Link>
                    </h3>
                    <span className="match" title="How many of the skills this team wants you have">
                      {team.matchPercent}% match
                    </span>
                  </div>
                  <p className="muted small">
                    Led by {team.leaderName} · {team.memberCount} of {team.maxTeamSize} members
                  </p>
                  {team.description && <p>{team.description}</p>}
                  <p>
                    <span className="muted small">Looking for: </span>
                    <SkillTags skills={team.lookingFor} />
                  </p>
                  <button className="btn btn-small" type="button" disabled={Boolean(applyReason)} onClick={() => handleApply(team)}>
                    Apply to join
                  </button>
                  {applyReason && <div className="hint">{applyReason}</div>}
                </div>
              );
            })}
          </div>
        </>
      )}
    </>
  );
}
