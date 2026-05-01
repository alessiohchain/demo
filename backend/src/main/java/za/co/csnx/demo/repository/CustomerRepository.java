package za.co.csnx.demo.repository;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import za.co.csnx.demo.common.BaseRepository;
import za.co.csnx.demo.domain.Customer;

@Repository
public interface CustomerRepository extends BaseRepository<Customer, Long> {

    Optional<Customer> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
