# Hackathon Team-Finder Portal

A website where students form teams for hackathons and submit them to their college for approval.
Course project for **Enterprise Web Application in Java**.

- **Student**: browse hackathons, create a team or apply to one, invite people by user code, submit the team to the college.
  A student without a team can click **"I'm looking for a team"**; leaders then see them in a **Find teammates** list
  (best skill match first) and can invite with one click.
- **College**: approve or reject the teams its students submit, download the registered teams as CSV.
- **Admin**: create hackathons (by form or CSV import) and colleges, delete teams, see the last 20 actions.

| Part | Tech |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Web MVC, Bean Validation, Spring Security + JWT (jjwt), plain JDBC (spring-jdbc), MySQL 8 |
| Frontend | React + Vite, plain JavaScript, plain CSS, react-router-dom |
| Tests | JUnit 5 + Mockito (32 tests, no database needed) |

```
/backend     Spring Boot (Maven)
/frontend    React + Vite
/docs        SYLLABUS_MAP.md (syllabus topic -> file), API.md (every endpoint),
             DEMO_SCRIPT.md (5-minute demo), sample-hackathons.csv (for the CSV import)
```

## How to run

### 1. What you need installed

- JDK 21 or newer
- Node.js 20 or newer
- MySQL 8, running on `localhost:3306`

Maven does **not** need to be installed: the project includes the Maven Wrapper (`mvnw.cmd`), which downloads Maven by itself on first use.

### 2. Tell the backend your MySQL password (one time)

```
cd backend
copy local.properties.example local.properties
```

Open `backend/local.properties` and put your MySQL username and password in it. This file is ignored by git.
The database `htf_portal` and all its tables are created automatically.

### 3. First start: load the demo data (also before every demo)

```
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=reset"
```

The **reset** profile deletes every table, creates them again (`schema.sql`) and loads the demo data (`data.sql`).
Stop it with Ctrl+C when you see `Backend ready`.

### 4. Normal start (port 8080)

```
cd backend
.\mvnw.cmd spring-boot:run
```

A normal start never changes your data. Check it: open http://localhost:8080/api/health

### 5. Start the frontend (port 5173)

```
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 and log in with one of the demo accounts below.

### Build and test

```
cd backend
.\mvnw.cmd verify
```

```
cd frontend
npm run build
```

### Files the backend writes

- `backend/logs/app.log`: the log (also printed on the console).
- `backend/data/uploads/`: profile pictures, saved under random names.
- `backend/data/snapshots/`: one `REG-....ser` file per approved team (Java serialization).

The reset profile only resets the database. Old files in `backend/data` stay; a snapshot is overwritten when the same
registration code is approved again.

## Demo logins

Loaded by `data.sql` (reset profile). **Every password is `Demo@1234`.**

| Who | Email | User code | Notes |
|---|---|---|---|
| Admin | `admin@portal.test` | | |
| College reviewer | `review@greenfield.test` | | Greenfield Institute of Technology |
| College reviewer | `review@riverside.test` | | Riverside College of Engineering: **Data Wizards** waits in its queue |
| Student | `asha@greenfield.test` | HTF-ASHA23 | Leads **Byte Me** (OPEN, 2/4) with 3 pending requests |
| Student | `ravi@greenfield.test` | HTF-RAVK45 | In Byte Me and Solar Squad |
| Student | `meena@greenfield.test` | HTF-MEEN67 | In Solar Squad; free in CodeSprint and on its "looking for a team" list (invite her!) |
| Student | `kiran@greenfield.test` | HTF-KRN234 | Leads **Solar Squad** (REGISTERED, REG-year-000001); on CodeSprint's list; has an invite from Byte Me |
| Student | `priya@riverside.test` | HTF-PRYA89 | Leads **Data Wizards** (SUBMITTED) |
| Student | `arjun@riverside.test` | HTF-ARJN56 | In Data Wizards and Green Coders |
| Student | `sneha@riverside.test` | HTF-SNHA78 | Leads **Green Coders** (FULL, 3/3); applied to Byte Me |
| Student | `dev@greenfield.test` | HTF-DEVM99 | In no team; on CodeSprint's list; applied to Byte Me (100% match) |

Hackathons (dates count from the day you reset, so both are always open):
**CodeSprint** (teams of 2–4, closes in 20 days) and **GreenTech Challenge** (teams of 2–3, closes in 10 days).
More hackathons can be imported from `docs/sample-hackathons.csv` on the admin page.

The 5-minute walkthrough is in [docs/DEMO_SCRIPT.md](docs/DEMO_SCRIPT.md).

## Settings you can override

| Setting | Where | Default |
|---|---|---|
| MySQL user / password | `backend/local.properties` or `DB_USERNAME` / `DB_PASSWORD` | `root` / `root` |
| JWT secret (32+ characters) | `JWT_SECRET` environment variable | a dev-only value in `application.properties` |
| User-code prefix, upload folder, max picture size, snapshot folder | `backend/src/main/resources/portal.properties` | `HTF-`, `data/uploads`, 2 MB, `data/snapshots` |

## Coding conventions

- **Layers:** Controller → Service → DAO. Controllers only read the request and call one service method.
  Services hold every business rule and ownership check. DAOs only run SQL.
- **Packages:** `config, security, controller, service, dao, model, dto, exception, io, util` under `com.htf.portal`.
- **Names:** classes `PascalCase`, methods and variables `camelCase`, constants `UPPER_SNAKE_CASE`,
  DB tables and columns `snake_case`. DAO interfaces are `XxxDao`, their JDBC versions `JdbcXxxDao`.
- **Every class** starts with a 2–3 line comment saying what it is for. Other comments explain *why*.
- **Fields** are `private`. Methods are `public` only if another class needs them, `protected` if only subclasses do.
- **Status and role values** are always enums, never loose strings.
- **SQL** always uses `PreparedStatement` with `?` placeholders. Values are never glued into SQL text.
- **Errors:** services throw our own exceptions (`NotFoundException`, `ConflictException`, ...).
  `GlobalExceptionHandler` turns them into JSON. Controllers never use try/catch.
- **No secrets in code:** passwords and the JWT secret come from properties files or environment variables.
- **Readable over clever:** a plain `for` loop instead of streams or lambdas where that is clearer.
- **Frontend:** function components with `useState` / `useEffect`. All backend calls go through `src/api.js`.
  Disabled buttons always show why (the reason comes from the backend).
