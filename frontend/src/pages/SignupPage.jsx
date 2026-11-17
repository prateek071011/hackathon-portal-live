// Student signup form. The college dropdown is filled from the backend.
// After signup the student is logged in straight away and sees their new user code on the profile page.
import { useContext, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { AuthContext } from '../AuthContext.jsx';
import MessageBar from '../components/MessageBar.jsx';

const EMPTY_FORM = {
  name: '',
  email: '',
  password: '',
  collegeId: '',
  rollNumber: '',
  branch: '',
  year: '1',
  skills: '',
};

export default function SignupPage() {
  const { login } = useContext(AuthContext);
  const navigate = useNavigate();

  const [form, setForm] = useState(EMPTY_FORM);
  const [colleges, setColleges] = useState([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api
      .get('/auth/colleges')
      .then((res) => setColleges(res.data))
      .catch((err) => setError(err.message));
  }, []);

  // One handler for every field: the input's "name" says which form value to change.
  function handleChange(event) {
    setForm({ ...form, [event.target.name]: event.target.value });
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setBusy(true);
    try {
      const body = { ...form, collegeId: Number(form.collegeId), year: Number(form.year) };
      const res = await api.post('/auth/signup', body);
      login(res.data.token, res.data.user);
      navigate('/profile', { state: { message: res.message } });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const noColleges = colleges.length === 0;

  return (
    <div className="narrow">
      <MessageBar type="error" text={error} />
      <form className="card" onSubmit={handleSubmit}>
        <h2>Student signup</h2>

        <div className="form-row">
          <label htmlFor="name">Full name</label>
          <input id="name" name="name" required maxLength={100} value={form.name} onChange={handleChange} />
        </div>
        <div className="form-row">
          <label htmlFor="email">Email</label>
          <input id="email" name="email" type="email" required maxLength={150} value={form.email} onChange={handleChange} />
        </div>
        <div className="form-row">
          <label htmlFor="password">Password (at least 8 characters)</label>
          <input id="password" name="password" type="password" required minLength={8} maxLength={72} value={form.password} onChange={handleChange} />
        </div>
        <div className="form-row">
          <label htmlFor="collegeId">College</label>
          <select id="collegeId" name="collegeId" required value={form.collegeId} onChange={handleChange}>
            <option value="">Choose your college</option>
            {colleges.map((college) => (
              <option key={college.id} value={college.id}>
                {college.name}
              </option>
            ))}
          </select>
        </div>
        <div className="form-grid">
          <div className="form-row">
            <label htmlFor="rollNumber">Roll number</label>
            <input id="rollNumber" name="rollNumber" required maxLength={30} value={form.rollNumber} onChange={handleChange} />
          </div>
          <div className="form-row">
            <label htmlFor="branch">Branch</label>
            <input id="branch" name="branch" required maxLength={60} placeholder="e.g. CSE" value={form.branch} onChange={handleChange} />
          </div>
          <div className="form-row">
            <label htmlFor="year">Year</label>
            <select id="year" name="year" value={form.year} onChange={handleChange}>
              <option value="1">1st year</option>
              <option value="2">2nd year</option>
              <option value="3">3rd year</option>
              <option value="4">4th year</option>
              <option value="5">5th year</option>
            </select>
          </div>
        </div>
        <div className="form-row">
          <label htmlFor="skills">Skills (comma-separated)</label>
          <input id="skills" name="skills" maxLength={500} placeholder="java, react, sql" value={form.skills} onChange={handleChange} />
        </div>

        <button className="btn" type="submit" disabled={busy || noColleges}>
          {busy ? 'Creating account...' : 'Create account'}
        </button>
        {noColleges && <span className="hint">No colleges yet. Ask the admin to add yours.</span>}
        <p className="muted">
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </form>
    </div>
  );
}
