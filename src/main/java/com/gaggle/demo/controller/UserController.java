package com.gaggle.demo.controller;

import com.gaggle.demo.dto.PageResponse;
import com.gaggle.demo.entity.User;
import com.gaggle.demo.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public PageResponse<User> listUsers(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PageResponse.of(userService.listAll(pageable));
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@Valid @RequestBody UserRequest req) {
        return userService.create(req.name(), req.email(), req.schoolIdentifier());
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest req) {
        return userService.update(id, req.name(), req.email(), req.schoolIdentifier());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    record UserRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotNull Long schoolIdentifier
    ) {}
}
