


package java.time.temporal;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.DateTimeException;


public final class ValueRange implements Serializable {


    private static final long serialVersionUID = -7317881728594519368L;


    private final long minSmallest;

    private final long minLargest;

    private final long maxSmallest;

    private final long maxLargest;


    public static ValueRange of(long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException("Minimum value must be less than maximum value");
        }
        return new ValueRange(min, min, max, max);
    }


    public static ValueRange of(long min, long maxSmallest, long maxLargest) {
        return of(min, min, maxSmallest, maxLargest);
    }


    public static ValueRange of(long minSmallest, long minLargest, long maxSmallest, long maxLargest) {
        if (minSmallest > minLargest) {
            throw new IllegalArgumentException("Smallest minimum value must be less than largest minimum value");
        }
        if (maxSmallest > maxLargest) {
            throw new IllegalArgumentException("Smallest maximum value must be less than largest maximum value");
        }
        if (minLargest > maxLargest) {
            throw new IllegalArgumentException("Minimum value must be less than maximum value");
        }
        return new ValueRange(minSmallest, minLargest, maxSmallest, maxLargest);
    }


    private ValueRange(long minSmallest, long minLargest, long maxSmallest, long maxLargest) {
        this.minSmallest = minSmallest;
        this.minLargest = minLargest;
        this.maxSmallest = maxSmallest;
        this.maxLargest = maxLargest;
    }

    //-----------------------------------------------------------------------

    public boolean isFixed() {
        return minSmallest == minLargest && maxSmallest == maxLargest;
    }

    //-----------------------------------------------------------------------

    public long getMinimum() {
        return minSmallest;
    }


    public long getLargestMinimum() {
        return minLargest;
    }


    public long getSmallestMaximum() {
        return maxSmallest;
    }


    public long getMaximum() {
        return maxLargest;
    }

    //-----------------------------------------------------------------------

    public boolean isIntValue() {
        return getMinimum() >= Integer.MIN_VALUE && getMaximum() <= Integer.MAX_VALUE;
    }


    public boolean isValidValue(long value) {
        return (value >= getMinimum() && value <= getMaximum());
    }


    public boolean isValidIntValue(long value) {
        return isIntValue() && isValidValue(value);
    }


    public long checkValidValue(long value, TemporalField field) {
        if (isValidValue(value) == false) {
            throw new DateTimeException(genInvalidFieldMessage(field, value));
        }
        return value;
    }


    public int checkValidIntValue(long value, TemporalField field) {
        if (isValidIntValue(value) == false) {
            throw new DateTimeException(genInvalidFieldMessage(field, value));
        }
        return (int) value;
    }

    private String genInvalidFieldMessage(TemporalField field, long value) {
        if (field != null) {
            return "Invalid value for " + field + " (valid values " + this + "): " + value;
        } else {
            return "Invalid value (valid values " + this + "): " + value;
        }
    }

    //-----------------------------------------------------------------------

    private void readObject(ObjectInputStream s)
         throws IOException, ClassNotFoundException, InvalidObjectException
    {
        s.defaultReadObject();
        if (minSmallest > minLargest) {
            throw new InvalidObjectException("Smallest minimum value must be less than largest minimum value");
        }
        if (maxSmallest > maxLargest) {
            throw new InvalidObjectException("Smallest maximum value must be less than largest maximum value");
        }
        if (minLargest > maxLargest) {
            throw new InvalidObjectException("Minimum value must be less than maximum value");
        }
    }

    //-----------------------------------------------------------------------

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ValueRange) {
            ValueRange other = (ValueRange) obj;
           return minSmallest == other.minSmallest && minLargest == other.minLargest &&
                   maxSmallest == other.maxSmallest && maxLargest == other.maxLargest;
        }
        return false;
    }


    @Override
    public int hashCode() {
        long hash = minSmallest + minLargest << 16 + minLargest >> 48 + maxSmallest << 32 +
            maxSmallest >> 32 + maxLargest << 48 + maxLargest >> 16;
        return (int) (hash ^ (hash >>> 32));
    }

    //-----------------------------------------------------------------------

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(minSmallest);
        if (minSmallest != minLargest) {
            buf.append('/').append(minLargest);
        }
        buf.append(" - ").append(maxSmallest);
        if (maxSmallest != maxLargest) {
            buf.append('/').append(maxLargest);
        }
        return buf.toString();
    }

}
