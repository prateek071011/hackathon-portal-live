package com.htf.portal.service;

import static com.htf.portal.util.TextUtils.normalizeEmail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.htf.portal.dao.CollegeDao;
import com.htf.portal.dao.UserDao;
import com.htf.portal.dto.CollegeForm;
import com.htf.portal.dto.CollegeView;
import com.htf.portal.exception.ConflictException;
import com.htf.portal.exception.DuplicateKeyException;
import com.htf.portal.model.College;
import com.htf.portal.model.Role;
import com.htf.portal.model.User;

/**
 * Colleges: the list for the signup dropdown, and the admin's "create college", which also
 * creates the COLLEGE login account that reviews that college's teams.
 */
@Service
public class CollegeService {

    private static final Logger log = LoggerFactory.getLogger(CollegeService.class);

    private final CollegeDao collegeDao;
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public CollegeService(CollegeDao collegeDao, UserDao userDao, PasswordEncoder passwordEncoder) {
        this.collegeDao = collegeDao;
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    public List<College> listAll() {
        return collegeDao.findAll();
    }

    /** Every college with its reviewer account, for the admin page. */
    public List<CollegeView> listWithReviewers() {
        // Put the reviewers in a HashMap (collegeId -> reviewer) so each college can find its own in one step.
        Map<Long, User> reviewerByCollege = new HashMap<>();
        for (User reviewer : userDao.findByRole(Role.COLLEGE)) {
            reviewerByCollege.put(reviewer.getCollegeId(), reviewer);
        }

        List<CollegeView> views = new ArrayList<>();
        for (College college : collegeDao.findAll()) {
            User reviewer = reviewerByCollege.get(college.getId());
            if (reviewer == null) {
                views.add(new CollegeView(college.getId(), college.getName(), null, null));
            } else {
                views.add(new CollegeView(college.getId(), college.getName(), reviewer.getName(), reviewer.getEmail()));
            }
        }
        return views;
    }

    /**
     * Creates the college AND its reviewer login as one Spring transaction: both rows are saved, or neither.
     * Example: the college insert works but the email turns out to be taken. The DuplicateKeyException becomes a
     * ConflictException (unchecked), Spring sees it leave the method and rolls the college insert back too.
     */
    @Transactional
    public CollegeView create(CollegeForm form) {
        String name = form.name().trim();
        String email = normalizeEmail(form.email());
        if (collegeDao.findByName(name).isPresent()) {
            throw new ConflictException("A college called '" + name + "' already exists");
        }
        if (userDao.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        College college = new College(name);
        User reviewer = new User(form.reviewerName().trim(), email, passwordEncoder.encode(form.password()),
                Role.COLLEGE, null);
        try {
            collegeDao.insert(college);
            reviewer.setCollegeId(college.getId());
            userDao.insert(reviewer);
        } catch (DuplicateKeyException e) {
            throw new ConflictException("That college name or email was just taken. Please try again.");
        }
        log.info("College created: id={}, name={}, reviewerId={}", college.getId(), name, reviewer.getId());
        return new CollegeView(college.getId(), name, reviewer.getName(), reviewer.getEmail());
    }
}
