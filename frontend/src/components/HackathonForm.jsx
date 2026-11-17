// The create / edit hackathon form used on the admin page.
// Props: initial (the values to start with), editing (true = "Save changes"), sizesLocked (hackathon already
// has teams, so sizes cannot change), busy, onSave(values), onCancel().
// The parent gives it a new "key" to start over with fresh values (React then builds a new form).
import { useState } from 'react';

export const EMPTY_HACKATHON = {
  title: '',
  organizer: '',
  description: '',
  startDate: '',
  endDate: '',
  registrationDeadline: '',
  minTeamSize: '2',
  maxTeamSize: '4',
};

export default function HackathonForm({ initial, editing, sizesLocked, busy, onSave, onCancel }) {
  const [form, setForm] = useState(initial);

  function handleChange(event) {
    setForm({ ...form, [event.target.name]: event.target.value });
  }

  function handleSubmit(event) {
    event.preventDefault();
    onSave({ ...form, minTeamSize: Number(form.minTeamSize), maxTeamSize: Number(form.maxTeamSize) });
  }

  return (
    <form onSubmit={handleSubmit}>
      <div className="form-grid two">
        <div className="form-row">
          <label htmlFor="title">Title</label>
          <input id="title" name="title" required maxLength={150} value={form.title} onChange={handleChange} />
        </div>
        <div className="form-row">
          <label htmlFor="organizer">Organizer</label>
          <input id="organizer" name="organizer" required maxLength={150} value={form.organizer} onChange={handleChange} />
        </div>
      </div>
      <div className="form-row">
        <label htmlFor="description">Description</label>
        <textarea id="description" name="description" rows={3} maxLength={5000} value={form.description} onChange={handleChange} />
      </div>
      <div className="form-grid">
        <div className="form-row">
          <label htmlFor="registrationDeadline">Registration deadline</label>
          <input id="registrationDeadline" name="registrationDeadline" type="date" required value={form.registrationDeadline} onChange={handleChange} />
        </div>
        <div className="form-row">
          <label htmlFor="startDate">Start date</label>
          <input id="startDate" name="startDate" type="date" required value={form.startDate} onChange={handleChange} />
        </div>
        <div className="form-row">
          <label htmlFor="endDate">End date</label>
          <input id="endDate" name="endDate" type="date" required value={form.endDate} onChange={handleChange} />
        </div>
        <div className="form-row">
          <label htmlFor="minTeamSize">Min team size</label>
          <input id="minTeamSize" name="minTeamSize" type="number" min={1} max={6} required disabled={sizesLocked} value={form.minTeamSize} onChange={handleChange} />
        </div>
        <div className="form-row">
          <label htmlFor="maxTeamSize">Max team size</label>
          <input id="maxTeamSize" name="maxTeamSize" type="number" min={1} max={6} required disabled={sizesLocked} value={form.maxTeamSize} onChange={handleChange} />
        </div>
      </div>
      {sizesLocked && <p className="hint">Team sizes are fixed because this hackathon already has teams.</p>}
      <div className="actions">
        <button className="btn" type="submit" disabled={busy}>
          {busy ? 'Saving...' : editing ? 'Save changes' : 'Create hackathon'}
        </button>
        {editing && (
          <button className="btn btn-secondary" type="button" onClick={onCancel}>
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}
