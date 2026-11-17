// Top bar shown on every page. The links depend on who is logged in (student, college or admin).
// Students also see how many notifications they have not read yet. The count is fetched once each time
// a page opens (no polling). On the Notifications page itself the badge is hidden: you are reading them.
import { useContext, useEffect, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { AuthContext } from '../AuthContext.jsx';

export default function NavBar() {
  const { user, logout } = useContext(AuthContext);
  const navigate = useNavigate();
  const location = useLocation();
  const [unread, setUnread] = useState(0);

  const isStudent = user !== null && user.role === 'STUDENT';

  useEffect(() => {
    if (!isStudent) {
      return;
    }
    api
      .get('/notifications/unread-count')
      .then((res) => setUnread(res.data))
      .catch(() => setUnread(0)); // the badge is a nice extra; never block the page over it
  }, [isStudent, location.pathname]);

  function handleLogout() {
    logout();
    navigate('/login');
  }

  const showBadge = unread > 0 && location.pathname !== '/notifications';

  return (
    <header className="navbar">
      <Link to="/" className="brand">
        Hackathon Team-Finder
      </Link>

      {user === null && (
        <nav className="nav-links">
          <NavLink to="/login">Log in</NavLink>
          <NavLink to="/signup">Sign up</NavLink>
        </nav>
      )}

      {user !== null && (
        <nav className="nav-links">
          {user.role === 'STUDENT' && (
            <>
              <NavLink to="/hackathons">Hackathons</NavLink>
              <NavLink to="/requests">My Requests</NavLink>
              <NavLink to="/notifications">
                Notifications{showBadge && <span className="count-badge">{unread}</span>}
              </NavLink>
              <NavLink to="/profile">My Profile</NavLink>
            </>
          )}
          {user.role === 'COLLEGE' && (
            <>
              <NavLink to="/college/queue">Review queue</NavLink>
              <NavLink to="/college/registered">Registered teams</NavLink>
            </>
          )}
          {user.role === 'ADMIN' && (
            <>
              <NavLink to="/admin/hackathons">Hackathons</NavLink>
              <NavLink to="/admin/colleges">Colleges</NavLink>
              <NavLink to="/admin/teams">Teams</NavLink>
            </>
          )}
          <span className="nav-user">{user.name}</span>
          <button className="btn btn-secondary btn-small" onClick={handleLogout}>
            Log out
          </button>
        </nav>
      )}
    </header>
  );
}
