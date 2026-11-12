package com.offerflow.api.user;

import com.offerflow.api.common.NotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository users;
    public UserService(UserRepository users) { this.users = users; }
    @Override public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return new CurrentUser(users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password")));
    }
    public User requireUser(Long id) { return users.findById(id).orElseThrow(() -> new NotFoundException("User not found")); }
}
