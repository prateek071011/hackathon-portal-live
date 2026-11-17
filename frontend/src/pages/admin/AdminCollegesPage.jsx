// Admin: list colleges and create a new college together with its reviewer login.
import { useEffect, useState } from 'react';
import { api } from '../../api.js';
import MessageBar from '../../components/MessageBar.jsx';

const EMPTY_FORM = { name: '', reviewerName: '', email: '', password: '' };

export default function AdminCollegesPage() {
  const [colleges, setColleges] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  function loadColleges() {
    api
      .get('/admin/colleges')
      .then((res) => setColleges(res.data))
      .catch((err) => setError(err.message));
  }

  useEffect(() => {
    loadColleges();
  }, []);

  function handleChange(event) {
    setForm({ ...form, [event.target.name]: event.target.value });
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setMessage('');
    setError('');
    setBusy(true);
    try {
      const res = await api.post('/admin/colleges', form);
      setMessage(res.message + '. The reviewer can now log in with ' + res.data.reviewerEmail + '.');
      setForm(EMPTY_FORM);
      loadColleges();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <h2>Manage colleges</h2>
      <MessageBar type="success" text={message} />
      <MessageBar type="error" text={error} />

      <form className="card" onSubmit={handleSubmit}>
        <h3>Add a college</h3>
        <p className="muted">This also creates the login that reviews this college's teams.</p>
        <div className="form-grid two">
          <div className="form-row">
            <label htmlFor="name">College name</label>
            <input id="name" name="name" required maxLength={150} value={form.name} onChange={handleChange} />
          </div>
          <div className="form-row">
            <label htmlFor="reviewerName">Reviewer name</label>
            <input id="reviewerName" name="reviewerName" required maxLength={100} value={form.reviewerName} onChange={handleChange} />
          </div>
          <div className="form-row">
            <label htmlFor="email">Reviewer login email</label>
            <input id="email" name="email" type="email" required maxLength={150} value={form.email} onChange={handleChange} />
          </div>
          <div className="form-row">
            <label htmlFor="password">Reviewer password (at least 8 characters)</label>
            <input id="password" name="password" type="password" required minLength={8} maxLength={72} value={form.password} onChange={handleChange} />
          </div>
        </div>
        <button className="btn" type="submit" disabled={busy}>
          {busy ? 'Creating...' : 'Create college'}
        </button>
      </form>

      <div className="card">
        <h3>All colleges ({colleges.length})</h3>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>College</th>
                <th>Reviewer</th>
                <th>Login email</th>
              </tr>
            </thead>
            <tbody>
              {colleges.map((c) => (
                <tr key={c.id}>
                  <td>{c.name}</td>
                  <td>{c.reviewerName || <span className="muted">none</span>}</td>
                  <td>{c.reviewerEmail || <span className="muted">none</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}
