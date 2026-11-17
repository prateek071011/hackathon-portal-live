# Syllabus map

Where each syllabus topic shows up in the Java code. Paths are under `backend/src/main/java/com/htf/portal/`.
Every item below is implemented (✅). To see one in action, the demo script in `DEMO_SCRIPT.md` names the page that uses it.

## Unit 1: Java foundations

| Topic | File | How it is used |
|---|---|---|
| Packages | `config, security, controller, service, dao, model, dto, exception, io, util` | ✅ Each layer has its own package. |
| Interfaces | `dao/GenericDao.java` | ✅ The basic DAO operations, declared once. |
| Interfaces (one impl each) | `dao/UserDao` → `JdbcUserDao`, `CollegeDao`, `HackathonDao`, `TeamDao`, `TeamMemberDao`, `TeamSeekerDao`, `JoinRequestDao`, `RegistrationDao`, `NotificationDao` (each → `Jdbc...`) | ✅ Services depend on the interface, Spring injects the JDBC class. `JdbcTeamMemberDao.update` and `JdbcTeamSeekerDao.update` throw `UnsupportedOperationException` on purpose (those rows are only inserted or deleted). |
| Interfaces (own, generic) | `io/CsvLineParser<T>` → `io/HackathonCsvParser` | ✅ The reader works with any parser; the hackathon parser knows the columns. |
| Interfaces (framework callback) | `config/StartupCheck.java` | ✅ Implements Spring's `ApplicationRunner`: `run()` is called once after start-up and warns if no admin exists (demo data not loaded). |
| Interfaces (implementing two) | `security/SecurityErrorHandler.java` | ✅ One class implements both `AuthenticationEntryPoint` (401) and `AccessDeniedHandler` (403). |
| Abstract class | `model/BaseEntity.java` | ✅ Holds `id` and `createdAt` for every model. Abstract because it is never used alone. |
| Abstract class | `dao/AbstractJdbcDao.java` | ✅ Shared `protected` JDBC helpers. Each DAO implements the abstract `mapRow`. |
| Abstract class (framework) | `security/JwtAuthFilter.java` | ✅ Extends Spring's abstract `OncePerRequestFilter` and fills in `doFilterInternal`. |
| Access modifiers | `exception/ConflictException.java` | ✅ `public` constructor for normal use, `protected` constructor only for subclasses. |
| Access modifiers | `dao/AbstractJdbcDao.java` | ✅ `private` dataSource, `protected` helpers, `private` `translate()`. |
| Access modifiers | `config/PortalConfig.java` | ✅ `private final` fields, `public` getters, `getAllowedPictureTypes()` returns a copy. |
| Access modifiers | `util/UserCodeGenerator.java`, `util/DateUtils.java`, `util/TextUtils.java` | ✅ `private` constructor, so nobody can create an object of a static-only class. |
| Access modifiers (package-private) | `service/TeamRules.java` | ✅ No `public` on the class or its methods: only `TeamService` and `RequestService` (same package) can use these checks. |
| Inner and nested classes | `service/RequestService.Lookup`, `dto/AdminTeamsView.Row` / `HackathonCount` | ✅ `Lookup` is an inner class that calls its outer service's methods. The two records are nested because only `AdminTeamsView` uses them. |
| Constants | `model/Hackathon.MAX_ALLOWED_TEAM_SIZE`, `io/HackathonCsvParser.HEADER` | ✅ `public static final` values shared by code and tests. |
| Enums | `model/Role, HackathonStatus, TeamStatus, RequestType, RequestStatus, RegistrationStatus`, `dto/TeamViewerRole` | ✅ Every status and role is an enum. `Hackathon.statusOn()` returns a `HackathonStatus`; the team page gets `LEADER` / `MEMBER` / `VISITOR`. |
| Static vs instance | `util/UserCodeGenerator.java` | ✅ `static final SecureRandom` shared by everyone, `static generate()`. Services (e.g. `AuthService`) are instance beans with injected fields. |
| Static vs instance | `util/DateUtils`, `util/TextUtils`, `dto/UserSummary.from`, `io/CsvReader.splitLine` | ✅ Static helpers that need no object state. `AuthService` uses a **static import** of `TextUtils.normalizeEmail`. |
| Constructor overloading | `exception/NotFoundException.java` | ✅ `(message)` and `(what, id)`. |
| Constructor overloading | `model/User`, `College`, `Hackathon`, `Team`, `JoinRequest`, `Notification` | ✅ An empty constructor for the DAO plus a "new object" constructor, e.g. `Team(hackathonId, leaderId, name, ...)` which always starts `OPEN`, `JoinRequest(...)` which always starts `PENDING`. |
| Coding conventions | `README.md` | ✅ "Coding conventions" section. |

## Unit 2: Exceptions, collections, generics

| Topic | File | How it is used |
|---|---|---|
| Custom exception hierarchy | `exception/PortalException.java` and children | ✅ Abstract unchecked parent that holds the HTTP status → NotFound, ForbiddenAction, Conflict (→ TeamFull, AlreadyInTeam), BusinessRule (→ DeadlinePassed), LoginFailed. |
| Throwing them | `service/AuthService`, `ProfileService`, `HackathonService`, `CollegeService`, `io/PictureStorage` | ✅ Duplicate email → `ConflictException`, bad login → `LoginFailedException`, min > max → `BusinessRuleException`, unknown id → `NotFoundException`. |
| Checked vs unchecked | `exception/CsvImportException.java` used by `io/HackathonCsvParser`, `io/CsvReader`, `service/HackathonImportService` | ✅ Checked: `parse()` declares `throws CsvImportException`, so `CsvReader` **must** catch it. It does, records "Line 7: ...", and moves on. A bad header is re-thrown and the service turns it into an unchecked `BusinessRuleException`. |
| Checked → unchecked | `dao/AbstractJdbcDao.java`, `service/ProfileService.java`, `dao/JdbcHackathonDao.insertAll` | ✅ `SQLException` → `DatabaseException` / `DuplicateKeyException`. `IOException` → `UncheckedIOException`. |
| Catching a specific exception | `service/AuthService.signup`, `service/CollegeService.create` | ✅ Catches `DuplicateKeyException` and turns it into a clear `ConflictException`. |
| Multi-catch | `security/JwtService.readToken` | ✅ `catch (JwtException \| IllegalArgumentException e)`. |
| Global handler | `exception/GlobalExceptionHandler.java` | ✅ `@RestControllerAdvice` turns every exception into `{status, code, message}`. |
| ArrayList | `dao/AbstractJdbcDao`, `util/SkillUtils`, `io/CsvReader`, `service/HackathonService.listAll` | ✅ Query results, the CSV rows and error list. `listAll` builds two lists (open / others) and joins them. |
| HashSet | `util/SkillUtils.toSet / matchPercent`, `service/TeamService.teamsForHackathon`, `service/SeekerService.seekersForTeam`, `config/PortalConfig` | ✅ Skills become a `HashSet`; `retainAll` keeps the intersection of "team wants" and "student has". Team ids / student ids with a pending request go in a set for quick `contains` checks. Allowed picture types. |
| HashMap | `service/TeamService.adminOverview`, `dao/AbstractJdbcDao.queryForCounts`, `service/CollegeService.listWithReviewers`, `service/RequestService.Lookup` | ✅ Admin page counts teams per hackathon in Java: `put(id, getOrDefault(id, 0) + 1)`. Member counts per team come back as `teamId → count`. `Lookup` caches teams/users/hackathons by id. |
| LinkedList | `service/ActivityLog.java` | ✅ Last 20 actions: `addLast` the newest, `removeFirst` the oldest when there are more than 20. Methods are `synchronized` because requests run on several threads. |
| Iterator.remove() | `service/RequestService.invite` | ✅ Loops over the typed user codes and removes each one that cannot be invited (unknown, own code, already in a team, already pending), recording why. |
| Comparator.comparing().thenComparing() | `service/TeamService.OPEN_TEAM_ORDER`, `service/SeekerService.BEST_MATCH_FIRST` | ✅ `comparing(matchPercent, reverseOrder()).thenComparing(createdAt, reverseOrder())`: best match first, then newest. The "Find teammates" list: best match first, then by name. `RequestService.myRequests` sorts by `createdAt` reversed. |
| Generics | `dao/GenericDao<T, ID>`, `dao/AbstractJdbcDao<T extends BaseEntity>`, `dto/ApiResponse<T>` | ✅ `HackathonDao extends GenericDao<Hackathon, Long>`, `ApiResponse<List<HackathonView>>`. `T extends BaseEntity` is a bounded type. |
| Generics | `io/CsvReader<T>`, `io/CsvLineParser<T>` | ✅ `new CsvReader<>(new HackathonCsvParser())` gives a `CsvReader<Hackathon>` that returns `List<Hackathon>`. |
| Optional | `findById` everywhere | ✅ `HackathonService.findHackathon` and `ProfileService.findUser` use `orElseThrow`. `CollegeService.create` uses `findByName(...).isPresent()`. |
| LocalDate / ChronoUnit | `model/Hackathon.statusOn`, `util/DateUtils.describeDeadline` | ✅ `isBefore` / `isAfter` decide OPEN / CLOSED / ONGOING / COMPLETED. `ChronoUnit.DAYS.between` makes "Closes in 18 days". |
| Math.round | `util/SkillUtils.matchPercent` | ✅ `Math.round(100.0 * common / wanted)`: 2 of 3 skills → 67. |

## Unit 3: Files, serialization, logging, config

| Topic | File | How it is used |
|---|---|---|
| Byte streams | `io/PictureStorage.java` | ✅ Upload: `BufferedInputStream` (with `mark`/`reset` to peek at the JPEG/PNG signature bytes) → `BufferedOutputStream` in 8 KB chunks. Download: `BufferedInputStream` → `ByteArrayOutputStream`. |
| Character streams (read) | `service/HackathonImportService`, `io/CsvReader` | ✅ `InputStreamReader` (bytes → UTF-8 characters) wrapped in a `BufferedReader`, read with `readLine()`. |
| Character streams (write) | `io/CsvWriter`, `controller/CollegeController.exportCsv` | ✅ `BufferedWriter` with `write` / `newLine` / `flush` over an `OutputStreamWriter` (characters → UTF-8 bytes) on the HTTP response. Values with commas or quotes are quoted; a leading `=`/`+`/`-`/`@` gets a `'` (CSV injection). |
| Serialization | `model/RegistrationSnapshot`, `io/SnapshotStorage` | ✅ `implements Serializable`, `serialVersionUID = 1L`, `transient leaderEmail`. Approve writes it with `ObjectOutputStream` to `data/snapshots/{code}.ser`; the team page reads it back with `ObjectInputStream` and shows `leaderEmail` as null. An `ObjectInputFilter` allows only our class and plain JDK types (safe deserialization). `SnapshotStorageTest` checks the round trip. |
| Logging | services + `exception/GlobalExceptionHandler.java` | ✅ INFO: signup, login, hackathon created/updated/deleted, CSV import counts, college created. WARN: failed login, rule violations, skipped CSV lines, rolled-back batch. ERROR with stack trace: unexpected failures. Console + `logs/app.log`. |
| Properties | `config/PortalConfig.java` + `resources/portal.properties` | ✅ Loaded with `java.util.Properties` from the classpath, defaults for every key. |
| Spring config files | `resources/application.properties`, `application-reset.properties`, `backend/local.properties` | ✅ Spring settings with `${ENV:default}` placeholders; a profile file that only applies with `--spring.profiles.active=reset`; a git-ignored local file for the MySQL password. |
| try-with-resources | `config/PortalConfig`, `io/PictureStorage`, `service/HackathonImportService`, `dao/JdbcHackathonDao.insertAll` | ✅ Streams, readers and our own `Connection` are closed automatically. |
| java.nio.file | `config/PortalConfig.java`, `io/PictureStorage.java` | ✅ `Files.createDirectories`, `Files.newInputStream/newOutputStream`, `Path.normalize().startsWith()` to stay inside the upload folder. |

## Unit 4: JDBC

| Topic | File | How it is used |
|---|---|---|
| Schema / constraints | `resources/schema.sql` | ✅ 9 tables, UNIQUE, FOREIGN KEY (some `ON DELETE CASCADE`), CHECK, ENUM. The hackathon CHECKs repeat `Hackathon.checkRules()` as a safety net. |
| Subquery (NOT EXISTS) | `dao/JdbcTeamSeekerDao` | ✅ The "looking for a team" list skips anyone who is already in a team for that hackathon: `NOT EXISTS (SELECT 1 FROM team_members ...)`. |
| Seed data | `resources/data.sql`, `resources/reset.sql`, `resources/application-reset.properties` | ✅ The "reset" profile drops every table (`reset.sql`, children first because of foreign keys), recreates them (`schema.sql`) and loads the demo rows (`data.sql`): 1 admin, 2 colleges + reviewers, 8 students, 2 hackathons with dates relative to `CURDATE()`, a team in each status, pending requests, notifications. A normal start runs only `schema.sql`, so deleted rows never come back. |
| PreparedStatement + ResultSet in try-with-resources | `dao/AbstractJdbcDao.java` | ✅ Every query. |
| DataSourceUtils.getConnection | `dao/AbstractJdbcDao.java` | ✅ Joins the current Spring transaction if there is one. Released in `finally`. |
| Hand-written row mapping | `mapRow` in `JdbcUserDao`, `JdbcCollegeDao`, `JdbcHackathonDao` | ✅ Column by column, including `LocalDate` columns and nullable ones. |
| INSERT with generated keys | `dao/AbstractJdbcDao.insertAndGetId` | ✅ `Statement.RETURN_GENERATED_KEYS`. |
| SELECT / UPDATE / DELETE | `dao/JdbcHackathonDao` (`findAll`, `update`, `deleteById`, `countTeams`, GROUP BY count) | ✅ |
| Spring @Transactional | `service/CollegeService.create` | ✅ College row + reviewer user row commit together. A duplicate email on the second insert rolls back the first. |
| Spring @Transactional + SELECT ... FOR UPDATE | `service/RequestService.accept`, `dao/JdbcTeamDao.findByIdForUpdate` | ✅ Locks the team row, re-checks every rule, inserts the member, accepts the request, cancels other pending requests, sets FULL, notifies. The comment block above `accept` says what commits together and what rolls back. `Isolation.READ_COMMITTED` so reads after the lock see the newest data. Tested live: two accepts for the last seat at the same moment → exactly one joins. |
| @Transactional with a lock (other actions) | `service/TeamService.leaveTeam / removeMember / deleteTeam`, `service/RegistrationService.submit / approve / reject` | ✅ Lock the team (and registration) row, so a leave, an accept, a submit or two reviewers cannot interleave. `approve` writes the snapshot file inside the transaction: a failed write rolls the database back. |
| Manual transaction + batch | `dao/JdbcHackathonDao.insertAll` (called by `service/HackathonImportService`) | ✅ Own `Connection` from `dataSource.getConnection()`, `setAutoCommit(false)`, `addBatch` per row, `executeBatch`, then `commit()`, or `rollback()` on any `SQLException`. `setAutoCommit(true)` in `finally` before the connection goes back to the pool. |

## Security (supporting topic)

| Topic | File | How it is used |
|---|---|---|
| Password hashing | `config/SecurityConfig.passwordEncoder`, `service/AuthService`, `service/CollegeService` | ✅ BCrypt. Only the hash is stored. |
| JWT | `security/JwtService.java` | ✅ Signed HS256 token with user id + role, 24 h expiry, secret from properties. |
| Filter | `security/JwtAuthFilter.java` | ✅ Reads `Authorization: Bearer ...` once per request. |
| Role rules | `config/SecurityConfig.java` | ✅ `/api/admin/**` ADMIN, `/api/college/**` COLLEGE, `/api/auth/**` public. |
| Ownership checks | `service/ProfileService.updateProfile`, `service/TeamService` (`requireLeader`), `JoinRequest.isAnsweredBy / isSentBy`, `service/RegistrationService.requireMyCollege` | ✅ Users only edit their own profile. Only the leader edits/invites/removes/deletes/submits. Only the receiver accepts or rejects; only the sender cancels. A reviewer only sees and decides their own college's registrations. The user id always comes from the token. |
| Safe file names | `io/PictureStorage`, `io/SnapshotStorage.fileFor` | ✅ Random UUID names for pictures; snapshot codes must match `REG-\d{4}-\d{6}`, so no path can escape the folder. |
