package com.smartshop.user.service;

import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.user.dto.UserCreateRequest;
import com.smartshop.user.dto.UserResponse;
import com.smartshop.user.entity.User;
import com.smartshop.user.mapper.UserMapper;
import com.smartshop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String username = normalize(request.username());
        String email = normalize(request.email());
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("Nom d'utilisateur deja utilise : " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email deja utilise : " + email);
        }
        User user = new User(username, email, passwordEncoder.encode(request.password()),
                request.firstName().trim(), request.lastName().trim(), request.role());
        return userMapper.toResponse(userRepository.save(user));
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
}
