package com.zakat;

import com.zakat.entity.User;
import com.zakat.enums.UserRole;
import com.zakat.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@SpringBootApplication
public class ZakatApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZakatApplication.class, args);
    }

    @Bean
    public CommandLineRunner demo(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return (args) -> {
            if (userRepository.count() > 0) {
                return;
            }

            userRepository.save(new User(UUID.randomUUID(), "admin", "admin@yopmail.com", passwordEncoder.encode("admin"), UserRole.ADMIN, true));
            userRepository.save(new User(UUID.randomUUID(), "operator", "operator@yopmail.com", passwordEncoder.encode("operator"), UserRole.OPERATOR, true));
            userRepository.save(new User(UUID.randomUUID(), "viewer", "viewer@yopmail.com", passwordEncoder.encode("viewer"), UserRole.VIEWER, true));
        };
    }
}
