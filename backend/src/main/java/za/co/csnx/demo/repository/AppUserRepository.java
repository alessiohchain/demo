package za.co.csnx.demo.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.csnx.demo.domain.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, AppUser.Pk> {

    Optional<AppUser> findByCompanyCodeAndUsername(String companyCode, String username);
}
