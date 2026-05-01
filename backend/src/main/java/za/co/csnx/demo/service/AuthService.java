package za.co.csnx.demo.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.csnx.demo.domain.Customer;
import za.co.csnx.demo.repository.CustomerRepository;
import za.co.csnx.demo.security.JwtService;
import za.co.csnx.demo.web.dto.LoginRequest;
import za.co.csnx.demo.web.dto.RegisterRequest;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(CustomerRepository customerRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public Customer register(RegisterRequest request) {
        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        Customer customer = new Customer();
        customer.setEmail(request.email().toLowerCase());
        customer.setPasswordHash(passwordEncoder.encode(request.password()));
        customer.setDisplayName(request.displayName());
        return customerRepository.save(customer);
    }

    public Tokens login(LoginRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
        return issueTokens(auth.getName());
    }

    public Tokens refresh(String subject) {
        if (!customerRepository.existsByEmailIgnoreCase(subject)) {
            throw new UsernameNotFoundException("Unknown subject");
        }
        return issueTokens(subject);
    }

    private Tokens issueTokens(String subject) {
        return new Tokens(
                jwtService.issueAccess(subject),
                jwtService.issueRefresh(subject),
                jwtService.accessTokenTtlSeconds(),
                jwtService.refreshTokenTtlSeconds());
    }

    public record Tokens(String accessToken, String refreshToken,
                         long accessTtlSeconds, long refreshTtlSeconds) {
    }
}
