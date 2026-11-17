package com.htf.portal.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.htf.portal.dao.UserDao;
import com.htf.portal.model.Role;

/**
 * Runs once after the backend has started (ApplicationRunner). If there is no admin account yet, the database
 * is empty, and nobody could log in, so it logs how to load the demo data instead of failing silently.
 */
@Component
public class StartupCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupCheck.class);

    private final UserDao userDao;

    public StartupCheck(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userDao.findByRole(Role.ADMIN).isEmpty()) {
            log.warn("The database has no admin account. Start once with the 'reset' profile to load the demo data: "
                    + ".\\mvnw.cmd spring-boot:run \"-Dspring-boot.run.profiles=reset\"");
        } else {
            log.info("Backend ready. Demo logins are listed in README.md");
        }
    }
}
