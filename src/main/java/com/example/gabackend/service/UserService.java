package com.example.gabackend.service;

import com.example.gabackend.model.User;
import com.example.gabackend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User createUser(String username, String password, String roles) {
        User user = new User(username, new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(password), roles);
        return userRepository.save(user);
    }

    public boolean validateUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches(password,user.getPassword());
        }
        return false;
    }
}
