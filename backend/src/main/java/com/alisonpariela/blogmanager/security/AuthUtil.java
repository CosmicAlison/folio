package com.alisonpariela.blogmanager.security;

import com.alisonpariela.blogmanager.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtil {

    private AuthUtil() {}

    public static Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof User) {
            return ((User) principal).getId();
        }

        throw new RuntimeException("Principal is not an instance of User");
    }
}
