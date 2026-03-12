package com.kuet.minimarket.service;

import com.kuet.minimarket.dto.UserResponse;
import com.kuet.minimarket.entity.Role;
import com.kuet.minimarket.entity.RoleName;
import com.kuet.minimarket.entity.User;
import com.kuet.minimarket.exception.ResourceNotFoundException;
import com.kuet.minimarket.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private AdminService adminService;

    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        Set<Role> buyerRoles = new HashSet<>(Set.of(new Role(1L, RoleName.BUYER)));

        activeUser = User.builder()
                .id(1L).fullName("Active User").email("active@test.com")
                .password("enc").enabled(true).roles(buyerRoles).build();

        inactiveUser = User.builder()
                .id(2L).fullName("Inactive User").email("inactive@test.com")
                .password("enc").enabled(false).roles(buyerRoles).build();
    }

    // Test 11: getAllUsers returns mapped list of UserResponse
    @Test
    void getAllUsers_returnsMappedUserResponseList() {
        when(userRepository.findAll()).thenReturn(List.of(activeUser, inactiveUser));

        List<UserResponse> result = adminService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmail()).isEqualTo("active@test.com");
        assertThat(result.get(0).getRoles()).contains("BUYER");
        assertThat(result.get(1).isEnabled()).isFalse();
    }

    // Test 12: activateUser sets enabled = true and saves
    @Test
    void activateUser_setsEnabledTrueAndSaves() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(inactiveUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse result = adminService.activateUser(2L);

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

        UserResponse result = adminService.deactivateUser(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isEnabled()).isFalse();
        assertThat(result.isEnabled()).isFalse();
    }

    // Test 14: activateUser throws when user not found
    @Test
    void activateUser_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.activateUser(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
        verify(userRepository, never()).save(any());
    }

    // Test 15: deactivateUser throws when user not found
    @Test
    void deactivateUser_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(888L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deactivateUser(888L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("888");
        verify(userRepository, never()).save(any());
    }

    // Test 16: getAllUsers with empty repository returns empty list
    @Test
    void getAllUsers_emptyRepository_returnsEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserResponse> result = adminService.getAllUsers();

        assertThat(result).isEmpty();
        verify(userRepository).findAll();
    }
}
