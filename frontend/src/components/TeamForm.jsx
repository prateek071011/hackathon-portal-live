// Create / edit team form: name, description and the skills the team is looking for.
// Props: initial values, submitLabel, busy, disabledReason (shown instead of submitting), onSave(values).
// The parent gives it a new "key" to start over with fresh values.
import { useState } from 'react';

export const EMPTY_TEAM = { name: '', description: '', lookingFor: '' };

export default function TeamForm({ initial, submitLabel, busy, disabledReason, onSave }) {
  const [form, setForm] = useState(initial);

  function handleChange(event) {
    setForm({ ...form, [event.target.name]: event.target.value });
  }

  function handleSubmit(event) {
    event.preventDefault();
    onSave(form);
  }

  const disabled = busy || Boolean(disabledReason);

  return (
    <form onSubmit={handleSubmit}>
      <div className="form-row">
        <label htmlFor="team-name">Team name</label>
        <input id="team-name" name="name" required maxLength={100} disabled={Boolean(disabledReason)} value={form.name} onChange={handleChange} />
      </div>
      <div className="form-row">
        <label htmlFor="team-description">What will you build?</label>
        <textarea id="team-description" name="description" rows={2} maxLength={1000} disabled={Boolean(disabledReason)} value={form.description} onChange={handleChange} />
      </div>
      <div className="form-row">
        <label htmlFor="team-lookingFor">Looking for (skills, comma-separated)</label>
        <input id="team-lookingFor" name="lookingFor" maxLength={500} placeholder="react, figma, sql" disabled={Boolean(disabledReason)} value={form.lookingFor} onChange={handleChange} />
      </div>
      <button className="btn" type="submit" disabled={disabled}>
        {busy ? 'Saving...' : submitLabel}
      </button>
      {disabledReason && <span className="hint">{disabledReason}</span>}
    </form>
  );
}
