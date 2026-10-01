package com.smartshop.user.bootstrap;

import com.smartshop.user.dto.UserCreateRequest;
import com.smartshop.user.entity.Role;
import com.smartshop.user.repository.UserRepository;
import com.smartshop.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountInitializerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;

    @Test
    void createsAdminWhenNoUserExists() {
        when(userRepository.count()).thenReturn(0L);

        initializer("Admin-Password-1").run(null);

        ArgumentCaptor<UserCreateRequest> request = ArgumentCaptor.forClass(UserCreateRequest.class);
        verify(userService).create(request.capture());
        assertThat(request.getValue().role()).isEqualTo(Role.ADMIN);
        assertThat(request.getValue().username()).isEqualTo("admin");
    }

    @Test
    void doesNothingWhenUsersAlreadyExist() {
        when(userRepository.count()).thenReturn(3L);

        initializer("Admin-Password-1").run(null);

        verifyNoInteractions(userService);
    }

    @Test
    void doesNothingWhenPasswordIsNotConfigured() {
        when(userRepository.count()).thenReturn(0L);

        initializer("").run(null);

        verifyNoInteractions(userService);
    }

    @Test
    void failsWhenPasswordIsTooShort() {
        when(userRepository.count()).thenReturn(0L);

        assertThatThrownBy(() -> initializer("short").run(null))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(userService);
    }

    private AdminAccountInitializer initializer(String password) {
        return new AdminAccountInitializer(userRepository, userService,
                new BootstrapAdminProperties("admin", "admin@smartshop.local", password));
    }
}
