-- Creates every table the portal needs. Spring runs this file at startup (spring.sql.init.mode=always).
-- "IF NOT EXISTS" makes it safe to run again: existing tables and their rows are left alone.
-- To start from scratch: DROP DATABASE htf_portal, then restart the backend.
-- The service layer checks every rule first. The constraints below are a safety net behind it.

CREATE TABLE IF NOT EXISTS colleges (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_colleges_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS users (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,                   -- BCrypt hash, never the plain password
    role           ENUM('STUDENT', 'COLLEGE', 'ADMIN') NOT NULL,
    college_id     BIGINT       NULL,                       -- students and college accounts, NULL for admin
    user_code      VARCHAR(20)  NULL,                       -- students only, e.g. HTF-7K3Q9M
    roll_number    VARCHAR(30)  NULL,
    branch         VARCHAR(60)  NULL,
    study_year     TINYINT      NULL,
    skills         VARCHAR(500) NULL,                       -- comma-separated, e.g. java, react, sql
    picture_file   VARCHAR(100) NULL,                       -- random file name inside the upload folder
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email     UNIQUE (email),
    CONSTRAINT uq_users_user_code UNIQUE (user_code),       -- MySQL allows many NULLs in a UNIQUE column
    CONSTRAINT fk_users_college   FOREIGN KEY (college_id) REFERENCES colleges (id),
    CONSTRAINT chk_users_year     CHECK (study_year IS NULL OR study_year BETWEEN 1 AND 5)
);

CREATE TABLE IF NOT EXISTS hackathons (
    id                     BIGINT       AUTO_INCREMENT PRIMARY KEY,
    title                  VARCHAR(150) NOT NULL,
    organizer              VARCHAR(150) NOT NULL,
    description            TEXT         NULL,
    start_date             DATE         NOT NULL,
    end_date               DATE         NOT NULL,
    registration_deadline  DATE         NOT NULL,
    min_team_size          TINYINT      NOT NULL,
    max_team_size          TINYINT      NOT NULL,
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Status (OPEN / CLOSED / ONGOING / COMPLETED) is not stored: Java works it out from the dates.
    CONSTRAINT chk_hack_team_size CHECK (min_team_size >= 1 AND min_team_size <= max_team_size AND max_team_size <= 6),
    CONSTRAINT chk_hack_dates     CHECK (registration_deadline < start_date AND start_date <= end_date)
);

CREATE TABLE IF NOT EXISTS teams (
    id             BIGINT        AUTO_INCREMENT PRIMARY KEY,
    hackathon_id   BIGINT        NOT NULL,
    leader_id      BIGINT        NOT NULL,
    name           VARCHAR(100)  NOT NULL,
    description    VARCHAR(1000) NULL,
    looking_for    VARCHAR(500)  NULL,                      -- comma-separated skills the team wants
    status         ENUM('OPEN', 'FULL', 'SUBMITTED', 'REGISTERED') NOT NULL DEFAULT 'OPEN',
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_teams_name_per_hackathon UNIQUE (hackathon_id, name),
    -- No ON DELETE CASCADE here: a hackathon cannot be deleted while it still has teams.
    CONSTRAINT fk_teams_hackathon FOREIGN KEY (hackathon_id) REFERENCES hackathons (id),
    CONSTRAINT fk_teams_leader    FOREIGN KEY (leader_id)    REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS team_members (
    id             BIGINT   AUTO_INCREMENT PRIMARY KEY,
    team_id        BIGINT   NOT NULL,
    hackathon_id   BIGINT   NOT NULL,                       -- copied from the team on purpose, see the UNIQUE below
    user_id        BIGINT   NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- The database itself guarantees: one student, at most one team per hackathon.
    CONSTRAINT uq_member_one_team_per_hackathon UNIQUE (hackathon_id, user_id),
    CONSTRAINT fk_members_team      FOREIGN KEY (team_id)      REFERENCES teams (id) ON DELETE CASCADE,
    CONSTRAINT fk_members_hackathon FOREIGN KEY (hackathon_id) REFERENCES hackathons (id),
    CONSTRAINT fk_members_user      FOREIGN KEY (user_id)      REFERENCES users (id)
);

-- Students who clicked "I'm looking for a team" for one hackathon. Opt-in on purpose: leaders only ever
-- see students who asked to be found. A student's row is removed when they join or create a team.
CREATE TABLE IF NOT EXISTS team_seekers (
    id             BIGINT   AUTO_INCREMENT PRIMARY KEY,
    hackathon_id   BIGINT   NOT NULL,
    user_id        BIGINT   NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_seeker_once_per_hackathon UNIQUE (hackathon_id, user_id),
    CONSTRAINT fk_seekers_hackathon FOREIGN KEY (hackathon_id) REFERENCES hackathons (id) ON DELETE CASCADE,
    CONSTRAINT fk_seekers_user      FOREIGN KEY (user_id)      REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS join_requests (
    id             BIGINT   AUTO_INCREMENT PRIMARY KEY,
    team_id        BIGINT   NOT NULL,
    hackathon_id   BIGINT   NOT NULL,
    student_id     BIGINT   NOT NULL,                       -- the student who would join the team
    type           ENUM('INVITE', 'APPLICATION') NOT NULL,  -- INVITE: leader sent it. APPLICATION: student sent it.
    status         ENUM('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_requests_team      FOREIGN KEY (team_id)      REFERENCES teams (id) ON DELETE CASCADE,
    CONSTRAINT fk_requests_hackathon FOREIGN KEY (hackathon_id) REFERENCES hackathons (id),
    CONSTRAINT fk_requests_student   FOREIGN KEY (student_id)   REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS registrations (
    id                 BIGINT       AUTO_INCREMENT PRIMARY KEY,
    team_id            BIGINT       NOT NULL,
    college_id         BIGINT       NOT NULL,               -- the leader's college reviews it
    status             ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    registration_code  VARCHAR(30)  NULL,                   -- set on approval, e.g. REG-2026-000123
    reject_reason      VARCHAR(500) NULL,
    decided_at         DATETIME     NULL,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,   -- = submitted at
    CONSTRAINT uq_registrations_code UNIQUE (registration_code),
    CONSTRAINT fk_registrations_team    FOREIGN KEY (team_id)    REFERENCES teams (id) ON DELETE CASCADE,
    CONSTRAINT fk_registrations_college FOREIGN KEY (college_id) REFERENCES colleges (id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    message     VARCHAR(500) NOT NULL,
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
