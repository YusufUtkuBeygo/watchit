package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.PasswordChangeRequestDto;
import com.yutkubeygo.watchit.dto.UserUpdateRequestDto;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.exception.ForbiddenException;
import com.yutkubeygo.watchit.exception.TooManyRequestsException;
import com.yutkubeygo.watchit.mapper.UserMapper;
import com.yutkubeygo.watchit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//Şifre değiştirme ve e-posta değiştirme: şu anki şifre doğru olmadan hiçbir şey değişmez.
//Yanlış denemeler giriş sayacına yazılır, kilitliyse 429.
@ExtendWith(MockitoExtension.class)
class UserServicePasswordTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private LoginAttemptService loginAttemptService;
    @Mock private EmailVerificationService emailVerificationService;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp()
    {
        user = new User();
        user.setId(1L);
        user.setUsername("ayse");
        user.setEmail("ayse@test.local");
        user.setPassword("HASH");
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        lenient().when(passwordEncoder.matches("dogru-sifre", "HASH")).thenReturn(true);
    }

    private PasswordChangeRequestDto passwordChange(String current)
    {
        PasswordChangeRequestDto request = new PasswordChangeRequestDto();
        request.setCurrentPassword(current);
        request.setNewPassword("yeni-sifre-123");
        return request;
    }

    private UserUpdateRequestDto update(String email, String currentPassword)
    {
        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setUsername("ayse");
        request.setEmail(email);
        request.setCurrentPassword(currentPassword);
        return request;
    }

    @Test
    void correctPasswordChangesPassword()
    {
        when(passwordEncoder.encode("yeni-sifre-123")).thenReturn("YENI-HASH");
        when(jwtService.generateToken(user)).thenReturn("YENI-TOKEN");

        //Şifre değişince yeni token döner ve değişme zamanı yazılır (eski oturumlar kapansın diye)
        assertThat(userService.changePassword(1L, passwordChange("dogru-sifre"))).isEqualTo("YENI-TOKEN");
        assertThat(user.getPassword()).isEqualTo("YENI-HASH");
        assertThat(user.getPasswordChangedAt()).isNotNull();
        verify(loginAttemptService).recordSuccess("ayse@test.local");
    }

    @Test
    void wrongPasswordIsForbiddenAndCounted()
    {
        assertThatThrownBy(() -> userService.changePassword(1L, passwordChange("yanlis")))
                .isInstanceOf(ForbiddenException.class);

        assertThat(user.getPassword()).isEqualTo("HASH");
        verify(loginAttemptService).recordFailure("ayse@test.local");
        verify(userRepository, never()).save(any());
    }

    @Test
    void blockedAccountGets429EvenWithCorrectPassword()
    {
        when(loginAttemptService.isBlocked("ayse@test.local")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(1L, passwordChange("dogru-sifre")))
                .isInstanceOf(TooManyRequestsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void missingUserReturnsNull()
    {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.changePassword(99L, passwordChange("dogru-sifre"))).isNull();
    }

    @Test
    void sameEmailNeedsNoPassword()
    {
        userService.updateUser(1L, update(" Ayse@Test.local ", null));

        verify(loginAttemptService, never()).isBlocked(any());
        verify(userRepository).save(user);
    }

    @Test
    void newEmailWithoutCorrectPasswordIsForbidden()
    {
        assertThatThrownBy(() -> userService.updateUser(1L, update("yeni@test.local", null)))
                .isInstanceOf(ForbiddenException.class);

        assertThat(user.getEmail()).isEqualTo("ayse@test.local");
        verify(userRepository, never()).save(any());
    }

    @Test
    void newEmailWithCorrectPasswordIsSaved()
    {
        userService.updateUser(1L, update("Yeni@Test.local", "dogru-sifre"));

        assertThat(user.getEmail()).isEqualTo("yeni@test.local");
        verify(userRepository).save(user);
    }
}
