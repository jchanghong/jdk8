package java.time.temporal;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Period;
import java.util.List;
public interface TemporalAmount {
    long get(TemporalUnit unit);
    List<TemporalUnit> getUnits();
    Temporal addTo(Temporal temporal);
    Temporal subtractFrom(Temporal temporal);
}
