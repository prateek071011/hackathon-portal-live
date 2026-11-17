-- Demo data. Runs ONLY with the "reset" profile, right after reset.sql dropped every table and schema.sql
-- created them again, so plain INSERTs with fixed ids are safe.
-- Every account's password is Demo@1234. The long $2a$10$... value is its BCrypt hash, not the password.
-- Dates are relative to the day you reset (CURDATE()), so both hackathons are open whenever you run the demo.
--
-- What is in here (see README "Demo logins"):
--   2 colleges, 1 admin, 2 college reviewers, 8 students
--   2 hackathons: CodeSprint (teams of 2-4) and GreenTech Challenge (teams of 2-3)
--   4 teams, one in each status: Byte Me OPEN, Data Wizards SUBMITTED, Green Coders FULL, Solar Squad REGISTERED
--   3 pending requests for Byte Me, a little request history, and some notifications
--   Dev, Meena and Kiran are on CodeSprint's "looking for a team" list

INSERT INTO colleges (id, name) VALUES
    (1, 'Greenfield Institute of Technology'),
    (2, 'Riverside College of Engineering');

INSERT INTO users (id, name, email, password_hash, role, college_id, user_code, roll_number, branch, study_year, skills) VALUES
    (1,  'Portal Admin',        'admin@portal.test',      '$2a$10$h3RYWnb3VlCUvChLStXWmupA9QbgKt.xlQKv9YEFPAKVJptDeUf1u', 'ADMIN',   NULL, NULL, NULL, NULL, NULL, NULL),
    (2,  'Greenfield Reviewer', 'review@greenfield.test', '$2a$10$h3RYWnb3VlCUvChLStXWmupA9QbgKt.xlQKv9YEFPAKVJptDeUf1u', 'COLLEGE', 1,    NULL, NULL, NULL, NULL, NULL),
    (3,  'Riverside Reviewer',  'review@riverside.test',  '$2a$10$h3RYWnb3VlCUvChLStXWmupA9QbgKt.xlQKv9YEFPAKVJptDeUf1u', 'COLLEGE', 2,    NULL, NULL, NULL, NULL, NULL),
    (4,  'Asha Rao',            'asha@greenfield.test',   '$2a$10$h3RYWnb3VlCUvChLStXWmupA9QbgKt.xlQKv9YEFPAKVJptDeUf1u', 'STUDENT', 1, 'HTF-ASHA23', '22CS014', 'CSE', 3, 'java, spring, sql'),
    (5,  'Ravi Kumar',          'ravi@greenfield.test',   '$2a$10$h3RYWnb3VlCUvChLStXWmupA9QbgKt.xlQKv9YEFPAKVJptDeUf1u', 'STUDENT', 1, 'HTF-RAVK45', '22CS027', 'CSE', 3, 'react, javascript, css'),
    (6,  'Meena Iyer',          'meena@greenfield.test',  '$2a$10$h3RYWnb3VlCUvChLStXWmupA9QbgKt.xlQKv9YEFPAKVJptDeUf1u', 'STUDENT', 1, 'HTF-MEEN67', '23IT008', 'IT',  2, 'figma, ui design, react'),
    (7,  'Kiran Shah',          'kiran@greenfield.test',  '$2a$10$h3RYWnb3VlCUvChLStXWmupA9QbgKt.xlQKv9YEFPAKVJptDeUf1u', 'STUDENT', 1, 'HTF-KRN234', '21EC033', 'ECE', 4, 'python, machine learning'),
    (8,  'Priya Nair',          'priya@riverside.test',   '$2a$10$h3RYWnb3VlCUvChLStXWmupA9QbgKt.xlQKv9YEFPAKVJptDeUf1u', 'STUDENT', 2, 'HTF-PRYA89', '23CS041', 'CSE', 2, 'python, sql, data analysis'),
    (9,  'Arjun Das',           'arjun@riverside.test',   '$2a$10$h3RYWnb3VlCUvChLStXWmupA9QbgKt.xlQKv9YEFPAKVJptDeUf1u', 'STUDENT', 2, 'HTF-ARJN56', '22IT019', 'IT',  3, 'java, android'),
    (10, 'Sneha Patil',         'sneha@riverside.test',   '$2a$10$h3RYWnb3VlCUvChLStXWmupA9QbgKt.xlQKv9YEFPAKVJptDeUf1u', 'STUDENT', 2, 'HTF-SNHA78', '21CS052', 'CSE', 4, 'react, node, mongodb'),
    (11, 'Dev Mehta',           'dev@greenfield.test',    '$2a$10$h3RYWnb3VlCUvChLStXWmupA9QbgKt.xlQKv9YEFPAKVJptDeUf1u', 'STUDENT', 1, 'HTF-DEVM99', '24ME003', 'ME',  1, 'python, figma, 3d printing');

INSERT INTO hackathons (id, title, organizer, description, start_date, end_date, registration_deadline, min_team_size, max_team_size) VALUES
    (1, 'CodeSprint', 'Greenfield Tech Club',
        '24-hour build sprint. Any stack, any idea: ship something that works by the end.',
        CURDATE() + INTERVAL 27 DAY, CURDATE() + INTERVAL 28 DAY, CURDATE() + INTERVAL 20 DAY, 2, 4),
    (2, 'GreenTech Challenge', 'Riverside Eco Society',
        'Software that makes the campus greener: energy, waste or transport.',
        CURDATE() + INTERVAL 15 DAY, CURDATE() + INTERVAL 16 DAY, CURDATE() + INTERVAL 10 DAY, 2, 3);

-- One team in each status.
INSERT INTO teams (id, hackathon_id, leader_id, name, description, looking_for, status, created_at) VALUES
    (1, 1, 4,  'Byte Me',      'A study-group finder for our campus.',        'figma, python', 'OPEN',       NOW() - INTERVAL 3 DAY),
    (2, 1, 8,  'Data Wizards', 'Dashboards for the college library.',         'sql, react',    'SUBMITTED',  NOW() - INTERVAL 4 DAY),
    (3, 2, 10, 'Green Coders', 'An app to track hostel electricity use.',     'react',         'FULL',       NOW() - INTERVAL 2 DAY),
    (4, 2, 7,  'Solar Squad',  'Predicting solar panel output with ML.',      'python',        'REGISTERED', NOW() - INTERVAL 5 DAY);

-- hackathon_id is repeated from the team so UNIQUE(hackathon_id, user_id) can hold: one team per student per hackathon.
INSERT INTO team_members (id, team_id, hackathon_id, user_id, created_at) VALUES
    (1,  1, 1, 4,  NOW() - INTERVAL 3 DAY),   -- Byte Me: Asha (leader), Ravi
    (2,  1, 1, 5,  NOW() - INTERVAL 2 DAY),
    (3,  2, 1, 8,  NOW() - INTERVAL 4 DAY),   -- Data Wizards: Priya (leader), Arjun
    (4,  2, 1, 9,  NOW() - INTERVAL 4 DAY),
    (5,  3, 2, 10, NOW() - INTERVAL 2 DAY),   -- Green Coders: Sneha (leader), Arjun, Priya
    (6,  3, 2, 9,  NOW() - INTERVAL 2 DAY),
    (7,  3, 2, 8,  NOW() - INTERVAL 1 DAY),
    (8,  4, 2, 7,  NOW() - INTERVAL 5 DAY),   -- Solar Squad: Kiran (leader), Ravi, Meena
    (9,  4, 2, 5,  NOW() - INTERVAL 5 DAY),
    (10, 4, 2, 6,  NOW() - INTERVAL 4 DAY);

-- Pending: Dev and Sneha applied to Byte Me; Byte Me invited Kiran. The rest is history.
INSERT INTO join_requests (id, team_id, hackathon_id, student_id, type, status, created_at) VALUES
    (1, 1, 1, 11, 'APPLICATION', 'PENDING',   NOW() - INTERVAL 20 HOUR),
    (2, 1, 1, 7,  'INVITE',      'PENDING',   NOW() - INTERVAL 18 HOUR),
    (3, 1, 1, 10, 'APPLICATION', 'PENDING',   NOW() - INTERVAL 6 HOUR),
    (4, 4, 2, 6,  'INVITE',      'ACCEPTED',  NOW() - INTERVAL 4 DAY),
    (5, 3, 2, 6,  'APPLICATION', 'CANCELLED', NOW() - INTERVAL 4 DAY),
    (6, 1, 1, 5,  'APPLICATION', 'ACCEPTED',  NOW() - INTERVAL 2 DAY);

-- Students who clicked "I'm looking for a team" in CodeSprint (none of them is in a CodeSprint team).
INSERT INTO team_seekers (id, hackathon_id, user_id, created_at) VALUES
    (1, 1, 11, NOW() - INTERVAL 22 HOUR),   -- Dev
    (2, 1, 6,  NOW() - INTERVAL 12 HOUR),   -- Meena
    (3, 1, 7,  NOW() - INTERVAL 10 HOUR);   -- Kiran

-- Solar Squad was approved by Greenfield; Data Wizards waits in Riverside's queue.
INSERT INTO registrations (id, team_id, college_id, status, registration_code, reject_reason, decided_at, created_at) VALUES
    (1, 4, 1, 'APPROVED', CONCAT('REG-', YEAR(CURDATE()), '-000001'), NULL, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 2 DAY),
    (2, 2, 2, 'PENDING',  NULL, NULL, NULL, NOW() - INTERVAL 1 DAY);

INSERT INTO notifications (user_id, message, is_read, created_at) VALUES
    (4,  'Ravi Kumar joined your team Byte Me',                                      TRUE,  NOW() - INTERVAL 2 DAY),
    (4,  'Dev Mehta applied to join your team Byte Me',                              FALSE, NOW() - INTERVAL 20 HOUR),
    (4,  'Sneha Patil applied to join your team Byte Me',                            FALSE, NOW() - INTERVAL 6 HOUR),
    (7,  'Asha Rao invited you to join team Byte Me for CodeSprint',                 FALSE, NOW() - INTERVAL 18 HOUR),
    (7,  CONCAT('Team Solar Squad is registered for GreenTech Challenge! Registration code: REG-', YEAR(CURDATE()), '-000001'), TRUE, NOW() - INTERVAL 1 DAY),
    (5,  CONCAT('Team Solar Squad is registered for GreenTech Challenge! Registration code: REG-', YEAR(CURDATE()), '-000001'), FALSE, NOW() - INTERVAL 1 DAY),
    (6,  CONCAT('Team Solar Squad is registered for GreenTech Challenge! Registration code: REG-', YEAR(CURDATE()), '-000001'), FALSE, NOW() - INTERVAL 1 DAY),
    (9,  'Priya Nair submitted team Data Wizards to Riverside College of Engineering for approval', FALSE, NOW() - INTERVAL 1 DAY),
    (10, 'Your team Green Coders is now full',                                       TRUE,  NOW() - INTERVAL 1 DAY);
