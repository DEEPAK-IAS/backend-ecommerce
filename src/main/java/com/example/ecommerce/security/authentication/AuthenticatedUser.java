package com.example.ecommerce.security.authentication;

import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** The principal placed in the SecurityContext. Never carries the password hash. */
public final class AuthenticatedUser implements UserDetails {

    private final UUID id;
    private final String email;
    private final Role role;

    private AuthenticatedUser(UUID id, String email, Role role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole());
    }

    public UUID getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
