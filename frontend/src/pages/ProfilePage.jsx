// "My Profile" for students: shows the user code (used for invites), lets you edit your details
// and upload a profile picture (JPEG or PNG, up to 2 MB).
import { useContext, useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { api, apiDownload } from '../api.js';
import { AuthContext } from '../AuthContext.jsx';
import MessageBar from '../components/MessageBar.jsx';

const MAX_PICTURE_BYTES = 2 * 1024 * 1024;

export default function ProfilePage() {
  const { updateUser } = useContext(AuthContext);
  const location = useLocation();

  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState(null);
  const [pictureUrl, setPictureUrl] = useState('');
  const [pictureVersion, setPictureVersion] = useState(0);
  const [message, setMessage] = useState(location.state && location.state.message ? location.state.message : '');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);

  function showProfile(data) {
    setProfile(data);
    setForm({
      name: data.name,
      rollNumber: data.rollNumber || '',
      branch: data.branch || '',
      year: String(data.year || 1),
      skills: data.skills || '',
    });
  }

  useEffect(() => {
    api
      .get('/profile')
      .then((res) => showProfile(res.data))
      .catch((err) => setError(err.message));
  }, []);

  // Download the picture as a Blob (it needs our JWT header) and show it through a temporary URL.
  // Runs again after each upload because pictureVersion changes.
  const profileId = profile ? profile.id : null;
  const hasPicture = profile ? profile.hasPicture : false;
  useEffect(() => {
    if (profileId === null || !hasPicture) {
      return;
    }
    let objectUrl = '';
    let cancelled = false;
    apiDownload('/users/' + profileId + '/picture')
      .then((blob) => {
        if (!cancelled) {
          objectUrl = URL.createObjectURL(blob);
          setPictureUrl(objectUrl);
        }
      })
      .catch((err) => setError(err.message));
    // Clean-up: free the temporary URL when it is replaced or the page closes.
    return () => {
      cancelled = true;
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [profileId, hasPicture, pictureVersion]);

  function handleChange(event) {
    setForm({ ...form, [event.target.name]: event.target.value });
  }

  async function handleSave(event) {
    event.preventDefault();
    setMessage('');
    setError('');
    setSaving(true);
    try {
      const res = await api.put('/profile', { ...form, year: Number(form.year) });
      showProfile(res.data);
      updateUser({ name: res.data.name });
      setMessage(res.message);
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handlePictureChosen(event) {
    const file = event.target.files[0];
    event.target.value = ''; // so choosing the same file again still triggers a change
    if (!file) {
      return;
    }
    setMessage('');
    setError('');
    // Quick checks here save a slow upload; the backend checks the same rules again.
    if (file.type !== 'image/jpeg' && file.type !== 'image/png') {
      setError('Only JPEG or PNG pictures are allowed');
      return;
    }
    if (file.size > MAX_PICTURE_BYTES) {
      setError('Picture is too large (max 2 MB)');
      return;
    }

    const formData = new FormData();
    formData.append('file', file);
    setUploading(true);
    try {
      const res = await api.post('/profile/picture', formData);
      showProfile(res.data);
      setPictureVersion(pictureVersion + 1);
      setMessage(res.message);
    } catch (err) {
      setError(err.message);
    } finally {
      setUploading(false);
    }
  }

  async function copyUserCode() {
    try {
      await navigator.clipboard.writeText(profile.userCode);
      setMessage('User code copied');
    } catch (err) {
      setError('Could not copy. Please select the code and copy it by hand.');
    }
  }

  if (profile === null) {
    return (
      <>
        <MessageBar type="error" text={error} />
        <p className="muted">Loading profile...</p>
      </>
    );
  }

  return (
    <>
      <MessageBar type="success" text={message} />
      <MessageBar type="error" text={error} />

      <div className="card profile-header">
        {pictureUrl ? (
          <img className="avatar" src={pictureUrl} alt={profile.name} />
        ) : (
          <div className="avatar avatar-empty">{profile.name.charAt(0).toUpperCase()}</div>
        )}
        <div className="profile-info">
          <h2>{profile.name}</h2>
          <p className="muted">
            {profile.email} · {profile.collegeName}
          </p>
          <p>
            Your user code: <span className="user-code">{profile.userCode}</span>{' '}
            <button className="btn btn-secondary btn-small" type="button" onClick={copyUserCode}>
              Copy
            </button>
          </p>
          <p className="muted">Team leaders type this code to invite you.</p>
          <label className={uploading ? 'btn btn-secondary btn-small disabled' : 'btn btn-secondary btn-small'}>
            {uploading ? 'Uploading...' : profile.hasPicture ? 'Change picture' : 'Upload picture'}
            <input type="file" accept="image/jpeg,image/png" hidden disabled={uploading} onChange={handlePictureChosen} />
          </label>
          <span className="hint">JPEG or PNG, up to 2 MB</span>
        </div>
      </div>

      <form className="card" onSubmit={handleSave}>
        <h3>Edit details</h3>
        <div className="form-row">
          <label htmlFor="name">Full name</label>
          <input id="name" name="name" required maxLength={100} value={form.name} onChange={handleChange} />
        </div>
        <div className="form-grid">
          <div className="form-row">
            <label htmlFor="rollNumber">Roll number</label>
            <input id="rollNumber" name="rollNumber" required maxLength={30} value={form.rollNumber} onChange={handleChange} />
          </div>
          <div className="form-row">
            <label htmlFor="branch">Branch</label>
            <input id="branch" name="branch" required maxLength={60} value={form.branch} onChange={handleChange} />
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
        <button className="btn" type="submit" disabled={saving}>
          {saving ? 'Saving...' : 'Save'}
        </button>
      </form>
    </>
  );
}
