

package java.awt.datatransfer;

import java.awt.Toolkit;

import java.lang.ref.SoftReference;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.URL;
import java.net.MalformedURLException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import sun.awt.AppContext;
import sun.awt.datatransfer.DataTransferer;


public final class SystemFlavorMap implements FlavorMap, FlavorTable {


    private static String JavaMIME = "JAVA_DATAFLAVOR:";

    private static final Object FLAVOR_MAP_KEY = new Object();


    private static final String keyValueSeparators = "=: \t\r\n\f";
    private static final String strictKeyValueSeparators = "=:";
    private static final String whiteSpaceChars = " \t\r\n\f";


    private static final String[] UNICODE_TEXT_CLASSES = {
        "java.io.Reader", "java.lang.String", "java.nio.CharBuffer", "\"[C\""
    };


    private static final String[] ENCODED_TEXT_CLASSES = {
        "java.io.InputStream", "java.nio.ByteBuffer", "\"[B\""
    };


    private static final String TEXT_PLAIN_BASE_TYPE = "text/plain";


    private static final String HTML_TEXT_BASE_TYPE = "text/html";


    private final Map<String, LinkedHashSet<DataFlavor>> nativeToFlavor = new HashMap<>();


    private Map<String, LinkedHashSet<DataFlavor>> getNativeToFlavor() {
        if (!isMapInitialized) {
            initSystemFlavorMap();
        }
        return nativeToFlavor;
    }


    private final Map<DataFlavor, LinkedHashSet<String>> flavorToNative = new HashMap<>();


    private synchronized Map<DataFlavor, LinkedHashSet<String>> getFlavorToNative() {
        if (!isMapInitialized) {
            initSystemFlavorMap();
        }
        return flavorToNative;
    }


    private Map<String, LinkedHashSet<String>> textTypeToNative = new HashMap<>();


    private boolean isMapInitialized = false;


    private synchronized Map<String, LinkedHashSet<String>> getTextTypeToNative() {
        if (!isMapInitialized) {
            initSystemFlavorMap();
            // From this point the map should not be modified
            textTypeToNative = Collections.unmodifiableMap(textTypeToNative);
        }
        return textTypeToNative;
    }


    private final SoftCache<DataFlavor, String> nativesForFlavorCache = new SoftCache<>();


    private final SoftCache<String, DataFlavor> flavorsForNativeCache = new SoftCache<>();


    private Set<Object> disabledMappingGenerationKeys = new HashSet<>();


    public static FlavorMap getDefaultFlavorMap() {
        AppContext context = AppContext.getAppContext();
        FlavorMap fm = (FlavorMap) context.get(FLAVOR_MAP_KEY);
        if (fm == null) {
            fm = new SystemFlavorMap();
            context.put(FLAVOR_MAP_KEY, fm);
        }
        return fm;
    }

    private SystemFlavorMap() {
    }


    private void initSystemFlavorMap() {
        if (isMapInitialized) {
            return;
        }

        isMapInitialized = true;
        BufferedReader flavormapDotProperties =
            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<BufferedReader>() {
                    public BufferedReader run() {
                        String fileName =
                            System.getProperty("java.home") +
                            File.separator +
                            "lib" +
                            File.separator +
                            "flavormap.properties";
                        try {
                            return new BufferedReader
                                (new InputStreamReader
                                    (new File(fileName).toURI().toURL().openStream(), "ISO-8859-1"));
                        } catch (MalformedURLException e) {
                            System.err.println("MalformedURLException:" + e + " while loading default flavormap.properties file:" + fileName);
                        } catch (IOException e) {
                            System.err.println("IOException:" + e + " while loading default flavormap.properties file:" + fileName);
                        }
                        return null;
                    }
                });

        String url =
            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<String>() {
                    public String run() {
                        return Toolkit.getProperty("AWT.DnD.flavorMapFileURL", null);
                    }
                });

        if (flavormapDotProperties != null) {
            try {
                parseAndStoreReader(flavormapDotProperties);
            } catch (IOException e) {
                System.err.println("IOException:" + e + " while parsing default flavormap.properties file");
            }
        }

        BufferedReader flavormapURL = null;
        if (url != null) {
            try {
                flavormapURL = new BufferedReader(new InputStreamReader(new URL(url).openStream(), "ISO-8859-1"));
            } catch (MalformedURLException e) {
                System.err.println("MalformedURLException:" + e + " while reading AWT.DnD.flavorMapFileURL:" + url);
            } catch (IOException e) {
                System.err.println("IOException:" + e + " while reading AWT.DnD.flavorMapFileURL:" + url);
            } catch (SecurityException e) {
                // ignored
            }
        }

        if (flavormapURL != null) {
            try {
                parseAndStoreReader(flavormapURL);
            } catch (IOException e) {
                System.err.println("IOException:" + e + " while parsing AWT.DnD.flavorMapFileURL");
            }
        }
    }

    private void parseAndStoreReader(BufferedReader in) throws IOException {
        while (true) {
            // Get next line
            String line = in.readLine();
            if (line == null) {
                return;
            }

            if (line.length() > 0) {
                // Continue lines that end in slashes if they are not comments
                char firstChar = line.charAt(0);
                if (firstChar != '#' && firstChar != '!') {
                    while (continueLine(line)) {
                        String nextLine = in.readLine();
                        if (nextLine == null) {
                            nextLine = "";
                        }
                        String loppedLine =
                            line.substring(0, line.length() - 1);
                        // Advance beyond whitespace on new line
                        int startIndex = 0;
                        for(; startIndex < nextLine.length(); startIndex++) {
                            if (whiteSpaceChars.
                                    indexOf(nextLine.charAt(startIndex)) == -1)
                            {
                                break;
                            }
                        }
                        nextLine = nextLine.substring(startIndex,
                                                      nextLine.length());
                        line = loppedLine+nextLine;
                    }

                    // Find start of key
                    int len = line.length();
                    int keyStart = 0;
                    for(; keyStart < len; keyStart++) {
                        if(whiteSpaceChars.
                               indexOf(line.charAt(keyStart)) == -1) {
                            break;
                        }
                    }

                    // Blank lines are ignored
                    if (keyStart == len) {
                        continue;
                    }

                    // Find separation between key and value
                    int separatorIndex = keyStart;
                    for(; separatorIndex < len; separatorIndex++) {
                        char currentChar = line.charAt(separatorIndex);
                        if (currentChar == '\\') {
                            separatorIndex++;
                        } else if (keyValueSeparators.
                                       indexOf(currentChar) != -1) {
                            break;
                        }
                    }

                    // Skip over whitespace after key if any
                    int valueIndex = separatorIndex;
                    for (; valueIndex < len; valueIndex++) {
                        if (whiteSpaceChars.
                                indexOf(line.charAt(valueIndex)) == -1) {
                            break;
                        }
                    }

                    // Skip over one non whitespace key value separators if any
                    if (valueIndex < len) {
                        if (strictKeyValueSeparators.
                                indexOf(line.charAt(valueIndex)) != -1) {
                            valueIndex++;
                        }
                    }

                    // Skip over white space after other separators if any
                    while (valueIndex < len) {
                        if (whiteSpaceChars.
                                indexOf(line.charAt(valueIndex)) == -1) {
                            break;
                        }
                        valueIndex++;
                    }

                    String key = line.substring(keyStart, separatorIndex);
                    String value = (separatorIndex < len)
                        ? line.substring(valueIndex, len)
                        : "";

                    // Convert then store key and value
                    key = loadConvert(key);
                    value = loadConvert(value);

                    try {
                        MimeType mime = new MimeType(value);
                        if ("text".equals(mime.getPrimaryType())) {
                            String charset = mime.getParameter("charset");
                            if (DataTransferer.doesSubtypeSupportCharset
                                    (mime.getSubType(), charset))
                            {
                                // We need to store the charset and eoln
                                // parameters, if any, so that the
                                // DataTransferer will have this information
                                // for conversion into the native format.
                                DataTransferer transferer =
                                    DataTransferer.getInstance();
                                if (transferer != null) {
                                    transferer.registerTextFlavorProperties
                                        (key, charset,
                                         mime.getParameter("eoln"),
                                         mime.getParameter("terminators"));
                                }
                            }

                            // But don't store any of these parameters in the
                            // DataFlavor itself for any text natives (even
                            // non-charset ones). The SystemFlavorMap will
                            // synthesize the appropriate mappings later.
                            mime.removeParameter("charset");
                            mime.removeParameter("class");
                            mime.removeParameter("eoln");
                            mime.removeParameter("terminators");
                            value = mime.toString();
                        }
                    } catch (MimeTypeParseException e) {
                        e.printStackTrace();
                        continue;
                    }

                    DataFlavor flavor;
                    try {
                        flavor = new DataFlavor(value);
                    } catch (Exception e) {
                        try {
                            flavor = new DataFlavor(value, null);
                        } catch (Exception ee) {
                            ee.printStackTrace();
                            continue;
                        }
                    }

                    final LinkedHashSet<DataFlavor> dfs = new LinkedHashSet<>();
                    dfs.add(flavor);

                    if ("text".equals(flavor.getPrimaryType())) {
                        dfs.addAll(convertMimeTypeToDataFlavors(value));
                        store(flavor.mimeType.getBaseType(), key, getTextTypeToNative());
                    }

                    for (DataFlavor df : dfs) {
                        store(df, key, getFlavorToNative());
                        store(key, df, getNativeToFlavor());
                    }
                }
            }
        }
    }


    private boolean continueLine (String line) {
        int slashCount = 0;
        int index = line.length() - 1;
        while((index >= 0) && (line.charAt(index--) == '\\')) {
            slashCount++;
        }
        return (slashCount % 2 == 1);
    }


    private String loadConvert(String theString) {
        char aChar;
        int len = theString.length();
        StringBuilder outBuffer = new StringBuilder(len);

        for (int x = 0; x < len; ) {
            aChar = theString.charAt(x++);
            if (aChar == '\\') {
                aChar = theString.charAt(x++);
                if (aChar == 'u') {
                    // Read the xxxx
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        aChar = theString.charAt(x++);
                        switch (aChar) {
                          case '0': case '1': case '2': case '3': case '4':
                          case '5': case '6': case '7': case '8': case '9': {
                             value = (value << 4) + aChar - '0';
                             break;
                          }
                          case 'a': case 'b': case 'c':
                          case 'd': case 'e': case 'f': {
                             value = (value << 4) + 10 + aChar - 'a';
                             break;
                          }
                          case 'A': case 'B': case 'C':
                          case 'D': case 'E': case 'F': {
                             value = (value << 4) + 10 + aChar - 'A';
                             break;
                          }
                          default: {
                              throw new IllegalArgumentException(
                                           "Malformed \\uxxxx encoding.");
                          }
                        }
                    }
                    outBuffer.append((char)value);
                } else {
                    if (aChar == 't') {
                        aChar = '\t';
                    } else if (aChar == 'r') {
                        aChar = '\r';
                    } else if (aChar == 'n') {
                        aChar = '\n';
                    } else if (aChar == 'f') {
                        aChar = '\f';
                    }
                    outBuffer.append(aChar);
                }
            } else {
                outBuffer.append(aChar);
            }
        }
        return outBuffer.toString();
    }


    private <H, L> void store(H hashed, L listed, Map<H, LinkedHashSet<L>> map) {
        LinkedHashSet<L> list = map.get(hashed);
        if (list == null) {
            list = new LinkedHashSet<>(1);
            map.put(hashed, list);
        }
        if (!list.contains(listed)) {
            list.add(listed);
        }
    }


    private LinkedHashSet<DataFlavor> nativeToFlavorLookup(String nat) {
        LinkedHashSet<DataFlavor> flavors = getNativeToFlavor().get(nat);


        if (nat != null && !disabledMappingGenerationKeys.contains(nat)) {
            DataTransferer transferer = DataTransferer.getInstance();
            if (transferer != null) {
                LinkedHashSet<DataFlavor> platformFlavors =
                    transferer.getPlatformMappingsForNative(nat);
                if (!platformFlavors.isEmpty()) {
                    if (flavors != null) {
                        // Prepending the platform-specific mappings ensures
                        // that the flavors added with
                        // addFlavorForUnencodedNative() are at the end of
                        // list.
                        platformFlavors.addAll(flavors);
                    }
                    flavors = platformFlavors;
                }
            }
        }

        if (flavors == null && isJavaMIMEType(nat)) {
            String decoded = decodeJavaMIMEType(nat);
            DataFlavor flavor = null;

            try {
                flavor = new DataFlavor(decoded);
            } catch (Exception e) {
                System.err.println("Exception \"" + e.getClass().getName() +
                                   ": " + e.getMessage()  +
                                   "\"while constructing DataFlavor for: " +
                                   decoded);
            }

            if (flavor != null) {
                flavors = new LinkedHashSet<>(1);
                getNativeToFlavor().put(nat, flavors);
                flavors.add(flavor);
                flavorsForNativeCache.remove(nat);

                LinkedHashSet<String> natives = getFlavorToNative().get(flavor);
                if (natives == null) {
                    natives = new LinkedHashSet<>(1);
                    getFlavorToNative().put(flavor, natives);
                }
                natives.add(nat);
                nativesForFlavorCache.remove(flavor);
            }
        }

        return (flavors != null) ? flavors : new LinkedHashSet<>(0);
    }


    private LinkedHashSet<String> flavorToNativeLookup(final DataFlavor flav,
                                                       final boolean synthesize) {

        LinkedHashSet<String> natives = getFlavorToNative().get(flav);

        if (flav != null && !disabledMappingGenerationKeys.contains(flav)) {
            DataTransferer transferer = DataTransferer.getInstance();
            if (transferer != null) {
                LinkedHashSet<String> platformNatives =
                    transferer.getPlatformMappingsForFlavor(flav);
                if (!platformNatives.isEmpty()) {
                    if (natives != null) {
                        // Prepend the platform-specific mappings to ensure
                        // that the natives added with
                        // addUnencodedNativeForFlavor() are at the end of
                        // list.
                        platformNatives.addAll(natives);
                    }
                    natives = platformNatives;
                }
            }
        }

        if (natives == null) {
            if (synthesize) {
                String encoded = encodeDataFlavor(flav);
                natives = new LinkedHashSet<>(1);
                getFlavorToNative().put(flav, natives);
                natives.add(encoded);

                LinkedHashSet<DataFlavor> flavors = getNativeToFlavor().get(encoded);
                if (flavors == null) {
                    flavors = new LinkedHashSet<>(1);
                    getNativeToFlavor().put(encoded, flavors);
                }
                flavors.add(flav);

                nativesForFlavorCache.remove(flav);
                flavorsForNativeCache.remove(encoded);
            } else {
                natives = new LinkedHashSet<>(0);
            }
        }

        return new LinkedHashSet<>(natives);
    }


    @Override
    public synchronized List<String> getNativesForFlavor(DataFlavor flav) {
        LinkedHashSet<String> retval = nativesForFlavorCache.check(flav);
        if (retval != null) {
            return new ArrayList<>(retval);
        }

        if (flav == null) {
            retval = new LinkedHashSet<>(getNativeToFlavor().keySet());
        } else if (disabledMappingGenerationKeys.contains(flav)) {
            // In this case we shouldn't synthesize a native for this flavor,
            // since its mappings were explicitly specified.
            retval = flavorToNativeLookup(flav, false);
        } else if (DataTransferer.isFlavorCharsetTextType(flav)) {
            retval = new LinkedHashSet<>(0);

            // For text/* flavors, flavor-to-native mappings specified in
            // flavormap.properties are stored per flavor's base type.
            if ("text".equals(flav.getPrimaryType())) {
                LinkedHashSet<String> textTypeNatives =
                        getTextTypeToNative().get(flav.mimeType.getBaseType());
                if (textTypeNatives != null) {
                    retval.addAll(textTypeNatives);
                }
            }

            // Also include text/plain natives, but don't duplicate Strings
            LinkedHashSet<String> textTypeNatives =
                    getTextTypeToNative().get(TEXT_PLAIN_BASE_TYPE);
            if (textTypeNatives != null) {
                retval.addAll(textTypeNatives);
            }

            if (retval.isEmpty()) {
                retval = flavorToNativeLookup(flav, true);
            } else {
                // In this branch it is guaranteed that natives explicitly
                // listed for flav's MIME type were added with
                // addUnencodedNativeForFlavor(), so they have lower priority.
                retval.addAll(flavorToNativeLookup(flav, false));
            }
        } else if (DataTransferer.isFlavorNoncharsetTextType(flav)) {
            retval = getTextTypeToNative().get(flav.mimeType.getBaseType());

            if (retval == null || retval.isEmpty()) {
                retval = flavorToNativeLookup(flav, true);
            } else {
                // In this branch it is guaranteed that natives explicitly
                // listed for flav's MIME type were added with
                // addUnencodedNativeForFlavor(), so they have lower priority.
                retval.addAll(flavorToNativeLookup(flav, false));
            }
        } else {
            retval = flavorToNativeLookup(flav, true);
        }

        nativesForFlavorCache.put(flav, retval);
        // Create a copy, because client code can modify the returned list.
        return new ArrayList<>(retval);
    }


    @Override
    public synchronized List<DataFlavor> getFlavorsForNative(String nat) {
        LinkedHashSet<DataFlavor> returnValue = flavorsForNativeCache.check(nat);
        if (returnValue != null) {
            return new ArrayList<>(returnValue);
        } else {
            returnValue = new LinkedHashSet<>();
        }

        if (nat == null) {
            for (String n : getNativesForFlavor(null)) {
                returnValue.addAll(getFlavorsForNative(n));
            }
        } else {
            final LinkedHashSet<DataFlavor> flavors = nativeToFlavorLookup(nat);
            if (disabledMappingGenerationKeys.contains(nat)) {
                return new ArrayList<>(flavors);
            }

            final LinkedHashSet<DataFlavor> flavorsWithSynthesized =
                    nativeToFlavorLookup(nat);

            for (DataFlavor df : flavorsWithSynthesized) {
                returnValue.add(df);
                if ("text".equals(df.getPrimaryType())) {
                    String baseType = df.mimeType.getBaseType();
                    returnValue.addAll(convertMimeTypeToDataFlavors(baseType));
                }
            }
        }
        flavorsForNativeCache.put(nat, returnValue);
        return new ArrayList<>(returnValue);
    }

    private static Set<DataFlavor> convertMimeTypeToDataFlavors(
        final String baseType) {

        final Set<DataFlavor> returnValue = new LinkedHashSet<>();

        String subType = null;

        try {
            final MimeType mimeType = new MimeType(baseType);
            subType = mimeType.getSubType();
        } catch (MimeTypeParseException mtpe) {
            // Cannot happen, since we checked all mappings
            // on load from flavormap.properties.
        }

        if (DataTransferer.doesSubtypeSupportCharset(subType, null)) {
            if (TEXT_PLAIN_BASE_TYPE.equals(baseType))
            {
                returnValue.add(DataFlavor.stringFlavor);
            }

            for (String unicodeClassName : UNICODE_TEXT_CLASSES) {
                final String mimeType = baseType + ";charset=Unicode;class=" +
                                            unicodeClassName;

                final LinkedHashSet<String> mimeTypes =
                    handleHtmlMimeTypes(baseType, mimeType);
                for (String mt : mimeTypes) {
                    DataFlavor toAdd = null;
                    try {
                        toAdd = new DataFlavor(mt);
                    } catch (ClassNotFoundException cannotHappen) {
                    }
                    returnValue.add(toAdd);
                }
            }

            for (String charset : DataTransferer.standardEncodings()) {

                for (String encodedTextClass : ENCODED_TEXT_CLASSES) {
                    final String mimeType =
                            baseType + ";charset=" + charset +
                            ";class=" + encodedTextClass;

                    final LinkedHashSet<String> mimeTypes =
                        handleHtmlMimeTypes(baseType, mimeType);

                    for (String mt : mimeTypes) {

                        DataFlavor df = null;

                        try {
                            df = new DataFlavor(mt);
                            // Check for equality to plainTextFlavor so
                            // that we can ensure that the exact charset of
                            // plainTextFlavor, not the canonical charset
                            // or another equivalent charset with a
                            // different name, is used.
                            if (df.equals(DataFlavor.plainTextFlavor)) {
                                df = DataFlavor.plainTextFlavor;
                            }
                        } catch (ClassNotFoundException cannotHappen) {
                        }

                        returnValue.add(df);
                    }
                }
            }

            if (TEXT_PLAIN_BASE_TYPE.equals(baseType))
            {
                returnValue.add(DataFlavor.plainTextFlavor);
            }
        } else {
            // Non-charset text natives should be treated as
            // opaque, 8-bit data in any of its various
            // representations.
            for (String encodedTextClassName : ENCODED_TEXT_CLASSES) {
                DataFlavor toAdd = null;
                try {
                    toAdd = new DataFlavor(baseType +
                         ";class=" + encodedTextClassName);
                } catch (ClassNotFoundException cannotHappen) {
                }
                returnValue.add(toAdd);
            }
        }
        return returnValue;
    }

    private static final String [] htmlDocumntTypes =
            new String [] {"all", "selection", "fragment"};

    private static LinkedHashSet<String> handleHtmlMimeTypes(String baseType,
                                                             String mimeType) {

        LinkedHashSet<String> returnValues = new LinkedHashSet<>();

        if (HTML_TEXT_BASE_TYPE.equals(baseType)) {
            for (String documentType : htmlDocumntTypes) {
                returnValues.add(mimeType + ";document=" + documentType);
            }
        } else {
            returnValues.add(mimeType);
        }

        return returnValues;
    }


    @Override
    public synchronized Map<DataFlavor,String> getNativesForFlavors(DataFlavor[] flavors)
    {
        // Use getNativesForFlavor to generate extra natives for text flavors
        // and stringFlavor

        if (flavors == null) {
            List<DataFlavor> flavor_list = getFlavorsForNative(null);
            flavors = new DataFlavor[flavor_list.size()];
            flavor_list.toArray(flavors);
        }

        Map<DataFlavor, String> retval = new HashMap<>(flavors.length, 1.0f);
        for (DataFlavor flavor : flavors) {
            List<String> natives = getNativesForFlavor(flavor);
            String nat = (natives.isEmpty()) ? null : natives.get(0);
            retval.put(flavor, nat);
        }

        return retval;
    }


    @Override
    public synchronized Map<String,DataFlavor> getFlavorsForNatives(String[] natives)
    {
        // Use getFlavorsForNative to generate extra flavors for text natives
        if (natives == null) {
            List<String> nativesList = getNativesForFlavor(null);
            natives = new String[nativesList.size()];
            nativesList.toArray(natives);
        }

        Map<String, DataFlavor> retval = new HashMap<>(natives.length, 1.0f);
        for (String aNative : natives) {
            List<DataFlavor> flavors = getFlavorsForNative(aNative);
            DataFlavor flav = (flavors.isEmpty())? null : flavors.get(0);
            retval.put(aNative, flav);
        }
        return retval;
    }


    public synchronized void addUnencodedNativeForFlavor(DataFlavor flav,
                                                         String nat) {
        Objects.requireNonNull(nat, "Null native not permitted");
        Objects.requireNonNull(flav, "Null flavor not permitted");

        LinkedHashSet<String> natives = getFlavorToNative().get(flav);
        if (natives == null) {
            natives = new LinkedHashSet<>(1);
            getFlavorToNative().put(flav, natives);
        }
        natives.add(nat);
        nativesForFlavorCache.remove(flav);
    }


    public synchronized void setNativesForFlavor(DataFlavor flav,
                                                 String[] natives) {
        Objects.requireNonNull(natives, "Null natives not permitted");
        Objects.requireNonNull(flav, "Null flavors not permitted");

        getFlavorToNative().remove(flav);
        for (String aNative : natives) {
            addUnencodedNativeForFlavor(flav, aNative);
        }
        disabledMappingGenerationKeys.add(flav);
        nativesForFlavorCache.remove(flav);
    }


    public synchronized void addFlavorForUnencodedNative(String nat,
                                                         DataFlavor flav) {
        Objects.requireNonNull(nat, "Null native not permitted");
        Objects.requireNonNull(flav, "Null flavor not permitted");

        LinkedHashSet<DataFlavor> flavors = getNativeToFlavor().get(nat);
        if (flavors == null) {
            flavors = new LinkedHashSet<>(1);
            getNativeToFlavor().put(nat, flavors);
        }
        flavors.add(flav);
        flavorsForNativeCache.remove(nat);
    }


    public synchronized void setFlavorsForNative(String nat,
                                                 DataFlavor[] flavors) {
        Objects.requireNonNull(nat, "Null native not permitted");
        Objects.requireNonNull(flavors, "Null flavors not permitted");

        getNativeToFlavor().remove(nat);
        for (DataFlavor flavor : flavors) {
            addFlavorForUnencodedNative(nat, flavor);
        }
        disabledMappingGenerationKeys.add(nat);
        flavorsForNativeCache.remove(nat);
    }


    public static String encodeJavaMIMEType(String mimeType) {
        return (mimeType != null)
            ? JavaMIME + mimeType
            : null;
    }


    public static String encodeDataFlavor(DataFlavor flav) {
        return (flav != null)
            ? SystemFlavorMap.encodeJavaMIMEType(flav.getMimeType())
            : null;
    }


    public static boolean isJavaMIMEType(String str) {
        return (str != null && str.startsWith(JavaMIME, 0));
    }


    public static String decodeJavaMIMEType(String nat) {
        return (isJavaMIMEType(nat))
            ? nat.substring(JavaMIME.length(), nat.length()).trim()
            : null;
    }


    public static DataFlavor decodeDataFlavor(String nat)
        throws ClassNotFoundException
    {
        String retval_str = SystemFlavorMap.decodeJavaMIMEType(nat);
        return (retval_str != null)
            ? new DataFlavor(retval_str)
            : null;
    }

    private static final class SoftCache<K, V> {
        Map<K, SoftReference<LinkedHashSet<V>>> cache;

        public void put(K key, LinkedHashSet<V> value) {
            if (cache == null) {
                cache = new HashMap<>(1);
            }
            cache.put(key, new SoftReference<>(value));
        }

        public void remove(K key) {
            if (cache == null) return;
            cache.remove(null);
            cache.remove(key);
        }

        public LinkedHashSet<V> check(K key) {
            if (cache == null) return null;
            SoftReference<LinkedHashSet<V>> ref = cache.get(key);
            if (ref != null) {
                return ref.get();
            }
            return null;
        }
    }
}
