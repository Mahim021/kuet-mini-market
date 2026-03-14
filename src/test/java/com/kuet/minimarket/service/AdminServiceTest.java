package com.kuet.minimarket.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kuet.minimarket.dto.UserResponse;
import com.kuet.minimarket.entity.Role;
import com.kuet.minimarket.entity.RoleName;
import com.kuet.minimarket.entity.User;
import com.kuet.minimarket.exception.ForbiddenException;
import com.kuet.minimarket.exception.ResourceNotFoundException;
import com.kuet.minimarket.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private AdminService adminService;

    private User activeUser;
    private User inactiveUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        Set<Role> buyerRoles = new HashSet<>(Set.of(new Role(1L, RoleName.BUYER)));
        Set<Role> adminRoles = new HashSet<>(Set.of(new Role(3L, RoleName.ADMIN)));

        activeUser = User.builder()
                .id(1L).fullName("Active User").email("active@test.com")
                .password("enc").enabled(true).roles(buyerRoles).build();

        inactiveUser = User.builder()
                .id(2L).fullName("Inactive User").email("inactive@test.com")
                .password("enc").enabled(false).roles(buyerRoles).build();

        adminUser = User.builder()
                .id(3L).fullName("Admin User").email("admin@test.com")
                .password("enc").enabled(true).roles(adminRoles).build();
    }

    // Test 11: getAllUsers excludes the currently logged-in admin from the list
    @Test
    void getAllUsers_excludesCurrentAdmin() {
        when(userRepository.findAll()).thenReturn(List.of(activeUser, inactiveUser, adminUser));

        List<UserResponse> result = adminService.getAllUsers(3L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmail()).isEqualTo("active@test.com");
        assertThat(result.get(0).getRoles()).contains("BUYER");
        assertThat(result.get(1).isEnabled()).isFalse();
        assertThat(result).noneMatch(u -> u.getEmail().equals("admin@test.com"));
    }

    // Test 12: activateUser sets enabled = true and saves
    @Test
    void activateUser_setsEnabledTrueAndSaves() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(inactiveUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse result = adminService.activateUser(2L, 3L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isEnabled()).isTrue();
        assertThat(result.isEnabled()).isTrue();
    }

    // Test 13: deactivateUser sets enabled = false and saves
    @Test
    void deactivateUser_setsEnabledFalseAndSaves() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse result = adminService.deactivateUser(1L, 3L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isEnabled()).isFalse();
        assertThat(result.isEnabled()).isFalse();
    }

    // Test 14: activateUser throws when user not found
    @Test
    void activateUser_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.activateUser(999L, 3L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
        verify(userRepository, never()).save(any());
    }

    // Test 15: deactivateUser throws when user not found
    @Test
    void deactivateUser_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(888L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deactivateUser(888L, 3L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("888");
        verify(userRepository, never()).save(any());
    }

    // Test 16: deactivateUser throws ForbiddenException when admin tries to deactivate themselves
    @Test
    void deactivateUser_selfDeactivate_throwsForbiddenException() {
        assertThatThrownBy(() -> adminService.deactivateUser(3L, 3L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own");
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    // Test 17: activateUser throws ForbiddenException when admin tries to modify their own account
    @Test
    void activateUser_selfModify_throwsForbiddenException() {
        assertThatThrownBy(() -> adminService.activateUser(3L, 3L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own");
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    // Test 18: getAllUsers with empty repository returns empty list
    @Test
    void getAllUsers_emptyRepository_returnsEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserResponse> result = adminService.getAllUsers(3L);

        assertThat(result).isEmpty();
        verify(userRepository).findAll();
    }
}
