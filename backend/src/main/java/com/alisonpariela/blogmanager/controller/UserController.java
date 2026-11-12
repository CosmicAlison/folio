package com.alisonpariela.blogmanager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestBody;

import com.alisonpariela.blogmanager.DTO.UserDTO;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PutMapping;

import com.alisonpariela.blogmanager.service.UserService;

import com.alisonpariela.blogmanager.DTO.UpdateUserRequest;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(
            userService.updateUser(id, request.getUsername(), request.getEmail())
        );
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
