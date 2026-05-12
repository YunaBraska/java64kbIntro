package berlin.yuna;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Starts the demo after configuring the local macOS native-image runtime layout.
 */
public final class Noxius64kNativeLauncher {

    private Noxius64kNativeLauncher() {
    }

    /**
     * Configures the native output directory before AWT is initialized, then starts the demo.
     *
     * @param args command-line arguments passed to the demo
     */
    public static void main(final String[] args) {
        configureMacNativeImage();
        Noxius64kDemo.main(args);
    }

    static boolean enterMacAppLoopIfNeeded() {
        if (!isMacNativeImage()) {
            return false;
        }
        runMacAppLoop();
        return true;
    }

    static boolean exitMacAppLoopIfNeeded() {
        if (!isMacNativeImage()) {
            return false;
        }
        stopMacAppLoop();
        return true;
    }

    private static void configureMacNativeImage() {
        if (!isMacNativeImage()) {
            return;
        }

        final Path outputDir = executableDir().orElse(Path.of(".").toAbsolutePath()).normalize();
        final Path libDir = outputDir.resolve("lib");
        final String existingLibraryPath = System.getProperty("java.library.path", "");
        final String libraryPath = libDir + File.pathSeparator + outputDir
                + (existingLibraryPath.isBlank() ? "" : File.pathSeparator + existingLibraryPath);

        System.setProperty("java.home", outputDir.toString());
        System.setProperty("java.library.path", libraryPath);
        System.setProperty("sun.java2d.metal", "true");
        System.setProperty("sun.java2d.opengl", "false");
        System.loadLibrary("osxapp");
        System.loadLibrary("awt_lwawt");
        System.loadLibrary("jsound");
    }

    static boolean isMacNativeImage() {
        return isNativeImageRuntime() && isMac();
    }

    private static boolean isNativeImageRuntime() {
        return "runtime".equals(System.getProperty("org.graalvm.nativeimage.imagecode"));
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }

    private static Optional<Path> executableDir() {
        return ProcessHandle.current()
                .info()
                .command()
                .map(Path::of)
                .map(Path::toAbsolutePath)
                .map(Path::getParent);
    }

    private static native void runMacAppLoop();

    private static native void stopMacAppLoop();
}
