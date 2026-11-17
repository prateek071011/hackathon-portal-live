package com.htf.portal.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.htf.portal.dao.CollegeDao;
import com.htf.portal.dao.UserDao;
import com.htf.portal.dto.PictureData;
import com.htf.portal.dto.ProfileForm;
import com.htf.portal.dto.ProfileView;
import com.htf.portal.exception.ForbiddenActionException;
import com.htf.portal.exception.NotFoundException;
import com.htf.portal.io.PictureStorage;
import com.htf.portal.model.College;
import com.htf.portal.model.User;
import com.htf.portal.util.SkillUtils;

/**
 * The "My Profile" features: view and edit your details, upload a picture,
 * and read any user's picture (team pages show member pictures).
 */
@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final UserDao userDao;
    private final CollegeDao collegeDao;
    private final PictureStorage pictureStorage;

    public ProfileService(UserDao userDao, CollegeDao collegeDao, PictureStorage pictureStorage) {
        this.userDao = userDao;
        this.collegeDao = collegeDao;
        this.pictureStorage = pictureStorage;
    }

    public ProfileView getProfile(Long userId) {
        User user = findUser(userId);
        return ProfileView.from(user, collegeNameOf(user));
    }

    public ProfileView updateProfile(Long userId, ProfileForm form) {
        User user = findUser(userId);
        if (!user.isStudent()) {
            throw new ForbiddenActionException("Only student profiles can be edited");
        }
        user.setName(form.name().trim());
        user.setRollNumber(form.rollNumber().trim());
        user.setBranch(form.branch().trim());
        user.setStudyYear(form.year());
        user.setSkills(SkillUtils.normalize(form.skills()));
        userDao.update(user);
        log.info("Profile updated: userId={}", userId);
        return ProfileView.from(user, collegeNameOf(user));
    }

    public ProfileView uploadPicture(Long userId, MultipartFile file) {
        User user = findUser(userId);
        String oldFile = user.getPictureFile();

        String newFile;
        try {
            newFile = pictureStorage.save(file);
        } catch (IOException e) {
            // A disk problem is unexpected: let the global handler log it and answer 500.
            throw new UncheckedIOException("Could not save picture for user " + userId, e);
        }
        userDao.updatePicture(userId, newFile);
        user.setPictureFile(newFile);

        // Remove the old file only after the database points at the new one.
        if (oldFile != null) {
            pictureStorage.delete(oldFile);
        }
        log.info("Picture uploaded: userId={}, file={}", userId, newFile);
        return ProfileView.from(user, collegeNameOf(user));
    }

    public PictureData getPicture(Long userId) {
        User user = findUser(userId);
        if (!user.hasPicture()) {
            throw new NotFoundException("This user has no picture");
        }
        try {
            byte[] bytes = pictureStorage.read(user.getPictureFile());
            return new PictureData(bytes, PictureStorage.contentTypeFor(user.getPictureFile()));
        } catch (NoSuchFileException e) {
            log.warn("Picture file missing on disk: userId={}, file={}", userId, user.getPictureFile());
            throw new NotFoundException("This user's picture file is missing");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read picture for user " + userId, e);
        }
    }

    private User findUser(Long userId) {
        return userDao.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
    }

    private String collegeNameOf(User user) {
        if (user.getCollegeId() == null) {
            return null;   // the admin has no college
        }
        Optional<College> college = collegeDao.findById(user.getCollegeId());
        if (college.isEmpty()) {
            return null;
        }
        return college.get().getName();
    }
}
