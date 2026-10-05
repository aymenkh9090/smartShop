package com.smartshop.user.service;

import com.smartshop.auth.repository.RefreshTokenRepository;
import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.security.CurrentUserProvider;
import com.smartshop.user.dto.PasswordResetRequest;
import com.smartshop.user.dto.UserCreateRequest;
import com.smartshop.user.dto.UserFilter;
import com.smartshop.user.dto.UserResponse;
import com.smartshop.user.dto.UserUpdateRequest;
import com.smartshop.user.entity.Role;
import com.smartshop.user.entity.User;
import com.smartshop.user.mapper.UserMapper;
import com.smartshop.user.repository.UserRepository;
import com.smartshop.user.repository.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(UserFilter filter, Pageable pageable) {
        return PageResponse.from(userRepository.findAll(UserSpecifications.matching(filter), pageable)
                .map(userMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userMapper.toResponse(getEntity(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String username = normalize(request.username());
        String email = normalize(request.email());
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("Nom d'utilisateur déjà utilisé : " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email déjà utilisé : " + email);
        }
        User user = new User(username, email, passwordEncoder.encode(request.password()),
                request.firstName().trim(), request.lastName().trim(), request.role());
        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Un administrateur ne peut pas modifier son propre role, et le dernier ADMIN actif ne peut pas etre retrograde.
     */
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        String email = normalize(request.email());
        boolean demotion = request.role() != Role.ADMIN;
        List<User> activeAdmins = demotion ? userRepository.lockActiveAdmins() : List.of();
        User user = getEntity(id);
        if (request.role() != user.getRole() && isCurrentUser(user)) {
            throw new BusinessException("Vous ne pouvez pas modifier votre propre rôle");
        }
        if (demotion && user.isActiveAdmin()) {
            requireAnotherActiveAdmin(user, activeAdmins);
        }
        // Controle avant toute modification de l'entite managee (piege auto-flush)
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessException("Email déjà utilisé : " + email);
        }
        user.updateProfile(email, request.firstName().trim(), request.lastName().trim(), request.role());
        return userMapper.toResponse(user);
    }

    /** Desactiver un compte revoque aussi ses sessions (refresh tokens). */
    @Transactional
    public UserResponse updateStatus(Long id, boolean enabled) {
        if (enabled) {
            User user = getEntity(id);
            user.enable();
            return userMapper.toResponse(user);
        }
        List<User> activeAdmins = userRepository.lockActiveAdmins();
        User user = getEntity(id);
        if (isCurrentUser(user)) {
            throw new BusinessException("Vous ne pouvez pas désactiver votre propre compte");
        }
        if (user.isActiveAdmin()) {
            requireAnotherActiveAdmin(user, activeAdmins);
        }
        user.disable();
        refreshTokenRepository.revokeAllActiveByUserId(user.getId(), clock.instant());
        return userMapper.toResponse(user);
    }

    /** Nouveau mot de passe defini par un ADMIN : les sessions existantes de l'utilisateur sont revoquees. */
    @Transactional
    public UserResponse resetPassword(Long id, PasswordResetRequest request) {
        User user = getEntity(id);
        user.changePasswordHash(passwordEncoder.encode(request.password()));
        refreshTokenRepository.revokeAllActiveByUserId(user.getId(), clock.instant());
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", username));
    }

    /** Identifiants insensibles a la casse : toujours stockes et recherches en minuscules. */
    public static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private User getEntity(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
    }

    private boolean isCurrentUser(User user) {
        return user.getId().equals(currentUserProvider.currentUser().getId());
    }

    /** {@code activeAdmins} : resultat de {@link UserRepository#lockActiveAdmins()} dans la transaction courante. */
    private static void requireAnotherActiveAdmin(User admin, List<User> activeAdmins) {
        boolean anotherAdmin = activeAdmins.stream()
                .anyMatch(other -> !other.getId().equals(admin.getId()));
        if (!anotherAdmin) {
            throw new BusinessException("Impossible : il doit rester au moins un administrateur actif");
        }
    }
}
