package com.oracle.svm.hosted.jdk;

import com.oracle.svm.core.feature.InternalFeature;
import com.oracle.svm.core.jdk.JNIRegistrationUtil;
import com.oracle.svm.core.jdk.NativeLibrarySupport;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeJNIAccess;

/**
 * Registers the Darwin AWT/Java2D JNI surface that GraalVM 25 does not wire by default.
 */
public final class NoxiusDarwinAwtFeature extends JNIRegistrationUtil implements InternalFeature {
    private static final String[] STATIC_LIBRARIES = {
            "awt",
            "awt_lwawt",
            "fontmanager",
            "freetype",
            "javajpeg",
            "jsound",
            "lcms",
            "mlib_image",
            "osxapp",
            "osxui"
    };

    private static final String[] JNI_CLASSES = {
            "java.awt.AlphaComposite",
            "java.awt.BasicStroke",
            "java.awt.Color",
            "java.awt.Composite",
            "java.awt.CompositeContext",
            "java.awt.Component",
            "java.awt.Container",
            "java.awt.Dimension",
            "java.awt.DisplayMode",
            "java.awt.Font",
            "java.awt.GradientPaint",
            "java.awt.GraphicsEnvironment",
            "java.awt.Insets",
            "java.awt.Paint",
            "java.awt.Point",
            "java.awt.RadialGradientPaint",
            "java.awt.Rectangle",
            "java.awt.Stroke",
            "java.awt.SystemColor",
            "java.awt.Toolkit",
            "java.awt.Transparency",
            "java.awt.Window",
            "java.awt.event.InputEvent",
            "java.awt.event.KeyEvent",
            "java.awt.geom.AffineTransform",
            "java.awt.geom.Dimension2D",
            "java.awt.geom.Ellipse2D",
            "java.awt.geom.Ellipse2D$Double",
            "java.awt.geom.Ellipse2D$Float",
            "java.awt.geom.GeneralPath",
            "java.awt.geom.Line2D",
            "java.awt.geom.Line2D$Double",
            "java.awt.geom.Line2D$Float",
            "java.awt.geom.Path2D",
            "java.awt.geom.Path2D$Double",
            "java.awt.geom.Path2D$Float",
            "java.awt.geom.Point2D",
            "java.awt.geom.Point2D$Double",
            "java.awt.geom.Rectangle2D",
            "java.awt.geom.Rectangle2D$Double",
            "java.awt.geom.Rectangle2D$Float",
            "java.awt.image.BufferedImage",
            "java.awt.image.ColorModel",
            "java.awt.image.ComponentColorModel",
            "java.awt.image.ComponentSampleModel",
            "java.awt.image.DataBuffer",
            "java.awt.image.DataBufferByte",
            "java.awt.image.DataBufferDouble",
            "java.awt.image.DataBufferFloat",
            "java.awt.image.DataBufferInt",
            "java.awt.image.DataBufferShort",
            "java.awt.image.DataBufferUShort",
            "java.awt.image.DirectColorModel",
            "java.awt.image.IndexColorModel",
            "java.awt.image.MultiPixelPackedSampleModel",
            "java.awt.image.PackedColorModel",
            "java.awt.image.Raster",
            "java.awt.image.SampleModel",
            "java.awt.image.SinglePixelPackedSampleModel",
            "java.awt.image.WritableRaster",
            "java.awt.print.PageFormat",
            "java.awt.print.Pageable",
            "java.awt.print.Paper",
            "java.awt.print.PrinterAbortException",
            "java.lang.Integer",
            "java.lang.NoSuchMethodException",
            "java.lang.Number",
            "java.lang.Object",
            "java.lang.Runnable",
            "java.lang.RuntimeException",
            "java.lang.String",
            "java.util.ArrayList",
            "java.util.Locale",
            "javax.sound.sampled.AudioFormat",
            "javax.sound.sampled.LineUnavailableException",
            "javax.sound.sampled.Mixer$Info",
            "com.sun.media.sound.DirectAudioDevice",
            "com.sun.media.sound.DirectAudioDeviceProvider",
            "com.sun.media.sound.DirectAudioDeviceProvider$DirectAudioDeviceInfo",
            "com.sun.media.sound.Platform",
            "com.sun.media.sound.PortMixer",
            "com.sun.media.sound.PortMixerProvider",
            "com.sun.media.sound.PortMixerProvider$PortMixerInfo",
            "javax.accessibility.Accessible",
            "javax.accessibility.AccessibleRole",
            "javax.accessibility.AccessibleState",
            "sun.awt.AWTAutoShutdown",
            "sun.awt.CGraphicsEnvironment",
            "sun.awt.SunHints",
            "sun.awt.SunHints$Value",
            "sun.awt.SunToolkit",
            "sun.awt.datatransfer.DataTransferer",
            "sun.awt.image.BufImgSurfaceData",
            "sun.awt.image.BufImgSurfaceData$ICMColorData",
            "sun.awt.image.ByteComponentRaster",
            "sun.awt.image.BytePackedRaster",
            "sun.awt.image.IntegerComponentRaster",
            "sun.awt.image.ShortComponentRaster",
            "sun.awt.image.SunWritableRaster",
            "sun.font.CFont",
            "sun.font.CFontManager",
            "sun.font.CharToGlyphMapper",
            "sun.font.CStrike",
            "sun.font.Font2D",
            "sun.font.GlyphLayout$GVData",
            "sun.font.GlyphList",
            "sun.font.PhysicalStrike",
            "sun.font.StandardGlyphVector",
            "sun.font.StandardGlyphVector$GlyphTransformInfo",
            "sun.font.StrikeMetrics",
            "sun.font.TrueTypeFont",
            "sun.font.Type1Font",
            "sun.java2d.Disposer",
            "sun.java2d.InvalidPipeException",
            "sun.java2d.NullSurfaceData",
            "sun.java2d.OSXOffScreenSurfaceData",
            "sun.java2d.RenderBuffer",
            "sun.java2d.RenderQueue",
            "sun.java2d.ShapeSpanIterator",
            "sun.java2d.SpanClipRenderer",
            "sun.java2d.SpanIterator",
            "sun.java2d.StateTrackable",
            "sun.java2d.StateTrackableDelegate",
            "sun.java2d.SunGraphics2D",
            "sun.java2d.SunGraphicsEnvironment",
            "sun.java2d.Surface",
            "sun.java2d.SurfaceData",
            "sun.java2d.SurfaceDataProxy",
            "sun.java2d.loops.Blit",
            "sun.java2d.loops.BlitBg",
            "sun.java2d.loops.CompositeType",
            "sun.java2d.loops.CustomComponent",
            "sun.java2d.loops.DrawGlyphList",
            "sun.java2d.loops.DrawGlyphListAA",
            "sun.java2d.loops.DrawGlyphListColor",
            "sun.java2d.loops.DrawGlyphListLCD",
            "sun.java2d.loops.DrawLine",
            "sun.java2d.loops.DrawParallelogram",
            "sun.java2d.loops.DrawPath",
            "sun.java2d.loops.DrawPolygons",
            "sun.java2d.loops.DrawRect",
            "sun.java2d.loops.FillParallelogram",
            "sun.java2d.loops.FillPath",
            "sun.java2d.loops.FillRect",
            "sun.java2d.loops.FillSpans",
            "sun.java2d.loops.FontInfo",
            "sun.java2d.loops.GeneralRenderer",
            "sun.java2d.loops.GraphicsPrimitive",
            "sun.java2d.loops.GraphicsPrimitiveMgr",
            "sun.java2d.loops.GraphicsPrimitiveProxy",
            "sun.java2d.loops.MaskBlit",
            "sun.java2d.loops.MaskFill",
            "sun.java2d.loops.ProcessPath",
            "sun.java2d.loops.RenderCache",
            "sun.java2d.loops.RenderLoops",
            "sun.java2d.loops.ScaledBlit",
            "sun.java2d.loops.SurfaceType",
            "sun.java2d.loops.TransformBlit",
            "sun.java2d.loops.TransformHelper",
            "sun.java2d.loops.XORComposite",
            "sun.java2d.metal.MTLBlitLoops",
            "sun.java2d.metal.MTLBufImgOps",
            "sun.java2d.metal.MTLContext",
            "sun.java2d.metal.MTLDrawImage",
            "sun.java2d.metal.MTLGraphicsConfig",
            "sun.java2d.metal.MTLLayer",
            "sun.java2d.metal.MTLMaskBlit",
            "sun.java2d.metal.MTLMaskFill",
            "sun.java2d.metal.MTLPaints",
            "sun.java2d.metal.MTLRenderer",
            "sun.java2d.metal.MTLRenderQueue",
            "sun.java2d.metal.MTLSurfaceData",
            "sun.java2d.metal.MTLSurfaceDataProxy",
            "sun.java2d.metal.MTLTextRenderer",
            "sun.java2d.metal.MTLVolatileSurfaceManager",
            "sun.java2d.opengl.CGLLayer",
            "sun.java2d.opengl.OGLSurfaceData",
            "sun.java2d.pipe.Region",
            "sun.java2d.pipe.RegionIterator",
            "sun.lwawt.LWComponentPeer",
            "sun.lwawt.LWWindowPeer",
            "sun.lwawt.macosx.CAccessibility",
            "sun.lwawt.macosx.CAccessible",
            "sun.lwawt.macosx.CAccessibleText",
            "sun.lwawt.macosx.CCheckboxMenuItem",
            "sun.lwawt.macosx.CClipboard",
            "sun.lwawt.macosx.CDragSourceContextPeer",
            "sun.lwawt.macosx.CDropTargetContextPeer",
            "sun.lwawt.macosx.CFileDialog",
            "sun.lwawt.macosx.CInputMethod",
            "sun.lwawt.macosx.CMenuItem",
            "sun.lwawt.macosx.CPlatformComponent",
            "sun.lwawt.macosx.CPlatformView",
            "sun.lwawt.macosx.CPlatformWindow",
            "sun.lwawt.macosx.CPrinterDialog",
            "sun.lwawt.macosx.CPrinterJob",
            "sun.lwawt.macosx.CPrinterJobDialog",
            "sun.lwawt.macosx.CPrinterPageDialog",
            "sun.lwawt.macosx.CFRetainedResource",
            "sun.lwawt.macosx.CFRetainedResource$CFNativeAction",
            "sun.lwawt.macosx.CFRetainedResource$CFNativeActionGet",
            "sun.lwawt.macosx.CTrayIcon",
            "sun.lwawt.macosx.CViewEmbeddedFrame",
            "sun.lwawt.macosx.LWCToolkit",
            "sun.lwawt.macosx.NSEvent",
            "berlin.yuna.Noxius64kNativeLauncher"
    };

    @Override
    public boolean isInConfiguration(final IsInConfigurationAccess access) {
        return Platform.includedIn(Platform.DARWIN.class);
    }

    @Override
    public void afterRegistration(final AfterRegistrationAccess access) {
        final NativeLibrarySupport nativeLibraries = NativeLibrarySupport.singleton();
        for (final String library : STATIC_LIBRARIES) {
            nativeLibraries.preregisterUninitializedBuiltinLibrary(library);
        }
        RuntimeClassInitialization.initializeAtRunTime("sun.java2d.MacOSFlags");
        RuntimeClassInitialization.initializeAtRunTime("sun.lwawt.macosx");
    }

    @Override
    public void beforeAnalysis(final BeforeAnalysisAccess access) {
        if (Runtime.version().major() >= 19) {
            registerJni(access, "sun.font.FontUtilities");
        }
        for (final String className : JNI_CLASSES) {
            registerJni(access, className);
        }
        registerJniMethod(java.lang.System.class, "load", String.class);
    }

    @Override
    public void afterAnalysis(final AfterAnalysisAccess access) {
        final JNIRegistrationSupport jniRegistrationSupport = JNIRegistrationSupport.singleton();
        if (!jniRegistrationSupport.isRegisteredLibrary("awt")) {
            return;
        }

        jniRegistrationSupport.addJvmShimExports(
                "JVM_IsStaticallyLinked",
                "jio_fprintf",
                "jio_snprintf");
        jniRegistrationSupport.addJavaShimExports(
                "JNU_CallMethodByName",
                "JNU_CallStaticMethodByName",
                "JNU_GetEnv",
                "JNU_GetStaticFieldByName",
                "JNU_GetStringPlatformChars",
                "JNU_IsInstanceOfByName",
                "JNU_NewObjectByName",
                "JNU_NewStringPlatform",
                "JNU_ReleaseStringPlatformChars",
                "JNU_SetFieldByName",
                "JNU_ThrowArrayIndexOutOfBoundsException",
                "JNU_ThrowByName",
                "JNU_ThrowIllegalArgumentException",
                "JNU_ThrowInternalError",
                "JNU_ThrowIOException",
                "JNU_ThrowNullPointerException",
                "JNU_ThrowOutOfMemoryError");

        for (final String library : STATIC_LIBRARIES) {
            jniRegistrationSupport.registerLibrary(library);
        }
    }

    private static void registerJni(final BeforeAnalysisAccess access, final String className) {
        final Class<?> type = access.findClassByName(className);
        if (type == null) {
            return;
        }
        RuntimeJNIAccess.register(type);
        RuntimeJNIAccess.register(type.getDeclaredFields());
        RuntimeJNIAccess.register(type.getDeclaredMethods());
        RuntimeJNIAccess.register(type.getDeclaredConstructors());
    }

    private static void registerJniMethod(final Class<?> type, final String name, final Class<?>... parameterTypes) {
        try {
            RuntimeJNIAccess.register(type);
            RuntimeJNIAccess.register(type.getDeclaredMethod(name, parameterTypes));
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
