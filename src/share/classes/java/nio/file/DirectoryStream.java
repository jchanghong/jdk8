

package java.nio.file;

import java.util.Iterator;
import java.io.Closeable;
import java.io.IOException;



public interface DirectoryStream<T>
    extends Closeable, Iterable<T> {

    @FunctionalInterface
    public static interface Filter<T> {

        boolean accept(T entry) throws IOException;
    }


    @Override
    Iterator<T> iterator();
}
