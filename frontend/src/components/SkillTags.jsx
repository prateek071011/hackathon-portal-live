// Shows a comma-separated skill list ("java, react") as small rounded tags.
export default function SkillTags({ skills }) {
  if (!skills) {
    return <span className="muted small">none listed</span>;
  }
  return (
    <span className="tags">
      {skills.split(',').map((skill) => (
        <span key={skill.trim()} className="tag">
          {skill.trim()}
        </span>
      ))}
    </span>
  );
}
