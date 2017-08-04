

package java.awt.datatransfer;

import java.util.Map;



public interface FlavorMap {


    Map<DataFlavor,String> getNativesForFlavors(DataFlavor[] flavors);


    Map<String,DataFlavor> getFlavorsForNatives(String[] natives);
}
