# CLAUDE.md: working rules and progress

## Working rules (from the build spec)

- Student course project, explained file by file in a viva: **simple and readable beats clever or complete**.
- Build **one phase at a time**. End of each phase: `mvnw verify` green, `npm run build` green, app starts,
  short plain-language summary, then **stop** for the go-ahead.
- **No libraries beyond the spec's section 2.** Ask first.
  - Backend: Spring Boot 4 (webmvc, validation, jdbc, security), mysql-connector-j, jjwt, JUnit 5 + Mockito.
  - Frontend: react, react-dom, react-router-dom, vite, @vitejs/plugin-react. No Axios, Tailwind, UI kits, TypeScript.
- Only Controller → Service → DAO. Plain JDBC (`PreparedStatement`/`ResultSet`). **No JPA / Hibernate / Spring Data / JdbcTemplate.**
- Small classes. Plain `for` loops over streams. The one allowed lambda-style exception is `Comparator.comparing(...).thenComparing(...)` (syllabus item).
- Every class starts with a 2–3 line purpose comment. Other comments explain *why*.
- No secrets in code: DB password and JWT secret come from properties with dev defaults. The real local password goes in `backend/local.properties` (git-ignored).
- Keep `docs/SYLLABUS_MAP.md` and `docs/API.md` updated every phase.

## Decisions agreed in Phase 0 (defaults accepted)

1. Maven Wrapper (`mvnw.cmd`, Maven 3.9.16). Compile target Java 21, runs on the installed JDK 26.
2. DB user `root`. Password from `backend/local.properties` or `DB_PASSWORD`.
3. `schema.sql` uses `CREATE TABLE IF NOT EXISTS`. (Changed in Phase 6: `data.sql` runs only with the reset profile.)
4. No `@SpringBootTest`: `mvnw verify` must pass without MySQL. Tests are Mockito service tests.
5. The leader cannot leave a team, only delete it.
6. Teams may mix colleges. The registration goes to the **leader's** college.
7. College rejects → team returns to OPEN, or FULL if it is at max size.
8. Admin cannot change min/max team size once a hackathon has teams, and cannot delete a hackathon that has teams (also enforced by the FK).
9. Deadline is inclusive: actions are allowed up to and including the deadline date.
10. CSV import: bad lines are skipped and reported. Good lines go in one batch, committed together. A DB error rolls the whole batch back.
11. Pictures and CSV downloads are fetched with the JWT as a blob (`URL.createObjectURL`). Every endpoint except auth/health needs login.
12. "One PENDING request per (team, student)" is checked in the service only (no generated-column trick).
13. Registration code = `REG-{year}-{registration id padded to 6}`. The snapshot's `transient` field = leader email.
14. The "last 20 actions" list is in memory only (empty after restart).
15. Error JSON = `ApiResponse` with `success:false` and `data: {status, code, message}`.
16. `AbstractJdbcDao.mapRow` is `protected abstract` (the spec said private) because the base class calls it.

## Progress log

- **Phase 0 (Plan):** done.
- **Phase 1 (Skeleton):** done. Maven + Vite projects, `schema.sql` (8 tables), `portal.properties` + `PortalConfig`,
  exception hierarchy + `GlobalExceptionHandler`, `ApiResponse<T>`, `GenericDao` / `AbstractJdbcDao`, `BaseEntity` + enums,
  `GET /api/health`, CORS (`WebConfig`), React status page + `api.js` + `MessageBar` + `styles.css`, README, docs.
  Spring Security + jjwt are added in Phase 2 (adding them now would lock every endpoint).
  Verified on 2026-11-17: `mvnw verify` + `npm run build` green. Backend starts against MySQL (schema.sql ran),
  `/api/health` OK, 404/405 error JSON OK, CORS allows only :5173. React status page shows live backend data.
  `.claude/launch.json` starts the Vite dev server for the in-app browser preview.
- **Phase 2 (Login and profiles):** done 2026-11-17. Spring Security + jjwt 0.13.0. `SecurityConfig` (rules, BCrypt, CORS;
  `WebConfig` removed), `JwtService`, `JwtAuthFilter` (created with `new`, not a bean), `SecurityErrorHandler` (401/403 JSON),
  `AuthUser` principal. `User`/`College` models, `UserDao`/`CollegeDao` + JDBC impls. `AuthService` (signup → auto-login,
  generic login error), `ProfileService`, `CollegeService`, `PictureStorage` (byte streams + magic-byte check),
  `UserCodeGenerator`, `SkillUtils`, `LoginFailedException`. `data.sql`: admin + 2 colleges + 2 reviewers (ids 1–3, password Demo@1234).
  Frontend: `AuthContext` (+ `homePathFor`), `NavBar`, `RequireRole`, Login/Signup/Profile pages, placeholders for later pages.
  Tests: `AuthServiceTest` (3), `UserCodeGeneratorTest` (1).
  Default Boot "user" account switched off via `spring.autoconfigure.exclude` (it printed a random password).
  Mockito prints a "self-attaching" warning on JDK 26. It is harmless, so it is left as is.
- **Test accounts created while testing Phase 2** (in the local DB, ids 4 and 5): `test.student@example.test` (password `secret123`)
  and `priya.ui@example.test` (password `uitest-pass-1`). **Phase 6 seed note:** demo students must not use fixed ids 4+
  unless the DB is reset first (`DROP DATABASE htf_portal`), or `INSERT IGNORE` would silently skip them.
- **Phase 3 (Hackathons):** done 2026-11-17. `Hackathon` model (`checkRules()`, `statusOn(today)`), `HackathonDao`/`JdbcHackathonDao`
  (`countTeams`, `countTeamsPerHackathon` via new `AbstractJdbcDao.queryForCounts` HashMap helper, manual-transaction `insertAll`),
  `HackathonService`, `HackathonImportService`, `CsvReader<T>` + `CsvLineParser<T>` + `HackathonCsvParser` (quoted values, BOM, header check),
  `CollegeService.create` (@Transactional college + reviewer), `DateUtils`, `TextUtils` (normalizeEmail moved here).
  Controllers: `HackathonController` (any logged-in), `AdminHackathonController`, `AdminCollegeController`.
  Frontend: student `HackathonListPage` + `HackathonDetailPage` (teams placeholder), `AdminHackathonsPage` (form, CSV import with
  template download, table), `AdminCollegesPage`, `HackathonForm`, `StatusBadge`, `format.js`. `docs/sample-hackathons.csv` (5 good + 3 bad lines).
  Tests: `HackathonServiceTest` (5), `CsvReaderTest` (2). Total 11.
  Local DB now has the 5 sample hackathons (imported while testing) and a test college "Hillside University" (reviewer review@hillside.test / Demo@1234).
  Not tested live: the `CollegeService.create` rollback path (the pre-checks catch duplicates first), and locked team sizes (no teams exist until Phase 4).
- **Phase 4 (Teams and requests):** done 2026-11-17. Models `Team`, `TeamMember`, `JoinRequest`, `Notification` + DAOs.
  `TeamService` (create/view/edit/delete/leave/remove, open teams by match %, my teams, admin overview + delete),
  `RequestService` (invite by codes with Iterator.remove, apply, **accept transaction** with team row `FOR UPDATE` +
  READ_COMMITTED, reject, cancel, my requests), `TeamRules` (package-private checks), `ActivityLog` (LinkedList, last 20),
  `NotificationService.notify` (rows only; page in Phase 5), `SkillUtils.toSet/matchPercent`.
  Controllers: `TeamController`, `RequestController`, `AdminTeamController`.
  Frontend: `TeamPage`, `MyRequestsPage`, `AdminTeamsPage`, teams section on `HackathonDetailPage`, "Your teams" on the list page,
  `TeamForm`, `SkillTags`. Tests: `RequestServiceTest` (7), `TeamServiceTest` (4). Total 22.
  More test accounts (password `testpass123`): meena.test@, kiran.test@, dev.test@example.test. Local DB now has
  teams "Alpha Squad" (FULL, leader test.student) and "Gamma" (OPEN, leader dev.test) in AI for Good.
  Accept race tested live: 2 parallel accepts for the last seat → exactly one joined.
- **Phase 5 (Registration):** done 2026-11-17. `Registration` model + DAO (`findByIdForUpdate`, `findLatestByTeam`),
  `RegistrationSnapshot` (Serializable, serialVersionUID, transient leaderEmail), `SnapshotStorage` (Object streams +
  ObjectInputFilter), `CsvWriter` (BufferedWriter, CSV-injection guard), `RegistrationService` (submitBlockedReason, submit,
  registrationForTeam, college list/detail/approve/reject, CSV export), `NotificationService` list/unread/read-all.
  Controllers: `CollegeController` (/api/college/registrations), `NotificationController`, `TeamController` POST /teams/{id}/submit.
  `TeamView` gained `submitBlockedReason` + `registration`. Frontend: `NotificationsPage` (StrictMode-safe read-all),
  unread badge in `NavBar` (fetched per page change, hidden on /notifications), college `CollegeQueuePage`,
  `CollegeReviewPage`, `CollegeRegisteredPage` (CSV via `downloadFile`), team page Submit + registration/snapshot card.
  `ComingSoonPage` deleted (no placeholders left). Tests: `RegistrationServiceTest` (5), `SnapshotStorageTest` (2). Total 29.
  Local DB: Alpha Squad REGISTERED (REG-2026-000002), Gamma REGISTERED (REG-2026-000003), snapshot files in backend/data/snapshots.
- **Phase 6 (Polish):** done 2026-11-17. Demo data now loads ONLY via the "reset" profile
  (`application-reset.properties`: `reset.sql` drops tables → `schema.sql` → `data.sql`); `application.properties` has
  `spring.sql.init.data-locations=` (empty) so normal restarts never resurrect deleted rows. `data.sql`: plain INSERTs,
  1 admin, 2 colleges/reviewers, 8 students (codes HTF-ASHA23 ...), CodeSprint + GreenTech Challenge (dates from CURDATE()),
  Byte Me OPEN / Data Wizards SUBMITTED / Green Coders FULL / Solar Squad REGISTERED, 3 pending requests, notifications.
  The seeded Solar Squad has no .ser snapshot (SQL cannot write files); teams approved live do.
  `config/StartupCheck` (ApplicationRunner) warns if no admin exists. README rewritten (reset step, demo logins table),
  `docs/DEMO_SCRIPT.md` added, API.md endpoint index, SYLLABUS_MAP final.
  The user approved a DB reset on 2026-11-17: all earlier test accounts/teams and old test files in backend/data were deleted.
  Demo flow verified end to end over the API, then the DB was reset again to the clean demo state.

- **Extra feature: "Looking for a team" (2026-11-17).** The user asked for it. It was on the spec's do-not-build list
  ("solo-participant posts"), and the user chose the opt-in version on purpose (privacy). New table `team_seekers`
  (UNIQUE(hackathon_id, user_id), FKs cascade), `TeamSeeker` + `TeamSeekerDao` (list uses NOT EXISTS to skip team members),
  `SeekerService` (start/stop looking, leader-only "Find teammates" list sorted by match %, no email/roll number),
  rows removed on join in `RequestService.accept` (step 5) and `TeamService.createTeam`. Endpoints
  `POST/DELETE /api/hackathons/{id}/looking-for-team`, `GET /api/teams/{id}/seekers`. `HackathonTeamsView` gained
  `lookingForTeam` + `seekerCount`. UI: card on the hackathon page, "Find teammates" card on the leader's team page
  (one-click Invite reuses the invite endpoint). Seed: Dev, Meena, Kiran on CodeSprint's list.
  Tests: `SeekerServiceTest` (3) + accept test checks the row is removed. Total 32.
  Note: the project is in OneDrive and Vite's file watcher once missed an edit; restart `npm run dev` if the page looks stale.

- **Project report (2026-11-17):** `docs/Project_Report.docx` + `.pdf`, built on the college's "Java Project Report Format"
  template (cover page kept; guide Ms. Shivani Mahendru; Krishna Gautam 1024030733, Prateek Yadav 1024031149).
  Diagrams and screenshots were generated from the running app; the builder scripts lived in a temp scratch folder,
  so later changes to the report should be made in Word.

## Project status

All six phases are done. Changes from here on: keep the rules above, update docs/ and this log, and run
`mvnw verify` + `npm run build` before calling anything finished.
