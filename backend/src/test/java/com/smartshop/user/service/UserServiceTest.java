package com.smartshop.user.service;

import com.smartshop.auth.repository.RefreshTokenRepository;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.security.CurrentUserProvider;
import com.smartshop.user.dto.PasswordResetRequest;
import com.smartshop.user.dto.UserCreateRequest;
import com.smartshop.user.dto.UserResponse;
import com.smartshop.user.dto.UserUpdateRequest;
import com.smartshop.user.entity.Role;
import com.smartshop.user.entity.User;
import com.smartshop.user.mapper.UserMapper;
import com.smartshop.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-05T10:00:00Z"), ZoneOffset.UTC);
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, Mappers.getMapper(UserMapper.class), passwordEncoder,
                refreshTokenRepository, currentUserProvider, clock);
    }

    @Test
    void create_normalizesIdentifiersAndHashesPassword() {
        when(passwordEncoder.encode("Secret123!")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.create(new UserCreateRequest(
                "  Sami.Seller ", "Sami@Shop.TN", "Secret123!", " Sami ", "Ben Ali", Role.SELLER));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("sami.seller");
        assertThat(saved.getValue().getEmail()).isEqualTo("sami@shop.tn");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getValue().getFirstName()).isEqualTo("Sami");
        assertThat(saved.getValue().isEnabled()).isTrue();
        assertThat(response.username()).isEqualTo("sami.seller");
        assertThat(response.role()).isEqualTo(Role.SELLER);
    }

    @Test
    void create_withExistingUsername_throwsBusinessException() {
        when(userRepository.existsByUsername("sami")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request("Sami", "sami@shop.tn")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Nom d'utilisateur déjà utilisé");
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_withExistingEmail_throwsBusinessException() {
        when(userRepository.existsByEmail("sami@shop.tn")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request("sami", "SAMI@shop.tn")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email déjà utilisé");
        verify(userRepository, never()).save(any());
    }

    @Test
    void getByUsername_unknownUser_throwsNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByUsername("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- update / status / password ----------

    @Test
    void update_normalizesEmailAndChangesProfile() {
        User seller = user(2L, "sami", Role.SELLER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(currentUserProvider.currentUser()).thenReturn(user(1L, "admin", Role.ADMIN));

        UserResponse response = userService.update(2L,
                new UserUpdateRequest(" Sami@Shop.TN ", " Sami ", "Ben Ali", Role.MANAGER));

        assertThat(response.email()).isEqualTo("sami@shop.tn");
        assertThat(response.firstName()).isEqualTo("Sami");
        assertThat(response.role()).isEqualTo(Role.MANAGER);
    }

    @Test
    void update_withEmailOfAnotherUser_throwsBeforeModifying() {
        User seller = user(2L, "sami", Role.SELLER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(userRepository.existsByEmailAndIdNot("taken@shop.tn", 2L)).thenReturn(true);

        assertThatThrownBy(() -> userService.update(2L,
                new UserUpdateRequest("taken@shop.tn", "Sami", "Ben Ali", Role.SELLER)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email déjà utilisé");
        assertThat(seller.getEmail()).isEqualTo("sami@shop.test");
    }

    @Test
    void update_ownRole_isRefused() {
        User admin = user(1L, "admin", Role.ADMIN);
        when(userRepository.lockActiveAdmins()).thenReturn(List.of(admin, user(3L, "boss", Role.ADMIN)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(currentUserProvider.currentUser()).thenReturn(admin);

        assertThatThrownBy(() -> userService.update(1L,
                new UserUpdateRequest("admin@shop.tn", "A", "B", Role.SELLER)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("propre rôle");
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void update_demotingLastActiveAdmin_isRefused() {
        User lastAdmin = user(3L, "boss", Role.ADMIN);
        when(userRepository.lockActiveAdmins()).thenReturn(List.of(lastAdmin));
        when(userRepository.findById(3L)).thenReturn(Optional.of(lastAdmin));
        when(currentUserProvider.currentUser()).thenReturn(user(1L, "disabled-admin", Role.ADMIN));

        assertThatThrownBy(() -> userService.update(3L,
                new UserUpdateRequest("boss@shop.tn", "A", "B", Role.MANAGER)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("au moins un administrateur actif");
        assertThat(lastAdmin.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void disable_revokesSessions() {
        User seller = user(2L, "sami", Role.SELLER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(currentUserProvider.currentUser()).thenReturn(user(1L, "admin", Role.ADMIN));

        UserResponse response = userService.updateStatus(2L, false);

        assertThat(response.enabled()).isFalse();
        verify(refreshTokenRepository).revokeAllActiveByUserId(2L, clock.instant());
    }

    @Test
    void disable_ownAccount_isRefused() {
        User admin = user(1L, "admin", Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(currentUserProvider.currentUser()).thenReturn(admin);

        assertThatThrownBy(() -> userService.updateStatus(1L, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("propre compte");
        assertThat(admin.isEnabled()).isTrue();
        verify(refreshTokenRepository, never()).revokeAllActiveByUserId(any(), any());
    }

    @Test
    void disable_lastActiveAdmin_isRefused() {
        User lastAdmin = user(3L, "boss", Role.ADMIN);
        when(userRepository.lockActiveAdmins()).thenReturn(List.of(lastAdmin));
        when(userRepository.findById(3L)).thenReturn(Optional.of(lastAdmin));
        when(currentUserProvider.currentUser()).thenReturn(user(1L, "disabled-admin", Role.ADMIN));

        assertThatThrownBy(() -> userService.updateStatus(3L, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("au moins un administrateur actif");
        assertThat(lastAdmin.isEnabled()).isTrue();
    }

    @Test
    void resetPassword_hashesAndRevokesSessions() {
        User seller = user(2L, "sami", Role.SELLER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(passwordEncoder.encode("NewSecret1!")).thenReturn("new-hash");

        userService.resetPassword(2L, new PasswordResetRequest("NewSecret1!"));

        assertThat(seller.getPasswordHash()).isEqualTo("new-hash");
        verify(refreshTokenRepository).revokeAllActiveByUserId(2L, clock.instant());
    }

    private static User user(Long id, String username, Role role) {
        User user = new User(username, username + "@shop.test", "hash", "Prenom", "Nom", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static UserCreateRequest request(String username, String email) {
        return new UserCreateRequest(username, email, "Secret123!", "Sami", "Ben Ali", Role.SELLER);
    }
}
