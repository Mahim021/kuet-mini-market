package com.kuet.minimarket.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private boolean enabled;
    private List<String> roles;
    private Instant createdAt;
}
