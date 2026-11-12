package com.offerflow.api.user;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
public record CurrentUser(User user) implements UserDetails {
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return java.util.List.of(); }
    @Override public String getPassword() { return user.getPasswordHash(); }
    @Override public String getUsername() { return user.getUsername(); }
}
