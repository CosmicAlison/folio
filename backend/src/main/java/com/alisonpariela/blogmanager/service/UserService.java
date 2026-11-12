package com.alisonpariela.blogmanager.service;

import org.springframework.stereotype.Service;

import com.alisonpariela.blogmanager.DTO.UserDTO;
import com.alisonpariela.blogmanager.model.User;
import com.alisonpariela.blogmanager.repository.UserRepository;
import com.alisonpariela.blogmanager.security.AuthUtil;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@AllArgsConstructor
@Service
public class UserService{
    private final UserRepository userRepository;

    @Transactional
    public UserDTO updateUser(Long userId, String username, String email) {
        Long authId = AuthUtil.getAuthenticatedUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!user.getId().equals(authId)){
            throw new AccessDeniedException("You are not permitted to update this user");
        }

        if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already in use by another user");
        }
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use by another user");
        }

        user.setUsername(username);
        user.setEmail(email);

        User saved = userRepository.save(user);
        return new UserDTO(saved.getId(), saved.getUsername(), saved.getEmail());
    }

    @Transactional
    public void deleteUser(Long userId){
        Long authId = AuthUtil.getAuthenticatedUserId();

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found."));

        if (!user.getId().equals(authId)){
            throw new AccessDeniedException("You are not permitted to delete this user");
        }

        userRepository.delete(user);
    }
}
