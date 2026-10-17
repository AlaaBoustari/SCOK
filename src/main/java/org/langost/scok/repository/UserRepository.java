package org.langost.scok.repository;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.langost.scok.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    public Optional<User> findByCognitoSub(String sub);
    public Optional<User> findByEmail(String email);

    boolean existsByEmail(@Email @NotBlank String email);
}
