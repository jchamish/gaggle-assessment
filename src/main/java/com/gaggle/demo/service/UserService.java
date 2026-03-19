package com.gaggle.demo.service;

import com.gaggle.demo.entity.User;
import com.gaggle.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Page<User> listAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User create(String name, String email, Long schoolIdentifier) {
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use: " + email);
        }
        return userRepository.save(
            User.builder().name(name).email(email).schoolIdentifier(schoolIdentifier).build()
        );
    }

    public User update(Long id, String name, String email, Long schoolIdentifier) {
        User user = getById(id);
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use: " + email);
        }
        user.setName(name);
        user.setEmail(email);
        user.setSchoolIdentifier(schoolIdentifier);
        return userRepository.save(user);
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        userRepository.deleteById(id);
    }
}
