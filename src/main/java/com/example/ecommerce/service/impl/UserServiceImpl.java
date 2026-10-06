package com.example.ecommerce.service.impl;

import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.mapper.UserMapper;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.service.UserService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
