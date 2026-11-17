// Page layout (nav bar on top) and the list of routes: URL -> page.
// <RequireRole> guards each private page: the wrong role (or nobody) is sent back to login.
import { useContext } from 'react';
import { Routes, Route, Link, Navigate } from 'react-router-dom';
import { AuthContext, homePathFor } from './AuthContext.jsx';
import NavBar from './components/NavBar.jsx';
import RequireRole from './components/RequireRole.jsx';
import LoginPage from './pages/LoginPage.jsx';
import SignupPage from './pages/SignupPage.jsx';
import ProfilePage from './pages/ProfilePage.jsx';
import StatusPage from './pages/StatusPage.jsx';
import HackathonListPage from './pages/HackathonListPage.jsx';
import HackathonDetailPage from './pages/HackathonDetailPage.jsx';
import AdminHackathonsPage from './pages/admin/AdminHackathonsPage.jsx';
import AdminCollegesPage from './pages/admin/AdminCollegesPage.jsx';
import AdminTeamsPage from './pages/admin/AdminTeamsPage.jsx';
import TeamPage from './pages/TeamPage.jsx';
import MyRequestsPage from './pages/MyRequestsPage.jsx';
import NotificationsPage from './pages/NotificationsPage.jsx';
import CollegeQueuePage from './pages/college/CollegeQueuePage.jsx';
import CollegeReviewPage from './pages/college/CollegeReviewPage.jsx';
import CollegeRegisteredPage from './pages/college/CollegeRegisteredPage.jsx';

// "/" sends you to your role's home page, or to login.
function HomeRedirect() {
  const { user } = useContext(AuthContext);
  if (user === null) {
    return <Navigate to="/login" replace />;
  }
  return <Navigate to={homePathFor(user.role)} replace />;
}

function NotFoundPage() {
  return (
    <div className="card">
      <h2>Page not found</h2>
      <p>
        <Link to="/">Go to the start page</Link>
      </p>
    </div>
  );
}

export default function App() {
  return (
    <>
      <NavBar />
      <main className="container">
        <Routes>
          <Route path="/" element={<HomeRedirect />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/status" element={<StatusPage />} />

          {/* Student pages */}
          <Route path="/hackathons" element={<RequireRole role="STUDENT"><HackathonListPage /></RequireRole>} />
          <Route path="/hackathons/:id" element={<RequireRole role="STUDENT"><HackathonDetailPage /></RequireRole>} />
          <Route path="/teams/:id" element={<RequireRole role="STUDENT"><TeamPage /></RequireRole>} />
          <Route path="/requests" element={<RequireRole role="STUDENT"><MyRequestsPage /></RequireRole>} />
          <Route path="/notifications" element={<RequireRole role="STUDENT"><NotificationsPage /></RequireRole>} />
          <Route path="/profile" element={<RequireRole role="STUDENT"><ProfilePage /></RequireRole>} />

          {/* College pages */}
          <Route path="/college/queue" element={<RequireRole role="COLLEGE"><CollegeQueuePage /></RequireRole>} />
          <Route path="/college/registrations/:id" element={<RequireRole role="COLLEGE"><CollegeReviewPage /></RequireRole>} />
          <Route path="/college/registered" element={<RequireRole role="COLLEGE"><CollegeRegisteredPage /></RequireRole>} />

          {/* Admin pages */}
          <Route path="/admin/hackathons" element={<RequireRole role="ADMIN"><AdminHackathonsPage /></RequireRole>} />
          <Route path="/admin/colleges" element={<RequireRole role="ADMIN"><AdminCollegesPage /></RequireRole>} />
          <Route path="/admin/teams" element={<RequireRole role="ADMIN"><AdminTeamsPage /></RequireRole>} />

          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
    </>
  );
}
