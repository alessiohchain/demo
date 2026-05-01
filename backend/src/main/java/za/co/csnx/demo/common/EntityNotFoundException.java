package za.co.csnx.demo.common;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String entity, Object id) {
        super("%s with id %s not found".formatted(entity, id));
    }

    public EntityNotFoundException(String message) {
        super(message);
    }
}
