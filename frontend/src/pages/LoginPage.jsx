// Login form for every role. On success the token is saved and you go to your role's home page.
import { useContext, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { AuthContext, homePathFor } from '../AuthContext.jsx';
import MessageBar from '../components/MessageBar.jsx';

export default function LoginPage() {
  const { user, login } = useContext(AuthContext);
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  // A route guard may have sent us here with a reason ("Please log in first.").
  let notice = '';
  if (location.state && location.state.message) {
    notice = location.state.message;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setBusy(true);
    try {
      const res = await api.post('/auth/login', { email: email, password: password });
      login(res.data.token, res.data.user);
      navigate(homePathFor(res.data.user.role));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="narrow">
      <MessageBar type="info" text={notice} />
      <MessageBar type="error" text={error} />

      {user !== null && (
        <div className="card">
          You are logged in as <strong>{user.name}</strong> ({user.role.toLowerCase()}).{' '}
          <Link to={homePathFor(user.role)}>Go to your home page</Link>
        </div>
      )}

      <form className="card" onSubmit={handleSubmit}>
        <h2>Log in</h2>
        <div className="form-row">
          <label htmlFor="email">Email</label>
          <input id="email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div className="form-row">
          <label htmlFor="password">Password</label>
          <input id="password" type="password" required value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>
        <button className="btn" type="submit" disabled={busy}>
          {busy ? 'Logging in...' : 'Log in'}
        </button>
        <p className="muted">
          New student? <Link to="/signup">Create an account</Link>
        </p>
      </form>
    </div>
  );
}
