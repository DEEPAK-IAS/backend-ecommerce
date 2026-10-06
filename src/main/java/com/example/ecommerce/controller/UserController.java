package com.example.ecommerce.controller;

import com.example.ecommerce.config.OpenApiConfig;
import com.example.ecommerce.constant.AppConstants;
import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.security.authentication.AuthenticatedUser;
import com.example.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_V1 + "/users")
@Tag(name = "Users")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Read-only for now (also the easiest way to check that your JWT works). PUT/PATCH come with the profile feature. */
    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success("Profile fetched successfully", userService.getById(principal.getId()));
    }
}
