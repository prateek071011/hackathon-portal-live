// The one place that talks to the backend. Every page calls api.get / api.post / ...
// It adds the login token (JWT) if we have one, sends and reads JSON, and if the backend
// says the request failed it throws an Error carrying the backend's message.
// It also remembers the session (token + user) in localStorage so a page reload keeps you logged in.

const API_BASE = 'http://localhost:8080/api';
const TOKEN_KEY = 'htf_token';
const USER_KEY = 'htf_user';

// ---------- Session (token + user) ----------

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getSavedUser() {
  const text = localStorage.getItem(USER_KEY);
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch (error) {
    return null;
  }
}

export function saveSession(token, user) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

// ---------- Requests ----------

async function send(path, method, body) {
  const headers = {};
  const token = getToken();
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }

  let requestBody = undefined;
  if (body instanceof FormData) {
    // File uploads: the browser sets the multipart Content-Type itself.
    requestBody = body;
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    requestBody = JSON.stringify(body);
  }

  try {
    return await fetch(API_BASE + path, { method: method, headers: headers, body: requestBody });
  } catch (networkError) {
    throw new Error('Cannot reach the server. Is the backend running?');
  }
}

async function readJson(response) {
  try {
    return await response.json();
  } catch (parseError) {
    return null;
  }
}

// Our token expired or is invalid (code UNAUTHORIZED, not a wrong password):
// forget the session and start again from the login page.
function handleExpiredSession(response, json) {
  const code = json && json.data ? json.data.code : '';
  if (response.status === 401 && code === 'UNAUTHORIZED' && getToken()) {
    clearSession();
    window.location.href = '/login';
  }
}

function errorMessage(response, json) {
  if (json && json.message) {
    return json.message;
  }
  return 'Request failed (HTTP ' + response.status + ')';
}

// Returns the whole backend reply: { success, message, data }.
export async function apiRequest(path, method, body) {
  const response = await send(path, method, body);
  const json = await readJson(response);
  if (!response.ok || json === null || !json.success) {
    handleExpiredSession(response, json);
    throw new Error(errorMessage(response, json));
  }
  return json;
}

// For pictures and CSV files: returns the raw file as a Blob instead of JSON.
// (An <img src> cannot send our JWT header, so pictures are fetched here and shown with URL.createObjectURL.)
export async function apiDownload(path) {
  const response = await send(path, 'GET');
  if (!response.ok) {
    const json = await readJson(response);
    handleExpiredSession(response, json);
    throw new Error(errorMessage(response, json));
  }
  return response.blob();
}

// Downloads a file from the backend (with our JWT) and makes the browser save it under fileName.
export async function downloadFile(path, fileName) {
  const blob = await apiDownload(path);
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}

export const api = {
  get: (path) => apiRequest(path, 'GET'),
  post: (path, body) => apiRequest(path, 'POST', body),
  put: (path, body) => apiRequest(path, 'PUT', body),
  del: (path) => apiRequest(path, 'DELETE'),
};
