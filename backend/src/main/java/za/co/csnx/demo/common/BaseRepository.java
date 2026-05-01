package za.co.csnx.demo.common;

import java.io.Serializable;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseRepository<T, ID extends Serializable>
        extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    default T findByIdOrThrow(ID id) {
        return findById(id).orElseThrow(() ->
                new EntityNotFoundException(getClass().getSimpleName(), id));
    }

    default Optional<T> findByIdOptional(ID id) {
        return findById(id);
    }
}
