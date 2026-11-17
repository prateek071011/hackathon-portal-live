# 5-minute demo script

**Before the demo**
1. Reset to the demo data: `cd backend` then `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=reset"`, wait for `Backend ready`, Ctrl+C.
2. Start normally: `.\mvnw.cmd spring-boot:run`, and in another window `cd frontend` then `npm run dev`.
3. Open two browser windows on http://localhost:5173: a normal one and a private (incognito) one.
   Each window remembers its own login, so you can switch people without logging out all the time.

Every password is `Demo@1234`.

---

## 0:00 – 0:45 · A new student signs up

*Normal window.*
1. **Sign up**: name `Nisha Verma`, email `nisha@greenfield.test`, password `Demo@1234`, college **Greenfield**,
   roll `23CS077`, branch `CSE`, year 2, skills `React, Figma`.
2. You land on **My Profile**: "Welcome! Your user code is HTF-XXXXXX". Point at the code: *"leaders type this to invite you"*.

> Talking point: password stored as a BCrypt hash; login returns a JWT that the React app sends on every request
> (`SecurityConfig`, `JwtAuthFilter`). The user code comes from `UserCodeGenerator` (static `SecureRandom`, no 0/O/1/I).

## 0:45 – 1:30 · Nisha creates a team

1. **Hackathons** → **CodeSprint** (status OPEN, "Closes in 20 days", worked out from today's date).
2. Open teams show **Byte Me** with Nisha's match %.
3. **Create team**: name `Quick Build`, idea `A canteen pre-order app`, looking for `python, figma`. You land on the team page as LEADER.

> Talking point: one team per student per hackathon is checked in `TeamService` **and** guaranteed by
> `UNIQUE(hackathon_id, user_id)` in `schema.sql`.

## 1:30 – 2:15 · Find teammates and invite by user code

1. Scroll to **Find teammates**: the students who clicked "I'm looking for a team" (Dev, Meena, Kiran), best match first.
   Point out: only people who opted in are listed, and no email or roll number is shown.
2. **Invite by user code**: type `HTF-MEEN67, HTF-ZZZZZZ` → *Invited: Meena Iyer*, and *Not invited: no student has this code*.
   (The **Invite** button in the Find teammates list does the same with one click.)
3. *Private window*: log in as `meena@greenfield.test` → **My Requests** → **Accept**. "You joined team Quick Build".
4. *Normal window*: reload Nisha's team page. Meena is a member and has **left the Find teammates list** automatically.

> Talking points: bad codes are dropped with `Iterator.remove()` and reported (`RequestService.invite`).
> The list (`SeekerService`) is a `team_seekers` table; joining a team deletes the row inside the accept transaction.

## 2:15 – 3:00 · Another student applies

1. *Private window*: log out, log in as `dev@greenfield.test` → **Hackathons** → **CodeSprint**.
   Open teams are sorted by **match %** (Dev has python + figma → 100%). **Apply to join** Quick Build.
2. *Normal window* (Nisha): reload the team page → **Pending requests** → **Accept**.
3. *Private window* (Dev): **My Requests** → his old application to **Byte Me** now says **CANCELLED**.

> Talking point (the core of the project): `RequestService.accept` is one `@Transactional` method that locks the team
> row with `SELECT ... FOR UPDATE`, re-checks everything, inserts the member, accepts the request, cancels Dev's other
> pending requests, and would set the team FULL. Any failure rolls everything back. Read the comment block above it.

## 3:00 – 3:45 · Submit to the college, college approves

1. *Normal window* (Nisha): **Submit team** (enabled: 3 members, minimum 2). The team shows SUBMITTED and the roster is locked.
2. *Private window*: log in as `review@greenfield.test` → **Review queue** → **Review** Quick Build.
   You see members, roll numbers and colleges. **Reject** stays disabled until a reason is typed. Click **Approve**.
   "Registration code REG-2026-00000X".
3. *Normal window* (Nisha): reload the team page → **REGISTERED**, the code, and the **Saved snapshot** read back from
   the `.ser` file. Leader email: *not stored (transient field)*. Notifications show a red badge.

> Talking points: `RegistrationSnapshot implements Serializable` with `serialVersionUID` and a `transient` field;
> `SnapshotStorage` uses `ObjectOutputStream` / `ObjectInputStream` with an `ObjectInputFilter`.
> The reviewer clicks **Registered teams → Download CSV** (`CsvWriter` with a `BufferedWriter`).

## 3:45 – 4:30 · Admin

1. *Private window*: log in as `admin@portal.test` → **Teams**: teams per hackathon (HashMap count),
   the **last 20 actions** (in-memory `LinkedList`), and every team.
2. **Delete** the team **Green Coders** → confirm. Its members get a notification.
3. Optional: **Hackathons** → **Import from CSV** → pick `docs/sample-hackathons.csv`:
   "Imported 5 hackathons, skipped 3 bad lines", with each bad line listed.

> Talking points: the import reads with `BufferedReader` and saves all good lines in **one manual transaction**
> (`JdbcHackathonDao.insertAll`: `setAutoCommit(false)`, `addBatch`, `executeBatch`, `commit` / `rollback`).
> A bad line throws the **checked** `CsvImportException`, which the reader must catch.

## 4:30 – 5:00 · Wrap-up

- Show `docs/SYLLABUS_MAP.md`: every syllabus topic → the file that uses it.
- Run `.\mvnw.cmd verify` if asked: 32 JUnit/Mockito tests, including "cannot join two teams in one hackathon",
  "accepting into a full team fails", "a full team cancels its pending requests" and "the deadline blocks actions".

---

## If something goes wrong

| Problem | Fix |
|---|---|
| "Cannot reach the server" | The backend is not running: `cd backend` then `.\mvnw.cmd spring-boot:run`. |
| Login says "Invalid email or password" for demo users | The demo data is not loaded: run the reset command above. |
| A demo step was already done (e.g. Nisha exists) | Reset again; it takes about 10 seconds. |
| Backend says `Access denied for user` | The MySQL password in `backend/local.properties` is wrong. |
