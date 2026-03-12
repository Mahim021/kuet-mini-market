package com.kuet.minimarket.service;

import com.kuet.minimarket.entity.Role;
import com.kuet.minimarket.entity.RoleName;
import com.kuet.minimarket.entity.User;
import com.kuet.minimarket.repository.UserRepository;
import com.kuet.minimarket.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadUserByUsername_found() {
        User user = User.builder()
                .email("test@test.com")
                .password("encoded")
                .enabled(true)
                .roles(new HashSet<>(Set.of(new Role(1L, RoleName.BUYER))))
                .build();

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("test@test.com");

        assertThat(details.getUsername()).isEqualTo("test@test.com");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_BUYER");
    }

    @Test
    void loadUserByUsername_disabledUser_returnsDisabledDetails() {
        User user = User.builder()
                .email("disabled@test.com")
                .password("encoded")
                .enabled(false)
                .roles(new HashSet<>())
                .build();

        when(userRepository.findByEmail("disabled@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("disabled@test.com");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_notFound_throws() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nobody@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("nobody@test.com");
    }
}
