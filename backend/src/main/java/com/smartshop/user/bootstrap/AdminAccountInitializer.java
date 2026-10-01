package com.smartshop.user.bootstrap;

import com.smartshop.user.dto.UserCreateRequest;
import com.smartshop.user.entity.Role;
import com.smartshop.user.repository.UserRepository;
import com.smartshop.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * L'inscription etant reservee aux ADMIN, le premier administrateur est cree ici,
 * uniquement si aucun utilisateur n'existe encore.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final UserService userService;
    private final BootstrapAdminProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        if (!StringUtils.hasText(properties.password())) {
            log.warn("Aucun utilisateur en base : definir ADMIN_PASSWORD pour creer le compte administrateur initial.");
            return;
        }
        if (properties.password().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD doit contenir au moins " + MIN_PASSWORD_LENGTH + " caracteres");
        }
        userService.create(new UserCreateRequest(properties.username(), properties.email(), properties.password(),
                "Administrateur", "SmartShop", Role.ADMIN));
        log.info("Compte administrateur initial cree : {}", properties.username());
    }
}
