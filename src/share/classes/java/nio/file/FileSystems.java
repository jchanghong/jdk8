package java.nio.file;
import java.nio.file.spi.FileSystemProvider;
import java.net.URI;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.lang.reflect.Constructor;
public final class FileSystems {
    private FileSystems() {
    }
    // lazy initialization of default file system
    private static class DefaultFileSystemHolder {
        static final FileSystem defaultFileSystem = defaultFileSystem();
        // returns default file system
        private static FileSystem defaultFileSystem() {
            // load default provider
            FileSystemProvider provider = AccessController
                .doPrivileged(new PrivilegedAction<FileSystemProvider>() {
                    public FileSystemProvider run() {
                        return getDefaultProvider();
                    }
                });
            // return file system
            return provider.getFileSystem(URI.create("file:///"));
        }
        // returns default provider
        private static FileSystemProvider getDefaultProvider() {
            FileSystemProvider provider = sun.nio.fs.DefaultFileSystemProvider.create();
            // if the property java.nio.file.spi.DefaultFileSystemProvider is
            // set then its value is the name of the default provider (or a list)
            String propValue = System
                .getProperty("java.nio.file.spi.DefaultFileSystemProvider");
            if (propValue != null) {
                for (String cn: propValue.split(",")) {
                    try {
                        Class<?> c = Class
                            .forName(cn, true, ClassLoader.getSystemClassLoader());
                        Constructor<?> ctor = c
                            .getDeclaredConstructor(FileSystemProvider.class);
                        provider = (FileSystemProvider)ctor.newInstance(provider);
                        // must be "file"
                        if (!provider.getScheme().equals("file"))
                            throw new Error("Default provider must use scheme 'file'");
                    } catch (Exception x) {
                        throw new Error(x);
                    }
                }
            }
            return provider;
        }
    }
    public static FileSystem getDefault() {
        return DefaultFileSystemHolder.defaultFileSystem;
    }
    public static FileSystem getFileSystem(URI uri) {
        String scheme = uri.getScheme();
        for (FileSystemProvider provider: FileSystemProvider.installedProviders()) {
            if (scheme.equalsIgnoreCase(provider.getScheme())) {
                return provider.getFileSystem(uri);
            }
        }
        throw new ProviderNotFoundException("Provider \"" + scheme + "\" not found");
    }
    public static FileSystem newFileSystem(URI uri, Map<String,?> env)
        throws IOException
    {
        return newFileSystem(uri, env, null);
    }
    public static FileSystem newFileSystem(URI uri, Map<String,?> env, ClassLoader loader)
        throws IOException
    {
        String scheme = uri.getScheme();
        // check installed providers
        for (FileSystemProvider provider: FileSystemProvider.installedProviders()) {
            if (scheme.equalsIgnoreCase(provider.getScheme())) {
                return provider.newFileSystem(uri, env);
            }
        }
        // if not found, use service-provider loading facility
        if (loader != null) {
            ServiceLoader<FileSystemProvider> sl = ServiceLoader
                .load(FileSystemProvider.class, loader);
            for (FileSystemProvider provider: sl) {
                if (scheme.equalsIgnoreCase(provider.getScheme())) {
                    return provider.newFileSystem(uri, env);
                }
            }
        }
        throw new ProviderNotFoundException("Provider \"" + scheme + "\" not found");
    }
    public static FileSystem newFileSystem(Path path,
                                           ClassLoader loader)
        throws IOException
    {
        if (path == null)
            throw new NullPointerException();
        Map<String,?> env = Collections.emptyMap();
        // check installed providers
        for (FileSystemProvider provider: FileSystemProvider.installedProviders()) {
            try {
                return provider.newFileSystem(path, env);
            } catch (UnsupportedOperationException uoe) {
            }
        }
        // if not found, use service-provider loading facility
        if (loader != null) {
            ServiceLoader<FileSystemProvider> sl = ServiceLoader
                .load(FileSystemProvider.class, loader);
            for (FileSystemProvider provider: sl) {
                try {
                    return provider.newFileSystem(path, env);
                } catch (UnsupportedOperationException uoe) {
                }
            }
        }
        throw new ProviderNotFoundException("Provider not found");
    }
}
