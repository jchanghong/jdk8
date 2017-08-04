package java.time.temporal;
import java.time.DateTimeException;
@FunctionalInterface
public interface TemporalQuery<R> {
    R queryFrom(TemporalAccessor temporal);
}
