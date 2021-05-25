import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// You shall not use this annotation in your solutions.
// It's there for CI purposes.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Replaces {
    public Class<?> what();
}
