package com.example.ecommerce.controller;

import com.example.ecommerce.constant.AppConstants;
import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.PingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tiny public endpoint to verify the API envelope end to end. Health checks use /actuator/health. */
@RestController
@RequestMapping(AppConstants.API_V1 + "/system")
@Tag(name = "System")
public class SystemController {

    @GetMapping("/ping")
    @Operation(summary = "Verify the API is reachable and see the standard response format")
    public ApiResponse<PingResponse> ping() {
        return ApiResponse.success("pong", new PingResponse("UP", Instant.now()));
    }
}
