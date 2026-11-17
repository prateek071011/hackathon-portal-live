package com.htf.portal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.htf.portal.config.PortalConfig;
import com.htf.portal.dao.CollegeDao;
import com.htf.portal.dao.UserDao;
import com.htf.portal.dto.LoginForm;
import com.htf.portal.dto.LoginResult;
import com.htf.portal.dto.SignupForm;
import com.htf.portal.exception.ConflictException;
import com.htf.portal.exception.LoginFailedException;
import com.htf.portal.model.College;
import com.htf.portal.model.Role;
import com.htf.portal.model.User;
import com.htf.portal.security.JwtService;

/**
 * Unit tests for AuthService. The DAOs, password encoder and JWT service are Mockito mocks,
 * so these tests need no database and run in milliseconds.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserDao userDao;
    @Mock
    private CollegeDao collegeDao;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private PortalConfig portalConfig;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userDao, collegeDao, passwordEncoder, jwtService, portalConfig);
    }

    private SignupForm signupForm(String email) {
        return new SignupForm("Asha Rao", email, "secret123", 1L, "21CS001", "CSE", 3, "Java, REACT, java");
    }

    @Test
    void signupCreatesStudentWithUserCodeAndCleanSkills() {
        when(userDao.existsByEmail("asha@example.com")).thenReturn(false);
        when(collegeDao.findById(1L)).thenReturn(Optional.of(new College(1L, "Greenfield", null)));
        when(portalConfig.getUserCodePrefix()).thenReturn("HTF-");
        when(userDao.existsByUserCode(anyString())).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(jwtService.createToken(any(User.class))).thenReturn("token-123");

        // The email is typed with capitals and spaces: signup must store it trimmed and lower-case.
        LoginResult result = authService.signup(signupForm("  Asha@Example.com "));

        assertEquals("token-123", result.token());
        assertEquals("asha@example.com", result.user().email());
        assertEquals(Role.STUDENT, result.user().role());
        assertTrue(result.user().userCode().matches("HTF-[2-9A-HJ-NP-Z]{6}"));
        verify(userDao).insert(any(User.class));
    }

    @Test
    void signupRejectsAnEmailThatIsAlreadyRegistered() {
        when(userDao.existsByEmail("asha@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.signup(signupForm("asha@example.com")));
        verify(userDao, never()).insert(any(User.class));
    }

    @Test
    void loginWithWrongPasswordAndUnknownEmailGiveTheSameMessage() {
        User user = new User("Asha Rao", "asha@example.com", "hashed", Role.STUDENT, 1L);
        when(userDao.findByEmail("asha@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);
        when(userDao.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        LoginFailedException wrongPassword = assertThrows(LoginFailedException.class,
                () -> authService.login(new LoginForm("asha@example.com", "wrong-password")));
        LoginFailedException unknownEmail = assertThrows(LoginFailedException.class,
                () -> authService.login(new LoginForm("nobody@example.com", "anything")));

        assertEquals(wrongPassword.getMessage(), unknownEmail.getMessage());
    }
}
