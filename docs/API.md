# API reference

Base URL: `http://localhost:8080/api`

The samples use the demo data from `data.sql` (see the README for logins; every password is `Demo@1234`).
Ids and dates in your database may differ.

| Area | Endpoints |
|---|---|
| Health | `GET /health` |
| Auth (public) | `POST /auth/signup`, `POST /auth/login`, `GET /auth/colleges` |
| Profile | `GET /profile`, `PUT /profile`, `POST /profile/picture`, `GET /users/{id}/picture` |
| Hackathons | `GET /hackathons`, `GET /hackathons/{id}` |
| Admin: hackathons | `POST /admin/hackathons`, `PUT /admin/hackathons/{id}`, `DELETE /admin/hackathons/{id}`, `POST /admin/hackathons/import` |
| Admin: colleges | `GET /admin/colleges`, `POST /admin/colleges` |
| Teams | `GET /hackathons/{id}/teams`, `POST /hackathons/{id}/teams`, `GET /teams/mine`, `GET /teams/{id}`, `PUT /teams/{id}`, `DELETE /teams/{id}`, `POST /teams/{id}/leave`, `DELETE /teams/{id}/members/{userId}`, `POST /teams/{id}/submit` |
| Looking for a team | `POST /hackathons/{id}/looking-for-team`, `DELETE /hackathons/{id}/looking-for-team`, `GET /teams/{id}/seekers` |
| Requests | `POST /teams/{id}/invites`, `POST /teams/{id}/apply`, `GET /requests/mine`, `POST /requests/{id}/accept`, `POST /requests/{id}/reject`, `POST /requests/{id}/cancel` |
| College | `GET /college/registrations`, `GET /college/registrations/{id}`, `POST /college/registrations/{id}/approve`, `POST /college/registrations/{id}/reject`, `GET /college/registrations/export` |
| Notifications | `GET /notifications`, `GET /notifications/unread-count`, `POST /notifications/read-all` |
| Admin: teams | `GET /admin/teams`, `DELETE /admin/teams/{id}` |

Every response uses the same envelope:

```json
{ "success": true, "message": "OK", "data": { } }
```

Errors use the same envelope with `success: false`. `data` holds the HTTP status and a short error code:

```json
{
  "success": false,
  "message": "Team 'Byte Me' is already full",
  "data": { "status": 409, "code": "TEAM_FULL", "message": "Team 'Byte Me' is already full" }
}
```

| Error code | HTTP | When |
|---|---|---|
| `NOT_FOUND` | 404 | Row or endpoint does not exist |
| `FORBIDDEN` | 403 | Logged in, but not allowed to do this (e.g. not the leader) |
| `CONFLICT` | 409 | Clashes with existing data (duplicate email, team name, ...) |
| `TEAM_FULL` | 409 | Team already has maxTeamSize members |
| `ALREADY_IN_TEAM` | 409 | Student already in a team for this hackathon |
| `RULE_VIOLATION` | 400 | Breaks a business rule |
| `DEADLINE_PASSED` | 400 | After the registration deadline |
| `VALIDATION_FAILED` | 400 | A request field is missing or invalid |
| `BAD_REQUEST` | 400 | Broken JSON or a wrong parameter type |
| `INTERNAL_ERROR` | 500 | Unexpected failure (details only in the server log) |
| `UNAUTHORIZED` | 401 | No token, or the token is invalid / expired (log in again) |
| `BAD_CREDENTIALS` | 401 | Login with a wrong email or password |
| `FILE_TOO_LARGE` | 413 | Upload bigger than Spring's 3 MB hard limit |

## Logging in and access rules

Log in (or sign up) to get a token, then send it on every other request:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

| URL | Who may call it |
|---|---|
| `/api/auth/**`, `/api/health` | Anyone (public) |
| `/api/college/**` | COLLEGE accounts only |
| `/api/admin/**` | ADMIN only |
| everything else | Any logged-in user |

A missing or bad token gives `401 UNAUTHORIZED`. A wrong role gives `403 FORBIDDEN`.
Finer checks (e.g. "are you the team leader?") are done in the services and also give `403 FORBIDDEN`.

## Auth

### POST /api/auth/signup (public)

Creates a STUDENT account and logs it in. Answers `201 Created`.

```json
{
  "name": "Asha Rao", "email": "asha@example.com", "password": "secret123",
  "collegeId": 1, "rollNumber": "21CS001", "branch": "CSE", "year": 3, "skills": "Java, React, SQL"
}
```

```json
{
  "success": true,
  "message": "Welcome! Your user code is HTF-7K3Q9M",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": { "id": 4, "name": "Asha Rao", "email": "asha@example.com", "role": "STUDENT", "collegeId": 1, "userCode": "HTF-7K3Q9M" }
  }
}
```

Errors: `409 CONFLICT` (email already registered), `404 NOT_FOUND` (college), `400 VALIDATION_FAILED`.
The email is stored lower-case. Skills are cleaned to `"java, react, sql"`.

### POST /api/auth/login (public)

```json
{ "email": "admin@portal.test", "password": "Demo@1234" }
```

Same reply shape as signup (`message: "Logged in"`). A wrong email or password gives `401 BAD_CREDENTIALS`
with the same message, so nobody can tell which one was wrong.

### GET /api/auth/colleges (public)

The list for the signup dropdown.

```json
{ "success": true, "message": "OK", "data": [ { "id": 1, "name": "Greenfield Institute of Technology", "createdAt": "2026-11-17T00:18:02" } ] }
```

## Profile

### GET /api/profile (logged in)

```json
{
  "success": true, "message": "OK",
  "data": {
    "id": 4, "name": "Asha Rao", "email": "asha@example.com", "role": "STUDENT", "userCode": "HTF-7K3Q9M",
    "collegeId": 1, "collegeName": "Greenfield Institute of Technology",
    "rollNumber": "21CS001", "branch": "CSE", "year": 3, "skills": "java, react, sql", "hasPicture": false
  }
}
```

### PUT /api/profile (STUDENT)

```json
{ "name": "Asha Rao", "rollNumber": "21CS001", "branch": "CSE", "year": 4, "skills": "python, docker" }
```

Answers `"Profile saved"` with the updated profile. Email, college and user code cannot change.
Other roles get `403 FORBIDDEN`.

### POST /api/profile/picture (logged in)

`multipart/form-data` with one field `file`. JPEG or PNG, at most 2 MB. The first bytes of the file are
checked, so a renamed text file is rejected. Answers `"Picture uploaded"` with the updated profile.
The old picture file is deleted.

Errors: `400 RULE_VIOLATION` ("Only JPEG or PNG pictures are allowed", "The file is not a real JPEG or PNG image",
"Picture is too large (max 2 MB)"), `413 FILE_TOO_LARGE` (over 3 MB).

### GET /api/users/{id}/picture (logged in)

**Not JSON:** returns the raw image bytes with `Content-Type: image/png` or `image/jpeg`.
The frontend downloads it with the token (`apiDownload` in `api.js`) and shows it with `URL.createObjectURL`.
`404 NOT_FOUND` if the user has no picture.

## Hackathons

`status` is worked out from today's date, never stored: `OPEN` (on or before the deadline), `CLOSED`
(after the deadline, before the start), `ONGOING` (start to end), `COMPLETED` (after the end).

### GET /api/hackathons (logged in)

Open hackathons first (closing soonest at the top), then the others (most recent first).

```json
{
  "success": true, "message": "OK",
  "data": [
    {
      "id": 2, "title": "CodeSprint 2026", "organizer": "Greenfield Tech Club",
      "description": "24-hour build sprint. Any stack, any idea.",
      "startDate": "2026-12-12", "endDate": "2026-12-13", "registrationDeadline": "2026-12-05",
      "minTeamSize": 2, "maxTeamSize": 4,
      "status": "OPEN", "deadlineText": "Closes in 18 days", "teamCount": 0
    }
  ]
}
```

### GET /api/hackathons/{id} (logged in)

One hackathon, same shape as above. `404 NOT_FOUND` if it does not exist.

### POST /api/admin/hackathons (ADMIN)

Answers `201 Created` with the new hackathon.

```json
{
  "title": "CodeSprint 2026", "organizer": "Greenfield Tech Club", "description": "Optional text",
  "startDate": "2026-12-12", "endDate": "2026-12-13", "registrationDeadline": "2026-12-05",
  "minTeamSize": 2, "maxTeamSize": 4
}
```

Rules (`400 RULE_VIOLATION` / `VALIDATION_FAILED`): 1 ≤ min ≤ max ≤ 6, deadline before start, start ≤ end.

### PUT /api/admin/hackathons/{id} (ADMIN)

Same body as create. Answers `"Hackathon saved"`. If the hackathon already has teams, changing
`minTeamSize` / `maxTeamSize` gives `400 RULE_VIOLATION` ("Team sizes cannot change once teams exist").

### DELETE /api/admin/hackathons/{id} (ADMIN)

Answers `"Hackathon deleted"` (`data` is null). A hackathon with teams gives `400 RULE_VIOLATION`
("Delete its 3 teams first").

### POST /api/admin/hackathons/import (ADMIN)

`multipart/form-data` with one field `file` (a `.csv` file, UTF-8). First line must be:

```
title,organizer,description,startDate,endDate,registrationDeadline,minTeamSize,maxTeamSize
```

Values containing commas go in double quotes. Good lines are saved together in one manual transaction
(all or nothing). Bad lines are skipped and listed. Try it with `docs/sample-hackathons.csv`.

```json
{
  "success": true,
  "message": "Imported 5 hackathons, skipped 3 bad lines",
  "data": {
    "importedCount": 5,
    "errors": [
      "Line 7: startDate '2027-13-01' is not a date like 2026-12-31",
      "Line 8: Minimum team size cannot be bigger than the maximum",
      "Line 9: Registration deadline must be before the start date"
    ]
  }
}
```

A wrong first line stops the whole import: `400` "Import stopped. Line 1: the first line must be: ...".
A file not ending in `.csv` gives `400` "Please upload a .csv file".

## Colleges

### GET /api/admin/colleges (ADMIN)

```json
{
  "success": true, "message": "OK",
  "data": [ { "id": 1, "name": "Greenfield Institute of Technology", "reviewerName": "Greenfield Reviewer", "reviewerEmail": "review@greenfield.test" } ]
}
```

### POST /api/admin/colleges (ADMIN)

Creates the college **and** its COLLEGE reviewer login in one transaction. Answers `201 Created`.

```json
{ "name": "Hillside University", "reviewerName": "Hillside Reviewer", "email": "review@hillside.test", "password": "Demo@1234" }
```

`409 CONFLICT` if the college name or the email already exists.

## Teams

Team `status`: `OPEN` (taking members) → `FULL` (members = maxTeamSize, set automatically) → `SUBMITTED` → `REGISTERED`.
After submission the roster is locked. All rules are checked in the services; the user id always comes from the token.

### GET /api/hackathons/{id}/teams (logged in)

The teams part of a hackathon page for the logged-in student. `openTeams` are sorted by `matchPercent`
(share of the team's "looking for" skills the viewer has), then newest first. `joinBlockedReason` is null when the
student may create or join a team.

```json
{
  "success": true, "message": "OK",
  "data": {
    "myTeamId": null, "myTeamName": null, "joinBlockedReason": null,
    "openTeams": [
      { "id": 1, "name": "Alpha", "description": "AI tutor", "lookingFor": "react, figma, sql", "leaderName": "Asha Rao",
        "memberCount": 1, "maxTeamSize": 3, "matchPercent": 67, "pendingWithMe": false, "createdAt": "2026-11-17T06:15:02" }
    ]
  }
}
```

### POST /api/hackathons/{id}/teams (STUDENT)

```json
{ "name": "Alpha", "description": "AI tutor", "lookingFor": "React, Figma, SQL" }
```

Creates the team with the caller as leader (and first member). `201 Created` with the team (see GET /api/teams/{id}).
The caller's other pending requests in this hackathon are cancelled.
Errors: `409 ALREADY_IN_TEAM`, `409 CONFLICT` (name taken in this hackathon), `400 DEADLINE_PASSED`.

### GET /api/teams/mine (logged in)

```json
{ "success": true, "message": "OK", "data": [ { "id": 1, "name": "Alpha", "status": "FULL", "hackathonId": 4, "hackathonTitle": "AI for Good", "leader": true } ] }
```

### GET /api/teams/{id} (logged in)

Everything the team page needs. `viewerRole` is `LEADER`, `MEMBER` or `VISITOR`. Each `...Reason` is null when the
action is allowed, otherwise the sentence to show next to the disabled button. `pendingRequests` is filled only for the leader.

```json
{
  "success": true, "message": "OK",
  "data": {
    "id": 1, "name": "Alpha", "description": "AI tutor", "lookingFor": "react, figma, sql", "status": "OPEN",
    "hackathonId": 4, "hackathonTitle": "AI for Good", "minTeamSize": 2, "maxTeamSize": 3, "deadlineText": "Closes in 72 days",
    "members": [ { "userId": 4, "name": "Asha Rao", "userCode": "HTF-BJGVFS", "branch": "CSE", "year": 3, "skills": "java, react", "leader": true, "hasPicture": false } ],
    "viewerRole": "VISITOR", "matchPercent": 67,
    "lockedReason": null, "inviteBlockedReason": null, "applyBlockedReason": null,
    "pendingRequests": []
  }
}
```

### PUT /api/teams/{id} (leader)

Same body as create. Answers `"Team saved"` with the team. Not allowed once submitted.

### DELETE /api/teams/{id} (leader)

Deletes the team (its members, requests and registration go too). Members are notified. Not allowed once submitted.

### POST /api/teams/{id}/leave (member, not the leader)

A FULL team becomes OPEN again. Leader gets `400` "The leader cannot leave. Delete the team instead."

### DELETE /api/teams/{id}/members/{userId} (leader)

Removes a member (not the leader). A FULL team becomes OPEN again. Answers the updated team.

## Looking for a team (opt-in list)

A student without a team can put themselves on a hackathon's list. Team leaders of that hackathon can browse it
and invite with one click. Only students who opted in are listed. Joining or creating a team takes them off the list.

### POST /api/hackathons/{id}/looking-for-team (STUDENT)

Answers `"You are on the list. Team leaders in CodeSprint can now find and invite you."`
Errors: `409 ALREADY_IN_TEAM`, `409 CONFLICT` ("You are already on the list for this hackathon"),
`400 DEADLINE_PASSED`, `403 FORBIDDEN` (not a student).

### DELETE /api/hackathons/{id}/looking-for-team (logged in)

Takes me off the list. Answers `"You are no longer on the list"` (also when I was not on it).

The hackathon teams view (`GET /api/hackathons/{id}/teams`) also carries `"lookingForTeam": true|false` (am I on the list?)
and `"seekerCount": 3` (how many students are on it).

### GET /api/teams/{id}/seekers (leader)

The "Find teammates" list for my team: students on this hackathon's list who are not in a team, best match first
(match % against the team's "looking for" skills), then by name. No email or roll number.
`invitePending` = my team already has a pending invite or application with them. Invite them with
`POST /api/teams/{id}/invites` and their user code.

```json
{
  "success": true, "message": "OK",
  "data": [
    { "userId": 11, "name": "Dev Mehta", "userCode": "HTF-DEVM99", "collegeName": "Greenfield Institute of Technology",
      "branch": "ME", "year": 1, "skills": "python, figma, 3d printing", "matchPercent": 100, "invitePending": true }
  ]
}
```

Non-leaders get `403` "Only the team leader can see who is looking for a team".

## Requests (invites and applications)

One table, two types: `INVITE` (leader → student, by user code; the student answers) and `APPLICATION`
(student → OPEN team; the leader answers). Status: `PENDING` → `ACCEPTED` / `REJECTED` / `CANCELLED`.
Only one PENDING request per (team, student). Nothing new after the registration deadline.

### POST /api/teams/{id}/invites (leader)

```json
{ "userCodes": "HTF-3HPMVH, htf-fbwm99 HTF-ZZZZZZ" }
```

Codes can be separated by commas or spaces, any case. Codes that cannot be invited are skipped with a reason.

```json
{
  "success": true, "message": "Invites sent: 1",
  "data": {
    "sent": [ "Meena Iyer (HTF-3HPMVH)" ],
    "skipped": [ "HTF-FBWM99: Priya Sharma is already in a team for this hackathon", "HTF-ZZZZZZ: no student has this code" ]
  }
}
```

Errors: `409 TEAM_FULL`, `400 DEADLINE_PASSED`, `403 FORBIDDEN` (not the leader).

### POST /api/teams/{id}/apply (STUDENT)

Answers `"Application sent to Alpha"` (`data` is null).
Errors: `409 ALREADY_IN_TEAM`, `409 TEAM_FULL`, `409 CONFLICT` (already a pending request), `400 DEADLINE_PASSED`.

### GET /api/requests/mine (logged in)

Requests where I am the student plus requests for teams I lead. `incoming` = waiting for my answer.

```json
{
  "success": true, "message": "OK",
  "data": {
    "incoming": [
      { "id": 7, "type": "INVITE", "status": "PENDING", "teamId": 3, "teamName": "Gamma", "hackathonId": 4,
        "hackathonTitle": "AI for Good", "studentId": 8, "studentName": "Kiran Rao", "studentUserCode": "HTF-A37SXB",
        "leaderName": "Dev Patel", "createdAt": "2026-11-17T06:16:40", "incoming": true }
    ],
    "outgoing": []
  }
}
```

### POST /api/requests/{id}/accept (the receiver)

One transaction with the team row locked (`SELECT ... FOR UPDATE`): adds the member, marks the request ACCEPTED,
cancels the student's other pending requests in this hackathon, and if the team is now full sets it FULL and cancels
the team's other pending requests. Notifications go to the people affected. Answers e.g. `"You joined team Gamma"`.
Errors: `409 TEAM_FULL`, `409 ALREADY_IN_TEAM`, `400 DEADLINE_PASSED`, `400` "This request is already cancelled",
`403 FORBIDDEN` (not the receiver).

### POST /api/requests/{id}/reject (the receiver) · POST /api/requests/{id}/cancel (the sender)

Only PENDING requests. Answers `"Request rejected"` / `"Request cancelled"`.

## Registration through the college

### POST /api/teams/{id}/submit (leader)

Allowed when the team is OPEN or FULL, has between min and max members, and the deadline has not passed.
The team becomes `SUBMITTED` (roster locked), its pending requests are cancelled, and a `PENDING` registration
appears in the **leader's** college queue. Answers `"Team submitted to Greenfield Institute of Technology for approval"`.
Errors: `400` "Need 3 members — you have 2", "Already submitted. Waiting for your college to review it",
`400 DEADLINE_PASSED`, `403 FORBIDDEN` (not the leader).

The team page (`GET /api/teams/{id}`) carries `submitBlockedReason` (leader) and `registration` (leader and members):

```json
"registration": {
  "id": 3, "status": "APPROVED", "collegeName": "Greenfield Institute of Technology",
  "registrationCode": "REG-2026-000003", "rejectReason": null,
  "submittedAt": "2026-11-17T06:31:20", "decidedAt": "2026-11-17T06:31:58",
  "snapshot": {
    "fileName": "REG-2026-000003.ser", "registrationCode": "REG-2026-000003", "teamName": "Gamma",
    "hackathonTitle": "AI for Good", "collegeName": "Greenfield Institute of Technology",
    "memberNames": [ "Dev Patel", "Kiran Rao" ], "approvedAt": "2026-11-17T06:31:58", "leaderEmail": null
  }
}
```

`snapshot` is read back from `backend/data/snapshots/{code}.ser` on every request. `leaderEmail` is always `null`
because it is a `transient` field, so it was never written to the file.

### GET /api/college/registrations?status=PENDING (COLLEGE)

The college's own registrations only. `status` is `PENDING` (default, the review queue), `APPROVED` or `REJECTED`.

```json
{
  "success": true, "message": "OK",
  "data": [
    { "id": 2, "teamId": 1, "teamName": "Alpha Squad", "hackathonTitle": "AI for Good", "leaderName": "Test Student Two",
      "memberCount": 3, "status": "PENDING", "registrationCode": null, "rejectReason": null,
      "submittedAt": "2026-11-17T06:31:22", "decidedAt": null }
  ]
}
```

### GET /api/college/registrations/{id} (COLLEGE)

`summary` (same shape as above) plus `teamDescription`, `eventStart`, `eventEnd`, `minTeamSize`, `maxTeamSize` and
`members` (`name, email, userCode, rollNumber, branch, year, collegeName, leader`).
Another college's registration gives `403` "This team was submitted to another college".

### POST /api/college/registrations/{id}/approve (COLLEGE)

One transaction: registration `APPROVED` with code `REG-{year}-{id padded to 6}`, team `REGISTERED`, snapshot file
written, members notified. If the file cannot be written, everything rolls back.
Answers `"Team Alpha Squad approved. Registration code REG-2026-000002"`. Not PENDING any more: `400` "This team was already approved".

### POST /api/college/registrations/{id}/reject (COLLEGE)

```json
{ "reason": "Please add a third member, e.g. a designer" }
```

The reason is required (`400 VALIDATION_FAILED` "reason: please give a reason"). Team goes back to `OPEN`
(or `FULL` if it is at the maximum size) and may submit again. Members are told the reason.

### GET /api/college/registrations/export (COLLEGE)

**Not JSON:** a CSV file (`Content-Disposition: attachment; filename="registered-teams.csv"`) of the college's approved teams.

```
registrationCode,team,hackathon,leader,leaderEmail,members,approvedAt
REG-2026-000002,Alpha Squad,AI for Good,Test Student Two,test.student@example.test,Test Student Two; Meena Iyer; Priya Sharma,2026-11-17T06:31:35
```

## Notifications

Rows written by the services (invites, accepts, cancellations, submissions, decisions). No WebSocket, no polling.

### GET /api/notifications (logged in)

Newest first.

```json
{ "success": true, "message": "OK", "data": [ { "id": 31, "message": "Team Gamma is registered for AI for Good! Registration code: REG-2026-000003", "read": false, "createdAt": "2026-11-17T06:31:58" } ] }
```

### GET /api/notifications/unread-count (logged in)

```json
{ "success": true, "message": "OK", "data": 5 }
```

### POST /api/notifications/read-all (logged in)

Marks all as read. `data` is how many rows changed.

## Admin: teams

### GET /api/admin/teams (ADMIN)

```json
{
  "success": true, "message": "OK",
  "data": {
    "teams": [ { "id": 1, "name": "Alpha", "hackathonTitle": "AI for Good", "leaderName": "Asha Rao", "memberCount": 3, "maxTeamSize": 3, "status": "FULL", "createdAt": "2026-11-17T06:15:02" } ],
    "teamsPerHackathon": [ { "hackathonTitle": "AI for Good", "teamCount": 2 } ],
    "recentActions": [ "17 Nov 06:16 - Kiran Rao joined Gamma" ]
  }
}
```

`recentActions` is the in-memory "last 20 actions" list, newest first (empty after a restart).

### DELETE /api/admin/teams/{id} (ADMIN)

Deletes any team, whatever its status. Members are notified.

## Health

### GET /api/health (public)

Checks that the backend is up.

```json
{
  "success": true,
  "message": "Backend is running",
  "data": { "status": "UP", "userCodePrefix": "HTF-", "serverTime": "2026-11-16T23:45:10.123" }
}
```
