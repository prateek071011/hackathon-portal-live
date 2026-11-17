// Admin: create, edit and delete hackathons, and import many at once from a CSV file.
import { useEffect, useState } from 'react';
import { api } from '../../api.js';
import { formatDate, teamSizeText } from '../../format.js';
import MessageBar from '../../components/MessageBar.jsx';
import StatusBadge from '../../components/StatusBadge.jsx';
import HackathonForm, { EMPTY_HACKATHON } from '../../components/HackathonForm.jsx';

const CSV_HEADER = 'title,organizer,description,startDate,endDate,registrationDeadline,minTeamSize,maxTeamSize';
const CSV_EXAMPLE = 'Example Hack,Tech Club,"Build anything, in 24 hours",2027-02-20,2027-02-21,2027-02-10,2,4';

// The form works with text values, so numbers are turned into strings here.
function toFormValues(h) {
  return {
    title: h.title,
    organizer: h.organizer,
    description: h.description || '',
    startDate: h.startDate,
    endDate: h.endDate,
    registrationDeadline: h.registrationDeadline,
    minTeamSize: String(h.minTeamSize),
    maxTeamSize: String(h.maxTeamSize),
  };
}

export default function AdminHackathonsPage() {
  const [hackathons, setHackathons] = useState([]);
  const [editing, setEditing] = useState(null); // the hackathon being edited, or null when creating
  const [formKey, setFormKey] = useState(0); // a new key makes React build a fresh form
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const [csvFile, setCsvFile] = useState(null);
  const [importing, setImporting] = useState(false);
  const [importErrors, setImportErrors] = useState([]);

  function loadHackathons() {
    api
      .get('/hackathons')
      .then((res) => setHackathons(res.data))
      .catch((err) => setError(err.message));
  }

  useEffect(() => {
    loadHackathons();
  }, []);

  function clearMessages() {
    setMessage('');
    setError('');
  }

  function resetForm() {
    setEditing(null);
    setFormKey(formKey + 1);
  }

  async function handleSave(values) {
    clearMessages();
    setBusy(true);
    try {
      let res;
      if (editing !== null) {
        res = await api.put('/admin/hackathons/' + editing.id, values);
      } else {
        res = await api.post('/admin/hackathons', values);
      }
      setMessage(res.message);
      resetForm();
      loadHackathons();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  function startEdit(hackathon) {
    clearMessages();
    setEditing(hackathon);
    setFormKey(formKey + 1);
    window.scrollTo(0, 0);
  }

  async function handleDelete(hackathon) {
    if (!window.confirm('Delete "' + hackathon.title + '"? This cannot be undone.')) {
      return;
    }
    clearMessages();
    try {
      const res = await api.del('/admin/hackathons/' + hackathon.id);
      setMessage(res.message);
      if (editing !== null && editing.id === hackathon.id) {
        resetForm();
      }
      loadHackathons();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleImport(event) {
    event.preventDefault();
    clearMessages();
    setImportErrors([]);
    setImporting(true);
    const formData = new FormData();
    formData.append('file', csvFile);
    try {
      const res = await api.post('/admin/hackathons/import', formData);
      setMessage(res.message);
      setImportErrors(res.data.errors);
      loadHackathons();
    } catch (err) {
      setError(err.message);
    } finally {
      setImporting(false);
    }
  }

  // Builds a small CSV in the browser and saves it, so the admin starts from the right columns.
  function downloadTemplate() {
    const text = CSV_HEADER + '\n' + CSV_EXAMPLE + '\n';
    const url = URL.createObjectURL(new Blob([text], { type: 'text/csv' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = 'hackathons-template.csv';
    link.click();
    URL.revokeObjectURL(url);
  }

  let importHint = '';
  if (csvFile === null) {
    importHint = 'Choose a CSV file first';
  }

  return (
    <>
      <h2>Manage hackathons</h2>
      <MessageBar type="success" text={message} />
      <MessageBar type="error" text={error} />

      <div className="card">
        <h3>{editing !== null ? 'Edit: ' + editing.title : 'Add a hackathon'}</h3>
        <HackathonForm
          key={formKey}
          initial={editing !== null ? toFormValues(editing) : EMPTY_HACKATHON}
          editing={editing !== null}
          sizesLocked={editing !== null && editing.teamCount > 0}
          busy={busy}
          onSave={handleSave}
          onCancel={resetForm}
        />
      </div>

      <div className="card">
        <h3>Import from CSV</h3>
        <p className="muted">
          First line must be <code>{CSV_HEADER}</code>. Dates look like 2027-02-20. Good lines are saved together;
          bad lines are skipped and listed below.{' '}
          <button className="link-button" type="button" onClick={downloadTemplate}>
            Download a template
          </button>
        </p>
        <form className="actions" onSubmit={handleImport}>
          <input type="file" accept=".csv,text/csv" onChange={(e) => setCsvFile(e.target.files[0] || null)} />
          <button className="btn" type="submit" disabled={csvFile === null || importing}>
            {importing ? 'Importing...' : 'Import'}
          </button>
          {importHint && <span className="hint">{importHint}</span>}
        </form>
        {importErrors.length > 0 && (
          <div className="error-list">
            <strong>Skipped lines:</strong>
            <ul>
              {importErrors.map((line) => (
                <li key={line}>{line}</li>
              ))}
            </ul>
          </div>
        )}
      </div>

      <div className="card">
        <h3>All hackathons ({hackathons.length})</h3>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Registration closes</th>
                <th>Event</th>
                <th>Size</th>
                <th>Status</th>
                <th>Teams</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {hackathons.map((h) => (
                <tr key={h.id}>
                  <td>
                    <strong>{h.title}</strong>
                    <div className="muted small">{h.organizer}</div>
                  </td>
                  <td>
                    {formatDate(h.registrationDeadline)}
                    <div className="muted small">{h.deadlineText}</div>
                  </td>
                  <td>
                    {formatDate(h.startDate)} – {formatDate(h.endDate)}
                  </td>
                  <td>{teamSizeText(h.minTeamSize, h.maxTeamSize)}</td>
                  <td>
                    <StatusBadge status={h.status} />
                  </td>
                  <td>{h.teamCount}</td>
                  <td className="row-actions">
                    <button className="btn btn-secondary btn-small" type="button" onClick={() => startEdit(h)}>
                      Edit
                    </button>
                    <button
                      className="btn btn-danger btn-small"
                      type="button"
                      disabled={h.teamCount > 0}
                      onClick={() => handleDelete(h)}
                    >
                      Delete
                    </button>
                    {h.teamCount > 0 && <div className="hint">Has {h.teamCount} {h.teamCount === 1 ? 'team' : 'teams'}</div>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}
