package tect.host.tpl.dependency;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Class loader for runtime-downloaded dependencies
 *.
 * Uses the plugin class loader as parent while keeping loaded libraries
 * isolated from the rest of the server
 */
public final class IsolatedClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public IsolatedClassLoader(URL[] urls, ClassLoader parent) {
        super("TChat-Dependencies", urls, parent);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}