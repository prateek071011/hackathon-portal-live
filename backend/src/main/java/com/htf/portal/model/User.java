package com.htf.portal.model;

/**
 * One row of the users table: a student, a college reviewer or the admin.
 * Student-only fields (userCode, rollNumber, branch, studyYear, skills) stay null for other roles.
 * The password is only ever stored as a BCrypt hash.
 */
public class User extends BaseEntity {

    private String name;
    private String email;
    private String passwordHash;
    private Role role;
    private Long collegeId;

    // Student-only fields
    private String userCode;
    private String rollNumber;
    private String branch;
    private Integer studyYear;
    private String skills;        // comma-separated, e.g. "java, react, sql"
    private String pictureFile;   // random file name in the upload folder, or null

    // Used by the DAO, which fills the fields with setters while reading a row.
    public User() {
    }

    // Overloaded constructor for accounts that only need the basics (admin, college reviewer).
    public User(String name, String email, String passwordHash, Role role, Long collegeId) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.collegeId = collegeId;
    }

    public boolean isStudent() {
        return role == Role.STUDENT;
    }

    public boolean hasPicture() {
        return pictureFile != null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getCollegeId() {
        return collegeId;
    }

    public void setCollegeId(Long collegeId) {
        this.collegeId = collegeId;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Integer getStudyYear() {
        return studyYear;
    }

    public void setStudyYear(Integer studyYear) {
        this.studyYear = studyYear;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getPictureFile() {
        return pictureFile;
    }

    public void setPictureFile(String pictureFile) {
        this.pictureFile = pictureFile;
    }
}
