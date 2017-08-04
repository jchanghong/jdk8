package java.awt.datatransfer;
import java.util.List;
public interface FlavorTable extends FlavorMap {
    List<String> getNativesForFlavor(DataFlavor flav);
    List<DataFlavor> getFlavorsForNative(String nat);
}
