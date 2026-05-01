package za.co.csnx.demo.security;

import java.util.List;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import za.co.csnx.demo.repository.CustomerRepository;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    public AppUserDetailsService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return customerRepository.findByEmailIgnoreCase(email)
                .map(c -> User.withUsername(c.getEmail())
                        .password(c.getPasswordHash())
                        .authorities(List.of())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No user: " + email));
    }
}
