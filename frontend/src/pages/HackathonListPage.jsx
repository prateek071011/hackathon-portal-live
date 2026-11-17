// Student home page: every hackathon as a card. Open ones come first (the backend sorts them).
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { formatDate, teamSizeText } from '../format.js';
import MessageBar from '../components/MessageBar.jsx';
import StatusBadge from '../components/StatusBadge.jsx';

export default function HackathonListPage() {
  const [hackathons, setHackathons] = useState(null);
  const [myTeams, setMyTeams] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get('/hackathons')
      .then((res) => setHackathons(res.data))
      .catch((err) => setError(err.message));
    api
      .get('/teams/mine')
      .then((res) => setMyTeams(res.data))
      .catch((err) => setError(err.message));
  }, []);

  return (
    <>
      <h2>Hackathons</h2>
      <MessageBar type="error" text={error} />

      {myTeams.length > 0 && (
        <div className="card highlight">
          <h3>Your teams</h3>
          <ul className="plain-list">
            {myTeams.map((t) => (
              <li key={t.id}>
                <Link to={'/teams/' + t.id}>{t.name}</Link> <StatusBadge status={t.status} />{' '}
                <span className="muted small">
                  {t.hackathonTitle}
                  {t.leader ? ' · you lead it' : ''}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}
      {hackathons === null && !error && <p className="muted">Loading...</p>}
      {hackathons !== null && hackathons.length === 0 && (
        <div className="card muted">No hackathons yet. Check back soon.</div>
      )}

      <div className="card-grid">
        {hackathons !== null &&
          hackathons.map((h) => (
            <Link key={h.id} to={'/hackathons/' + h.id} className="card card-link">
              <div className="card-title-row">
                <h3>{h.title}</h3>
                <StatusBadge status={h.status} />
              </div>
              <p className="muted">by {h.organizer}</p>
              <p>
                {formatDate(h.startDate)} – {formatDate(h.endDate)}
              </p>
              <p className="muted">
                {teamSizeText(h.minTeamSize, h.maxTeamSize)} · {h.teamCount} teams so far
              </p>
              <p className={h.status === 'OPEN' ? 'deadline open' : 'deadline'}>{h.deadlineText}</p>
            </Link>
          ))}
      </div>
    </>
  );
}
