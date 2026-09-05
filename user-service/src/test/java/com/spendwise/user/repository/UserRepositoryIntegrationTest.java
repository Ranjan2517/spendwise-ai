package com.spendwise.user.repository;

import com.spendwise.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class UserRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("userdb_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndRetrievesUser_byEmail() {
        User user = new User("integration@example.com", "hashed-password");
        userRepository.save(user);

        var found = userRepository.findByEmail("integration@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("integration@example.com");
    }

    @Test
    void existsByEmail_returnsTrue_whenUserExists() {
        User user = new User("exists@example.com", "hashed-password");
        userRepository.save(user);

        boolean exists = userRepository.existsByEmail("exists@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_returnsFalse_whenUserDoesNotExist() {
        boolean exists = userRepository.existsByEmail("nobody@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    void enforcesUniqueEmailConstraint() {
        userRepository.save(new User("duplicate@example.com", "hash-one"));

        org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> {
                    userRepository.saveAndFlush(new User("duplicate@example.com", "hash-two"));
                }
        );
    }
}