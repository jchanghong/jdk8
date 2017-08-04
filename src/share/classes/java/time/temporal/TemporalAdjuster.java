package java.time.temporal;
import java.time.DateTimeException;
@FunctionalInterface
public interface TemporalAdjuster {
    Temporal adjustInto(Temporal temporal);
}
