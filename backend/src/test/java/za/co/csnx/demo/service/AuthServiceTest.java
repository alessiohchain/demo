package za.co.csnx.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.co.csnx.demo.domain.Customer;
import za.co.csnx.demo.repository.CustomerRepository;
import za.co.csnx.demo.security.JwtService;
import za.co.csnx.demo.web.dto.RegisterRequest;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock CustomerRepository customerRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;

    @InjectMocks AuthService authService;

    @Test
    void registerHashesPasswordAndPersists() {
        when(customerRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("hunter2!!")).thenReturn("BCRYPT_HASH");
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer saved = authService.register(
                new RegisterRequest("alice@example.com", "hunter2!!", "Alice"));

        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("BCRYPT_HASH");
        assertThat(saved.getDisplayName()).isEqualTo("Alice");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(customerRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("alice@example.com", "hunter2!!", "Alice")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void registerNormalizesEmailToLowercase() {
        when(customerRepository.existsByEmailIgnoreCase("Alice@Example.COM")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer saved = authService.register(
                new RegisterRequest("Alice@Example.COM", "hunter2!!", "Alice"));

        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
    }
}
