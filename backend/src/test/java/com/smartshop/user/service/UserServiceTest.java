package com.smartshop.user.service;

import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.user.dto.UserCreateRequest;
import com.smartshop.user.dto.UserResponse;
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

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, Mappers.getMapper(UserMapper.class), passwordEncoder);
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
                .hasMessageContaining("Nom d'utilisateur deja utilise");
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_withExistingEmail_throwsBusinessException() {
        when(userRepository.existsByEmail("sami@shop.tn")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request("sami", "SAMI@shop.tn")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email deja utilise");
        verify(userRepository, never()).save(any());
    }

    @Test
    void getByUsername_unknownUser_throwsNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByUsername("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static UserCreateRequest request(String username, String email) {
        return new UserCreateRequest(username, email, "Secret123!", "Sami", "Ben Ali", Role.SELLER);
    }
}
