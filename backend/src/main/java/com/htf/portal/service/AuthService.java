package com.htf.portal.service;

import static com.htf.portal.util.TextUtils.normalizeEmail;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.htf.portal.config.PortalConfig;
import com.htf.portal.dao.CollegeDao;
import com.htf.portal.dao.UserDao;
import com.htf.portal.dto.LoginForm;
import com.htf.portal.dto.LoginResult;
import com.htf.portal.dto.SignupForm;
import com.htf.portal.dto.UserSummary;
import com.htf.portal.exception.ConflictException;
import com.htf.portal.exception.DuplicateKeyException;
import com.htf.portal.exception.LoginFailedException;
import com.htf.portal.exception.NotFoundException;
import com.htf.portal.model.College;
import com.htf.portal.model.Role;
import com.htf.portal.model.User;
import com.htf.portal.security.JwtService;
import com.htf.portal.util.SkillUtils;
import com.htf.portal.util.UserCodeGenerator;

/**
 * Student signup and login for every role. Passwords are hashed with BCrypt on signup and
 * compared with the hash on login. Both return a JWT so the user is logged in straight away.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_CODE_ATTEMPTS = 10;
    private static final String EMAIL_TAKEN = "An account with this email already exists";

    private final UserDao userDao;
    private final CollegeDao collegeDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PortalConfig portalConfig;

    public AuthService(UserDao userDao, CollegeDao collegeDao, PasswordEncoder passwordEncoder,
                       JwtService jwtService, PortalConfig portalConfig) {
        this.userDao = userDao;
        this.collegeDao = collegeDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.portalConfig = portalConfig;
    }

    public LoginResult signup(SignupForm form) {
        String email = normalizeEmail(form.email());
        if (userDao.existsByEmail(email)) {
            throw new ConflictException(EMAIL_TAKEN);
        }
        College college = collegeDao.findById(form.collegeId())
                .orElseThrow(() -> new NotFoundException("College", form.collegeId()));

        User user = new User(form.name().trim(), email, passwordEncoder.encode(form.password()),
                Role.STUDENT, college.getId());
        user.setUserCode(newUniqueUserCode());
        user.setRollNumber(form.rollNumber().trim());
        user.setBranch(form.branch().trim());
        user.setStudyYear(form.year());
        user.setSkills(SkillUtils.normalize(form.skills()));

        try {
            userDao.insert(user);
        } catch (DuplicateKeyException e) {
            // Two signups with the same email at the same moment: our check above passed for both,
            // so the database's UNIQUE(email) rule is what stops the second one.
            throw new ConflictException(EMAIL_TAKEN);
        }
        log.info("New student signed up: id={}, userCode={}, collegeId={}", user.getId(), user.getUserCode(), college.getId());
        return new LoginResult(jwtService.createToken(user), UserSummary.from(user));
    }

    public LoginResult login(LoginForm form) {
        Optional<User> found = userDao.findByEmail(normalizeEmail(form.email()));
        // The same error whether the email is unknown or the password is wrong,
        // so nobody can use the login form to find out which emails are registered.
        if (found.isEmpty() || !passwordEncoder.matches(form.password(), found.get().getPasswordHash())) {
            log.warn("Failed login attempt");
            throw new LoginFailedException();
        }
        User user = found.get();
        log.info("User logged in: id={}, role={}", user.getId(), user.getRole());
        return new LoginResult(jwtService.createToken(user), UserSummary.from(user));
    }

    // Random codes can (very rarely) repeat, so we try again until we find an unused one.
    private String newUniqueUserCode() {
        for (int attempt = 1; attempt <= MAX_CODE_ATTEMPTS; attempt++) {
            String code = UserCodeGenerator.generate(portalConfig.getUserCodePrefix());
            if (!userDao.existsByUserCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not find a free user code after " + MAX_CODE_ATTEMPTS + " attempts");
    }

}
