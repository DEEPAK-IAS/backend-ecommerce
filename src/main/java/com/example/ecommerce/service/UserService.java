package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.UserResponse;
import java.util.UUID;

public interface UserService {

    UserResponse getById(UUID id);
}
