package com.oracle.svm.core.posix;

import com.oracle.svm.core.Uninterruptible;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.jdk.NativeLibrarySupport;
import com.oracle.svm.core.posix.headers.Time;
import com.oracle.svm.core.util.TimeUtils;
import org.graalvm.nativeimage.StackValue;

import java.io.Console;
import java.io.File;

@TargetClass(java.lang.System.class)
final class Target_java_lang_System_Posix {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    static volatile Console cons;

    @Substitute
    @Uninterruptible(reason = "Called from uninterruptible code.", mayBeInlined = true)
    public static long currentTimeMillis() {
        final Time.timespec ts = StackValue.get(Time.timespec.class);
        final int status = PosixUtils.clock_gettime(Time.CLOCK_REALTIME(), ts);
        PosixUtils.checkStatusIs0(status, "System.currentTimeMillis(): clock_gettime(CLOCK_REALTIME) failed.");
        return ts.tv_sec() * TimeUtils.millisPerSecond + ts.tv_nsec() / TimeUtils.nanosPerMilli;
    }

    @Substitute
    public static void load(final String filename) {
        final String library = DarwinAwtLoadSupport.libraryName(filename);
        if (library != null) {
            NativeLibrarySupport.singleton().registerInitializedBuiltinLibrary(library);
            return;
        }
        NativeLibrarySupport.singleton().loadLibraryAbsolute(new File(filename));
    }

    @Substitute
    public static void loadLibrary(final String libname) {
        NativeLibrarySupport.singleton().loadLibraryRelative(libname);
    }
}

final class DarwinAwtLoadSupport {
    private DarwinAwtLoadSupport() {
    }

    static String libraryName(final String filename) {
        final String name = new File(filename).getName();
        if (!name.startsWith("lib") || !name.endsWith(".dylib")) {
            return null;
        }
        final String library = name.substring(3, name.length() - 6);
        return isDarwinAwtLibrary(library) ? library : null;
    }

    private static boolean isDarwinAwtLibrary(final String library) {
        return "awt".equals(library)
                || "awt_lwawt".equals(library)
                || "fontmanager".equals(library)
                || "freetype".equals(library)
                || "javajpeg".equals(library)
                || "jsound".equals(library)
                || "lcms".equals(library)
                || "mlib_image".equals(library)
                || "osxapp".equals(library)
                || "osxui".equals(library);
    }
}
