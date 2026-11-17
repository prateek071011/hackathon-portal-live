// Remembers who is logged in so every page and the nav bar can read it.
// Pages use it with: const { user, login, logout } = useContext(AuthContext);
import { createContext, useState } from 'react';
import { getToken, getSavedUser, saveSession, clearSession } from './api.js';

export const AuthContext = createContext(null);

// Where each role lands after login.
export function homePathFor(role) {
  if (role === 'ADMIN') {
    return '/admin/hackathons';
  }
  if (role === 'COLLEGE') {
    return '/college/queue';
  }
  return '/hackathons';
}

export function AuthProvider({ children }) {
  // Start from what localStorage remembers, so a page reload keeps you logged in.
  const [token, setToken] = useState(getToken());
  const [user, setUser] = useState(getToken() ? getSavedUser() : null);

  function login(newToken, newUser) {
    saveSession(newToken, newUser);
    setToken(newToken);
    setUser(newUser);
  }

  function logout() {
    clearSession();
    setToken(null);
    setUser(null);
  }

  // After a profile edit, keep the name in the nav bar up to date.
  function updateUser(changes) {
    const updated = { ...user, ...changes };
    saveSession(token, updated);
    setUser(updated);
  }

  return (
    <AuthContext.Provider value={{ user, token, login, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
}
