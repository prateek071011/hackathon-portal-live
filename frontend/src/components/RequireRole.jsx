// Route guard: shows the page only to a logged-in user with the right role.
// Anyone else is sent to the login page with a short reason.
// (The backend checks roles too; this guard only keeps people away from pages that would fail.)
import { useContext } from 'react';
import { Navigate } from 'react-router-dom';
import { AuthContext } from '../AuthContext.jsx';

export default function RequireRole({ role, children }) {
  const { user } = useContext(AuthContext);

  if (user === null) {
    return <Navigate to="/login" replace state={{ message: 'Please log in first.' }} />;
  }
  if (user.role !== role) {
    const message = 'That page is only for ' + role.toLowerCase() + ' accounts.';
    return <Navigate to="/login" replace state={{ message: message }} />;
  }
  return children;
}
