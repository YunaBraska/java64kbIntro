package berlin.yuna;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferStrategy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class Noxius64kDemo extends Canvas implements Runnable, KeyListener {

    private static final int RENDER_W = 640;
    private static final int RENDER_H = 360;
    private static final int SCALE = 2;
    private static final int SAMPLE_RATE = 44_100;
    private static final double TPS = 60.0;
    private static final int SECTIONS = 7;
    private static final int BIRTH = 0;
    private static final int FLUID = 1;
    private static final int CORRIDOR = 2;
    private static final int MACHINE = 3;
    private static final int WORLD = 4;
    private static final int TRANSFORM = 5;
    private static final int ASCEND = 6;
    private static final int CITY = 7;
    private static final int LAND = 8;
    private static final int SPACE = 9;
    private static final int ORGANIC = 10;
    private static final int GEARS = 11;
    private static final int INTRO = 0;
    private static final int GROOVE = 1;
    private static final int BUILD = 2;
    private static final int DROP = 3;
    private static final int EXPAND = 4;
    private static final int RELEASE = 5;
    private static final int FINALE = 6;
    private static final double FAR = 1_850.0;
    private static final double[] SECTION_BEATS = {48, 64, 64, 64, 72, 56, 72};
    private static final double[] SECTION_INTENSITY = {0.18, 0.42, 0.58, 0.98, 0.76, 0.50, 0.86};
    private static final double[] SECTION_BRIGHTNESS = {0.28, 0.46, 0.62, 0.90, 0.68, 0.54, 0.82};
    private static final double[] SECTION_DRIVE = {0.02, 0.42, 0.66, 1.00, 0.80, 0.30, 0.88};
    private static final int[][] PROGRESSIONS = {
            {0, -5, -8, -3, 3, -2, -7, -5},
            {0, -7, -3, -10, 2, -5, -8, -3},
            {0, 3, -5, -2, -9, -7, -4, -5},
            {0, -2, -7, -5, 5, 3, -4, -2}
    };
    private static final int[][] BASS_PATTERNS = {
            {0, 0, 0, 7, 0, -5, 0, 10, 0, 0, -2, 0, 3, 5, 7, -5},
            {0, 0, 10, 0, -2, 0, 7, 0, 0, 12, 10, 7, 5, 3, -2, -5},
            {0, 0, 0, -12, 7, 0, 10, 0, 3, 0, -5, 0, 5, 7, 10, 12},
            {0, -12, 0, 7, 0, 10, 7, 5, 0, 3, -2, 0, -5, 0, 7, 10}
    };
    private static final int[][] LEAD_PATTERNS = {
            {12, 15, 19, 22, 24, 22, 19, 15, 12, 17, 20, 24, 27, 24, 20, 17},
            {12, 19, 22, 24, 27, 31, 29, 24, 22, 19, 15, 17, 22, 24, 27, 22},
            {15, 19, 24, 27, 31, 27, 24, 22, 19, 22, 27, 29, 31, 34, 31, 27},
            {12, 14, 19, 21, 26, 24, 21, 19, 17, 19, 24, 26, 29, 31, 29, 24}
    };
    private static final int[][] VISUAL_ORDERS = {
            {BIRTH, FLUID, CORRIDOR, MACHINE, WORLD, TRANSFORM, ASCEND},
            {FLUID, SPACE, WORLD, CITY, MACHINE, TRANSFORM, ASCEND},
            {MACHINE, CORRIDOR, ORGANIC, LAND, SPACE, BIRTH, ASCEND},
            {WORLD, FLUID, BIRTH, CITY, LAND, MACHINE, ASCEND},
            {TRANSFORM, ORGANIC, CORRIDOR, FLUID, SPACE, CITY, ASCEND},
            {CITY, LAND, MACHINE, FLUID, CORRIDOR, ORGANIC, ASCEND},
            {SPACE, BIRTH, LAND, MACHINE, CITY, TRANSFORM, ASCEND},
            {ORGANIC, FLUID, SPACE, CORRIDOR, WORLD, MACHINE, ASCEND},
            {LAND, CITY, BIRTH, SPACE, TRANSFORM, GEARS, ASCEND},
            {GEARS, CITY, MACHINE, CORRIDOR, SPACE, ORGANIC, ASCEND},
            {FLUID, GEARS, LAND, BIRTH, CITY, TRANSFORM, ASCEND}
    };
    private static final int[][] MUSIC_ORDERS = {
            {INTRO, GROOVE, BUILD, DROP, EXPAND, RELEASE, FINALE},
            {INTRO, BUILD, GROOVE, EXPAND, DROP, RELEASE, FINALE},
            {GROOVE, INTRO, BUILD, DROP, RELEASE, EXPAND, FINALE},
            {INTRO, RELEASE, BUILD, GROOVE, DROP, EXPAND, FINALE},
            {BUILD, INTRO, GROOVE, DROP, EXPAND, RELEASE, FINALE},
            {INTRO, GROOVE, DROP, RELEASE, BUILD, EXPAND, FINALE}
    };
    private static final int[][] KICK_BANKS = {
            {0x0000, 0x1111, 0x1911, 0x5555, 0x5155, 0x1011, 0x5555},
            {0x0001, 0x1011, 0x1511, 0x5155, 0x1915, 0x1001, 0x5155},
            {0x0101, 0x1110, 0x5111, 0x5955, 0x5151, 0x1010, 0x5D55},
            {0x0000, 0x1101, 0x1515, 0xD155, 0x5515, 0x1100, 0xD555}
    };
    private static final int[][] SNARE_BANKS = {
            {0x0000, 0x1010, 0x1810, 0x5850, 0x1010, 0x1000, 0x5850},
            {0x0000, 0x1000, 0x1818, 0x5890, 0x1810, 0x0000, 0x58D0},
            {0x0000, 0x0010, 0x1018, 0x5858, 0x1010, 0x1010, 0xD858},
            {0x0000, 0x1010, 0x5010, 0xD050, 0x1818, 0x1000, 0xD0D0}
    };
    private static final int[][] HAT_BANKS = {
            {0x0000, 0x2222, 0xAAAA, 0xEEEE, 0xAAEA, 0x2022, 0xFEFE},
            {0x0000, 0x0202, 0x8A8A, 0xAEAE, 0xA2EA, 0x0200, 0xFAEA},
            {0x0000, 0x2020, 0xAAAA, 0xEAEA, 0xAAAA, 0x2002, 0xEEEE},
            {0x0000, 0x2220, 0xA2AA, 0xFEAE, 0xEA2A, 0x0020, 0xFAFE}
    };
    private static final Color BLACK = new Color(0, 0, 0);

    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean[] keys = new AtomicBoolean[768];
    private final boolean[] previousKeys = new boolean[768];
    private final AtomicLong interactionBits = new AtomicLong(Double.doubleToLongBits(0.0));
    private final BufferedImage scene = new BufferedImage(RENDER_W, RENDER_H, BufferedImage.TYPE_INT_ARGB);
    private final BufferedImage glow = new BufferedImage(RENDER_W, RENDER_H, BufferedImage.TYPE_INT_ARGB);
    private final Particle[] particles = new Particle[2_400];
    private final Pulse[] pulses = new Pulse[14];
    private final long seed;
    private final int bpm;
    private final int rootNote;
    private final double beatSeconds;
    private final double introStretch;
    private final double cycleBeats;
    private final int harmonyVariant;
    private final int bassVariant;
    private final int leadVariant;
    private final int openingVariant;
    private final int cameraVariant;
    private final int sequenceVariant;
    private final int musicVariant;
    private final int grooveVariant;
    private final int beatShift;
    private final int backdropVariant;
    private final int kitVariant;
    private long startNanos;
    private double time;
    private double nudgeX;
    private double nudgeY;
    private double interactionEnergy;
    private double holdEnergy;
    private double tapPulse;
    private double breathing;
    private int pulseCursor;

    public Noxius64kDemo() {
        seed = System.nanoTime() ^ (System.currentTimeMillis() << 21) ^ Runtime.getRuntime().freeMemory();
        bpm = 102 + (int) (hash01(seed ^ 0xC0FFEE22L) * 19.0);
        rootNote = 29 + (int) (hash01(seed ^ 0x51A7E11AL) * 9.0);
        beatSeconds = 60.0 / bpm;
        introStretch = 0.78 + hash01(seed ^ 0xB11D5EEDL) * 0.62;
        harmonyVariant = (int) (hash01(seed ^ 0xCAFE1234L) * PROGRESSIONS.length);
        bassVariant = (int) (hash01(seed ^ 0xB4551234L) * BASS_PATTERNS.length);
        leadVariant = (int) (hash01(seed ^ 0x1EAD1234L) * LEAD_PATTERNS.length);
        openingVariant = (int) (hash01(seed ^ 0x0F3A9E21L) * 3.0);
        cameraVariant = (int) (hash01(seed ^ 0x7A6C01DEL) * 4.0);
        sequenceVariant = (int) (hash01(seed ^ 0x5C3E3A11L) * VISUAL_ORDERS.length);
        musicVariant = (int) (hash01(seed ^ 0xA0D106EEL) * MUSIC_ORDERS.length);
        grooveVariant = (int) (hash01(seed ^ 0x6000F00DL) * KICK_BANKS.length);
        beatShift = (int) (hash01(seed ^ 0xB3A7516EL) * 4.0) * 2;
        backdropVariant = (int) (hash01(seed ^ 0xBACD10FFL) * 7.0);
        kitVariant = (int) (hash01(seed ^ 0xD20D20D2L) * 4.0);
        cycleBeats = computeCycleBeats();

        setPreferredSize(new Dimension(RENDER_W * SCALE, RENDER_H * SCALE));
        setFocusable(true);
        setIgnoreRepaint(true);
        addKeyListener(this);

        for (int i = 0; i < keys.length; i++) {
            keys[i] = new AtomicBoolean();
        }
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle(seed + i * 0x9E3779B97F4A7C15L);
        }
        for (int i = 0; i < pulses.length; i++) {
            pulses[i] = new Pulse();
        }
    }

    public static void main(final String[] args) {
        final Frame frame = new Frame("Aperture of the Black Sun");
        final Noxius64kDemo demo = new Noxius64kDemo();

        frame.setResizable(false);
        frame.add(demo);
        frame.pack();
        if (Noxius64kNativeLauncher.isMacNativeImage()) {
            frame.setSize(RENDER_W * SCALE, RENDER_H * SCALE);
        }
        frame.setLocationRelativeTo(null);
        frame.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(final WindowEvent event) {
                demo.requestFocusInWindow();
            }

            @Override
            public void windowLostFocus(final WindowEvent event) {
                demo.clearKeys();
            }

            @Override
            public void windowClosing(final WindowEvent event) {
                demo.stop();
                frame.dispose();
                if (Noxius64kNativeLauncher.exitMacAppLoopIfNeeded()) {
                    System.exit(0);
                }
            }
        });
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event -> {
            if (!frame.isActive()) {
                return false;
            }
            if (Noxius64kNativeLauncher.isMacNativeImage()
                    && event.getID() == KeyEvent.KEY_PRESSED
                    && event.getKeyCode() == KeyEvent.VK_Q
                    && (event.getModifiersEx() & KeyEvent.META_DOWN_MASK) != 0) {
                demo.stop();
                frame.dispose();
                System.exit(0);
                return true;
            }
            if (event.getID() == KeyEvent.KEY_PRESSED) {
                demo.applyKey(event.getKeyCode(), true);
            } else if (event.getID() == KeyEvent.KEY_RELEASED) {
                demo.applyKey(event.getKeyCode(), false);
            }
            return false;
        });

        frame.setVisible(true);
        demo.requestFocusInWindow();
        demo.start();
        if (Noxius64kNativeLauncher.enterMacAppLoopIfNeeded()) {
            demo.stop();
            frame.dispose();
            System.exit(0);
        }
    }

    private boolean start() {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        startNanos = System.nanoTime();
        final Thread loop = new Thread(this, "noxius-demo-loop");
        loop.start();
        startAudio();
        return true;
    }

    private Thread startAudio() {
        final Thread audio = new Thread(this::runAudio, "noxius-demo-audio");
        audio.setDaemon(true);
        audio.start();
        return audio;
    }

    private boolean stop() {
        return running.getAndSet(false);
    }

    @Override
    public void run() {
        createBufferStrategy(2);
        final BufferStrategy strategy = getBufferStrategy();
        final double nanosPerTick = 1_000_000_000.0 / TPS;
        long previous = System.nanoTime();
        double lag = 0.0;

        while (running.get()) {
            final long now = System.nanoTime();
            lag += now - previous;
            previous = now;
            while (lag >= nanosPerTick) {
                update(1.0 / TPS);
                lag -= nanosPerTick;
            }
            render(strategy);
            try {
                Thread.sleep(1L);
            } catch (final InterruptedException ignored) {
                Thread.currentThread().interrupt();
                running.set(false);
            }
        }
    }

    private void update(final double dt) {
        time = (System.nanoTime() - startNanos) * 0.000000001;
        final Signals signals = signalsAt(time);
        final double axisX = axis(KeyEvent.VK_D, KeyEvent.VK_RIGHT) - axis(KeyEvent.VK_A, KeyEvent.VK_LEFT);
        final double axisY = axis(KeyEvent.VK_S, KeyEvent.VK_DOWN) - axis(KeyEvent.VK_W, KeyEvent.VK_UP);
        final boolean primaryTap = rising(KeyEvent.VK_SPACE) || rising(KeyEvent.VK_ENTER);
        final boolean secondaryTap = rising(KeyEvent.VK_SHIFT);

        nudgeX += (axisX - nudgeX) * (1.0 - Math.pow(0.0004, dt));
        nudgeY += (axisY - nudgeY) * (1.0 - Math.pow(0.0004, dt));
        holdEnergy += ((pressed(KeyEvent.VK_SHIFT) ? 1.0 : 0.0) - holdEnergy) * (1.0 - Math.pow(0.002, dt));
        interactionEnergy *= Math.pow(0.075, dt);
        tapPulse *= Math.pow(0.025, dt);

        if (primaryTap || secondaryTap) {
            final double beatDistance = Math.min(signals.beatPhase, 1.0 - signals.beatPhase);
            final double accuracy = smoothstep(0.20, 0.00, beatDistance);
            final double strength = (primaryTap ? 0.85 : 0.45) + accuracy * 1.75 + signals.barPulse * 0.35;
            interactionEnergy += strength;
            tapPulse = Math.max(tapPulse, strength);
            pulses[pulseCursor].reset(time, strength, signals.section, signals.loop);
            pulseCursor = (pulseCursor + 1) % pulses.length;
        }

        breathing += dt * (0.5 + signals.intensity * 0.8);
        interactionBits.set(Double.doubleToLongBits(clamp(interactionEnergy + holdEnergy * 0.65, 0.0, 4.0)));
        for (int i = 0; i < keys.length; i++) {
            previousKeys[i] = keys[i].get();
        }
    }

    private double axis(final int positiveKey, final int positiveAlt) {
        return pressed(positiveKey) || pressed(positiveAlt) ? 1.0 : 0.0;
    }

    private boolean rising(final int key) {
        return pressed(key) && key >= 0 && key < previousKeys.length && !previousKeys[key];
    }

    private void render(final BufferStrategy strategy) {
        final Signals signals = signalsAt(time);
        final Camera camera = cameraAt(signals);
        final Palette palette = paletteAt(signals);
        final Graphics2D g = scene.createGraphics();
        final Graphics2D fx = glow.createGraphics();
        try {
            configure(g);
            configure(fx);
            clearBuffers(g, fx, palette);
            drawBackground(g, fx, signals, palette);
            drawSection(g, fx, signals, camera, palette);
            drawParticles(g, fx, signals, camera, palette);
            drawInteractionPulses(g, fx, signals, camera, palette);
            drawAtmosphere(g, fx, signals, palette);
        } finally {
            g.dispose();
            fx.dispose();
        }

        do {
            do {
                final Graphics2D out = (Graphics2D) strategy.getDrawGraphics();
                try {
                    out.setColor(BLACK);
                    out.fillRect(0, 0, getWidth(), getHeight());
                    out.scale(SCALE, SCALE);
                    drawPost(out, signals);
                } finally {
                    out.dispose();
                }
            } while (strategy.contentsRestored());
            strategy.show();
        } while (strategy.contentsLost());
    }

    private void configure(final Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    private void clearBuffers(final Graphics2D g, final Graphics2D fx, final Palette palette) {
        g.setComposite(AlphaComposite.Src);
        g.setPaint(new GradientPaint(0, 0, palette.skyTop, 0, RENDER_H, palette.skyBottom));
        g.fillRect(0, 0, RENDER_W, RENDER_H);
        g.setComposite(AlphaComposite.SrcOver);

        fx.setComposite(AlphaComposite.Clear);
        fx.fillRect(0, 0, RENDER_W, RENDER_H);
        fx.setComposite(AlphaComposite.SrcOver);
    }

    private void drawBackground(final Graphics2D g, final Graphics2D fx, final Signals s, final Palette p) {
        final double lowGlow = 0.18 + s.low * 0.12 + interactionEnergy * 0.020;
        final Point2D center = new Point2D.Double(RENDER_W * 0.5, RENDER_H * (0.48 + s.release * 0.06));
        final float radius = (float) (RENDER_H * (0.68 + s.intensity * 0.18));
        g.setPaint(new RadialGradientPaint(center, radius,
                new float[]{0.0f, 0.42f, 1.0f},
                new Color[]{withAlpha(p.core, lowGlow), withAlpha(p.haze, 0.08), withAlpha(BLACK, 0.0)}));
        g.fillRect(0, 0, RENDER_W, RENDER_H);

        drawBackdrop(g, fx, s, p);

        final double sx = RENDER_W * (0.48 + Math.sin(seed * 0.00001 + s.section) * 0.14);
        final double sy = RENDER_H * (0.33 - s.release * 0.08);
        final int shafts = 10 + (int) (s.intensity * 10.0);
        for (int i = 0; i < shafts; i++) {
            final double h = hash01(seed + s.section * 101L + i * 19L);
            final double angle = -0.7 + h * 1.4 + Math.sin(time * 0.05 + i) * 0.04;
            final double reach = RENDER_H * (1.1 + h * 0.7);
            final Path2D.Double path = new Path2D.Double();
            path.moveTo(sx, sy);
            path.lineTo(sx + Math.cos(angle - 0.035) * reach, sy + Math.sin(angle - 0.035) * reach + RENDER_H * 0.55);
            path.lineTo(sx + Math.cos(angle + 0.035) * reach, sy + Math.sin(angle + 0.035) * reach + RENDER_H * 0.55);
            path.closePath();
            g.setColor(withAlpha(p.glow, (0.012 + s.high * 0.012) * (0.4 + h)));
            g.fill(path);
        }
    }

    private void drawBackdrop(final Graphics2D g, final Graphics2D fx, final Signals s, final Palette p) {
        final int family = visualFamily(s.section);
        final int motif = Math.floorMod(backdropVariant + family + s.section, 7);
        if (motif == 6) {
            return;
        }
        final int cx = (int) (RENDER_W * (0.50 + Math.sin(seed * 0.00001 + s.section * 0.8) * 0.12));
        final int cy = (int) (RENDER_H * (0.34 - s.release * 0.08 + Math.sin(seed * 0.00002) * 0.04));
        final int size = (int) (RENDER_H * (0.48 + hash01(seed + motif * 313L) * 1.15 + s.low * 0.10));
        fx.setColor(withAlpha(p.glow, 0.10 + s.high * 0.08));
        switch (motif) {
            case 0 -> {
                fx.fillOval(cx - size / 2, cy - size / 2, size, size);
                g.setColor(withAlpha(p.core, 0.07 + s.low * 0.04));
                g.fillOval(cx - size / 2, cy - size / 2, size, size);
                g.setColor(withAlpha(BLACK, 0.34 - s.release * 0.12));
                g.fillOval(cx - size / 3, cy - size / 3, size * 2 / 3, size * 2 / 3);
            }
            case 1 -> drawRing(g, fx, cameraAt(s), new Vec3(0, 130, 700), 250.0 + s.low * 32.0, time * 0.02, p.glow, 0.18, 4.0);
            case 2 -> {
                fx.fillRect(cx - size / 18, 0, size / 9, RENDER_H);
                g.setColor(withAlpha(p.glow, 0.05 + s.low * 0.03));
                g.fillRect(cx - size / 36, 0, size / 18, RENDER_H);
            }
            case 3 -> {
                for (int i = 0; i < 5; i++) {
                    g.setColor(withAlpha(p.edge, 0.03 + s.high * 0.02));
                    g.drawOval(cx - size / 2 + i * 18, cy - size / 2 + i * 18, size - i * 36, size - i * 36);
                }
            }
            case 4 -> {
                final Path2D.Double pth = new Path2D.Double();
                pth.moveTo(cx, cy - size / 2.0);
                pth.lineTo(cx + size * 0.42, cy + size * 0.25);
                pth.lineTo(cx - size * 0.42, cy + size * 0.25);
                pth.closePath();
                g.setColor(withAlpha(p.core, 0.06 + s.release * 0.03));
                g.fill(pth);
                fx.setColor(withAlpha(p.glow, 0.08));
                fx.fill(pth);
            }
            default -> {
                fx.fillOval(cx - size / 4, cy - size / 4, size / 2, size / 2);
                g.setColor(withAlpha(p.glow, 0.05 + s.barPulse * 0.03));
                g.drawOval(cx - size / 2, cy - size / 2, size, size);
            }
        }
    }

    private void drawSection(final Graphics2D g, final Graphics2D fx, final Signals s, final Camera camera, final Palette p) {
        switch (visualFamily(s.section)) {
            case BIRTH -> drawBirth(g, fx, s, camera, p);
            case FLUID -> drawMirrorSea(g, fx, s, camera, p);
            case CORRIDOR -> drawCorridor(g, fx, s, camera, p);
            case MACHINE -> drawMachine(g, fx, s, camera, p);
            case WORLD -> drawExterior(g, fx, s, camera, p);
            case TRANSFORM -> drawTransformation(g, fx, s, camera, p);
            case CITY -> drawCity(g, fx, s, camera, p);
            case LAND -> drawLandscape(g, fx, s, camera, p);
            case SPACE -> drawUniverse(g, fx, s, camera, p);
            case ORGANIC -> drawOrganic(g, fx, s, camera, p);
            case GEARS -> drawGears(g, fx, s, camera, p);
            default -> drawAscension(g, fx, s, camera, p);
        }
        if (s.sectionBlend > 0.0) {
            g.setColor(withAlpha(p.glow, s.sectionBlend * 0.05));
            g.fillRect(0, 0, RENDER_W, RENDER_H);
            fx.setColor(withAlpha(p.glow, s.sectionBlend * 0.10));
            fx.fillOval(RENDER_W / 2 - 240, RENDER_H / 2 - 240, 480, 480);
        }
    }

    private void drawBirth(final Graphics2D g, final Graphics2D fx, final Signals s, final Camera camera, final Palette p) {
        final double reveal = smoothstep(0.05, 0.78, s.sectionProgress);
        drawRing(g, fx, camera, new Vec3(0, 58, 250), 72 + reveal * 140 + tapPulse * 8.0,
                time * 0.16, p.glow, 0.36 + s.barPulse * 0.22, 5.0);
        drawRing(g, fx, camera, new Vec3(0, 58, 250), 148 + reveal * 220,
                -time * 0.09, p.core, 0.18 + s.high * 0.12, 2.0);
        if (openingVariant == 2) {
            drawOrb(g, fx, camera, new Vec3(0, 68, 290), 86.0 + reveal * 88.0, p.core, p.glow,
                    0.18 + reveal * 0.30 + s.low * 0.10);
        }
        for (int i = 0; i < 9; i++) {
            final double a = i * Math.PI * 2.0 / 9.0 + time * 0.05;
            final Vec3 c = new Vec3(Math.cos(a) * (96.0 + reveal * 44.0), 48.0 + Math.sin(a * 3.0) * 18.0, 225.0 + Math.sin(a) * 96.0);
            if (openingVariant == 1 && (i & 1) == 0) {
                drawPyramid(g, fx, camera, new Vec3(c.x, 0, c.z), 40.0 + reveal * 24.0,
                        92.0 + reveal * 80.0 + Math.sin(time * 0.5 + i) * 12.0, p.solid, p.edge, 0.28 + reveal * 0.38);
            } else {
                drawWireCube(g, fx, camera, c, 24.0 + reveal * 18.0, a + time * 0.11, p.edge, 0.24 + reveal * 0.35);
            }
        }
        drawBeam(g, fx, camera, new Vec3(0, -18, 140), new Vec3(0, 220, 380), 18.0 + reveal * 22.0,
                p.glow, 0.24 + reveal * 0.34);
    }

    private void drawCorridor(final Graphics2D g, final Graphics2D fx, final Signals s, final Camera camera, final Palette p) {
        final double travel = (time * (62.0 + s.intensity * 40.0)) % 96.0;
        drawFloor(g, fx, camera, p, -220, 1_520, 0.20 + s.low * 0.10, true);
        for (int i = -2; i < 24; i++) {
            final double z = i * 96.0 - travel;
            final double fade = 1.0 - clamp((z - 920.0) / 620.0, 0.0, 1.0);
            drawBox(g, fx, camera, new Vec3(-178, 58, z), 34, 116, 26, 0.0, p.solid, p.edge, 0.42 * fade);
            drawBox(g, fx, camera, new Vec3(178, 58, z), 34, 116, 26, 0.0, p.solid, p.edge, 0.42 * fade);
            if ((i & 1) == 0) {
                drawLine3(g, fx, camera, new Vec3(-174, 124, z), new Vec3(174, 124, z), p.edge, 0.26 * fade, 2.0);
            }
        }
        for (int side = -1; side <= 1; side += 2) {
            final double x = side * 78.0;
            drawLine3(g, fx, camera, new Vec3(x, 7, -180), new Vec3(x, 8, 1_430), p.glow, 0.76, 3.2 + s.low * 3.0);
            drawLine3(g, fx, camera, new Vec3(side * 145.0, 93, -180), new Vec3(side * 145.0, 96, 1_430), p.edge, 0.38, 2.0);
        }
        final double gatePulse = 1.0 + s.barPulse * 0.16 + interactionEnergy * 0.035;
        drawRing(g, fx, camera, new Vec3(0, 62, 920), 140.0 * gatePulse, Math.PI * 0.5, p.glow, 0.46, 5.0);
        drawWireCube(g, fx, camera, new Vec3(0, 62, 900), 132.0 * gatePulse, time * 0.04, p.edge, 0.32);
    }

    private void drawExterior(final Graphics2D g, final Graphics2D fx, final Signals s, final Camera camera, final Palette p) {
        drawFloor(g, fx, camera, p, -420, 1_260, 0.18 + s.low * 0.08, false);
        for (int i = 0; i < 18; i++) {
            final long objectSeed = seed + s.loop * 4099L + i * 337L;
            final double lane = Math.floor(hash01(objectSeed) * 7.0) - 3.0;
            final double x = lane * 105.0 + (hash01(objectSeed + 11L) - 0.5) * 44.0;
            final double z = -120.0 + i * 82.0 + hash01(objectSeed + 41L) * 72.0;
            final double h = 82.0 + hash01(objectSeed + 77L) * 210.0;
            final double w = 28.0 + hash01(objectSeed + 91L) * 48.0;
            final double active = 0.35 + smoothstep(0.20, 0.90, s.sectionProgress) * 0.55;
            drawBox(g, fx, camera, new Vec3(x, h * 0.5, z), w, h, w * 0.82, hash01(objectSeed + 3L) * 0.3,
                    p.solid, p.edge, active * (0.4 + hash01(objectSeed + 5L) * 0.6));
            if (hash01(objectSeed + 9L) > 0.55) {
                drawBeam(g, fx, camera, new Vec3(x, h + 12.0, z), new Vec3(x * 0.25, h + 300.0, z + 160.0),
                        6.0 + s.low * 9.0, p.glow, 0.18 + s.barPulse * 0.10);
            }
        }
        drawRing(g, fx, camera, new Vec3(-210, 72, 430), 128.0 + s.low * 18.0, time * 0.03, p.glow, 0.31, 3.5);
        drawBox(g, fx, camera, new Vec3(220, 92, 520), 130, 184, 130, Math.PI * 0.25, p.solid, p.edge, 0.62);
    }

    private void drawMirrorSea(final Graphics2D g, final Graphics2D fx, final Signals s, final Camera camera, final Palette p) {
        drawFloor(g, fx, camera, p, -360, 1_500, 0.26 + s.high * 0.08, true);
        final int horizon = (int) (RENDER_H * 0.60);
        g.setColor(withAlpha(p.haze, 0.08 + s.high * 0.04));
        g.fillRect(0, horizon, RENDER_W, RENDER_H - horizon);
        for (int i = 0; i < 7; i++) {
            final double z = 180.0 + i * 155.0;
            final double x = (i - 3.0) * 76.0 + Math.sin(time * 0.11 + i) * 34.0;
            final double radius = 80.0 + i * 16.0 + s.low * 10.0;
            final double rot = time * (0.04 + i * 0.005) + i * 0.43;
            drawRing(g, fx, camera, new Vec3(x, 92.0 + Math.sin(i) * 18.0, z), radius, rot, p.glow, 0.30, 4.0);
            drawRing(g, fx, camera, new Vec3(x, -82.0 - Math.sin(i) * 18.0, z), radius, rot, p.glow, 0.11, 2.0);
        }
        for (int i = 0; i < 5; i++) {
            final double x = (i - 2.0) * 130.0;
            final double z = 270.0 + i * 130.0;
            drawPyramid(g, fx, camera, new Vec3(x, 0, z), 100.0 + i * 16.0, 118.0 + i * 18.0,
                    p.solid, p.edge, 0.32 + s.barPulse * 0.11);
        }
    }

    private void drawMachine(final Graphics2D g, final Graphics2D fx, final Signals s, final Camera camera, final Palette p) {
        drawFloor(g, fx, camera, p, -280, 1_060, 0.15 + s.low * 0.10, false);
        drawRing(g, fx, camera, new Vec3(0, 92, 320), 242.0 + s.low * 18.0 + interactionEnergy * 5.0,
                time * 0.10, p.glow, 0.48 + s.beatPulse * 0.10, 5.0);
        drawRing(g, fx, camera, new Vec3(0, 92, 320), 178.0 + s.high * 14.0,
                -time * 0.17, p.edge, 0.42, 3.0);
        for (int i = 0; i < 58; i++) {
            final long objectSeed = seed + s.loop * 6151L + i * 97L;
            final double a = hash01(objectSeed) * Math.PI * 2.0 + time * (0.06 + hash01(objectSeed + 3L) * 0.04);
            final double r = 90.0 + Math.floor(hash01(objectSeed + 5L) * 4.0) * 48.0;
            final double x = Math.cos(a) * r;
            final double z = 320.0 + Math.sin(a) * r;
            final double y = 58.0 + (Math.floor(hash01(objectSeed + 7L) * 6.0) - 2.0) * 36.0
                    + Math.sin(time * 0.6 + i) * s.low * 10.0;
            final double size = 20.0 + hash01(objectSeed + 11L) * 42.0 + s.beatPulse * 3.0;
            drawWireCube(g, fx, camera, new Vec3(x, y, z), size, a + time * 0.13, p.edge,
                    0.22 + s.intensity * 0.34 + interactionEnergy * 0.018);
        }
        drawBox(g, fx, camera, new Vec3(0, 96, 320), 96, 192, 96, time * 0.05, p.solid, p.glow, 0.74);
    }

    private void drawTransformation(final Graphics2D g, final Graphics2D fx, final Signals s, final Camera camera, final Palette p) {
        drawFloor(g, fx, camera, p, -360, 1_420, 0.17 + s.release * 0.10, true);
        final double dissolve = smoothstep(0.08, 0.72, s.sectionProgress);
        for (int i = 0; i < 6; i++) {
            final double a = i * Math.PI * 2.0 / 6.0 + time * 0.035;
            final double r = 170.0 + Math.sin(time * 0.13 + i) * 38.0;
            final Vec3 center = new Vec3(Math.cos(a) * r, 62.0 + Math.sin(i * 1.7) * 32.0 + dissolve * 48.0,
                    380.0 + Math.sin(a) * 140.0);
            if ((i + openingVariant) % 3 == 0) {
                drawOrb(g, fx, camera, center, 46.0 + dissolve * 62.0 + s.high * 8.0, p.core, p.glow,
                        0.22 + dissolve * 0.32);
            } else if ((i + openingVariant) % 3 == 1) {
                drawPyramid(g, fx, camera, new Vec3(center.x, 0, center.z), 78.0 + dissolve * 34.0,
                        160.0 + dissolve * 150.0, p.solid, p.edge, 0.34 + dissolve * 0.25);
            } else {
                drawWireCube(g, fx, camera, center, 82.0 + dissolve * 44.0, a + time * 0.08, p.edge,
                        0.34 + s.high * 0.18);
            }
            drawLine3(g, fx, camera, new Vec3(0, 86.0 + dissolve * 42.0, 420.0), center, p.glow,
                    0.12 + s.phrasePulse * 0.14, 1.4 + s.high * 2.0);
        }
        drawRing(g, fx, camera, new Vec3(0, 108.0 + dissolve * 72.0, 420), 126.0 + dissolve * 180.0,
                -time * 0.055, p.glow, 0.26 + s.release * 0.22, 4.2);
        drawRing(g, fx, camera, new Vec3(0, 108.0 + dissolve * 72.0, 420), 260.0 + s.low * 24.0,
                time * 0.028, p.edge, 0.18 + dissolve * 0.16, 2.0);
        drawBeam(g, fx, camera, new Vec3(0, -34, 380), new Vec3(0, 340.0 + dissolve * 260.0, 520),
                18.0 + dissolve * 36.0, p.glow, 0.28 + dissolve * 0.36);
    }

    private void drawCity(final Graphics2D g, final Graphics2D fx, final Signals s, final Camera camera, final Palette p) {
        drawFloor(g, fx, camera, p, -520, 1_650, 0.22 + s.low * 0.08, true);
        for (int i = 0; i < 42; i++) {
            final long q = seed + sequenceVariant * 999L + i * 131L;
            final double side = hash01(q) < 0.5 ? -1.0 : 1.0;
            final double x = side * (110.0 + hash01(q + 1L) * 420.0);
            final double z = -170.0 + i * 52.0 + hash01(q + 2L) * 70.0;
            final double h = 70.0 + Math.pow(hash01(q + 3L), 0.45) * 360.0;
            final double w = 24.0 + hash01(q + 4L) * 64.0;
            drawBox(g, fx, camera, new Vec3(x, h * 0.5, z), w, h, w, hash01(q + 5L) * 0.12, p.solid, p.edge,
                    0.25 + s.intensity * 0.38);
            if ((i & 3) == 0) {
                drawLine3(g, fx, camera, new Vec3(x, h + 10.0, z), new Vec3(x * 0.18, h + 260.0, z + 140.0),
                        p.glow, 0.11 + s.barPulse * 0.12, 1.4 + s.low * 3.0);
            }
        }
        drawRing(g, fx, camera, new Vec3(0, 130, 760), 220.0 + s.low * 22.0, Math.PI * 0.5 + time * 0.02, p.glow, 0.30, 4.0);
    }

    private void drawLandscape(final Graphics2D g, final Graphics2D fx, final Signals s, final Camera camera, final Palette p) {
        drawFloor(g, fx, camera, p, -680, 1_720, 0.18 + s.high * 0.05, false);
        for (int i = 0; i < 26; i++) {
            final long q = seed + i * 271L + sequenceVariant * 79L;
            final double x = (hash01(q) - 0.5) * 880.0;
            final double z = -220.0 + i * 74.0 + hash01(q + 2L) * 120.0;
            final double w = 86.0 + hash01(q + 3L) * 180.0;
            final double h = 70.0 + hash01(q + 4L) * 250.0;
            if (hash01(q + 5L) < 0.62) {
                drawPyramid(g, fx, camera, new Vec3(x, 0, z), w, h, p.solid, p.edge, 0.26 + s.intensity * 0.23);
            } else {
                drawOrb(g, fx, camera, new Vec3(x, h * 0.7, z), w * 0.28, p.core, p.glow, 0.18 + s.release * 0.20);
            }
        }
        drawBeam(g, fx, camera, new Vec3(-260, 40, 520), new Vec3(260, 380, 760), 18.0 + s.low * 18.0, p.glow, 0.18 + s.phrasePulse * 0.12);
    }

    private void drawUniverse(final Graphics2D g, final Graphics2D fx, final Signals s, final Camera camera, final Palette p) {
        for (int i = 0; i < 12; i++) {
            final long q = seed + i * 307L + sequenceVariant * 41L;
            final double a = hash01(q) * Math.PI * 2.0 + time * (0.01 + hash01(q + 1L) * 0.018);
            final double r = 120.0 + hash01(q + 2L) * 520.0;
            final Vec3 c = new Vec3(Math.cos(a) * r, 40.0 + hash01(q + 3L) * 300.0, 300.0 + Math.sin(a) * r);
            drawOrb(g, fx, camera, c, 28.0 + hash01(q + 4L) * 95.0 + s.low * 5.0, p.core, p.glow, 0.20 + hash01(q + 5L) * 0.32);
            drawRing(g, fx, camera, c, 60.0 + hash01(q + 6L) * 150.0, a + Math.PI * 0.5, p.edge, 0.12 + s.high * 0.06, 1.8);
        }
        drawRing(g, fx, camera, new Vec3(0, 170, 520), 360.0 + s.release * 90.0, time * 0.015, p.glow, 0.34, 4.4);
    }

    private void drawOrganic(final Graphics2D g, final Graphics2D fx, final Signals s, final Camera camera, final Palette p) {
        drawFloor(g, fx, camera, p, -360, 1_260, 0.12 + s.low * 0.06, true);
        for (int i = 0; i < 7; i++) {
            final double a = i * Math.PI * 2.0 / 7.0 + time * 0.035;
            final Vec3 c = new Vec3(Math.cos(a) * 210.0, 88.0 + Math.sin(time * 0.23 + i) * 36.0, 420.0 + Math.sin(a) * 170.0);
            drawOrb(g, fx, camera, c, 34.0 + Math.sin(time * 0.7 + i) * 8.0, p.core, p.glow, 0.20 + s.intensity * 0.22);
            for (int j = 0; j < 5; j++) {
                final double b = a + (j - 2) * 0.42 + Math.sin(time * 0.3 + i) * 0.12;
                drawLine3(g, fx, camera, c, new Vec3(c.x + Math.cos(b) * (70.0 + j * 16.0), c.y + Math.sin(b * 1.7) * 44.0, c.z + Math.sin(b) * 100.0),
                        p.edge, 0.20 + s.high * 0.12, 2.0 + s.low * 1.8);
            }
        }
        drawRing(g, fx, camera, new Vec3(0, 98, 420), 260.0 + s.low * 28.0, time * 0.025, p.glow, 0.24, 3.2);
    }

    private void drawGears(final Graphics2D g, final Graphics2D fx, final Signals s, final Camera camera, final Palette p) {
        drawFloor(g, fx, camera, p, -300, 1_240, 0.16 + s.low * 0.07, false);
        for (int i = 0; i < 9; i++) {
            final long q = seed + i * 353L + sequenceVariant * 101L;
            final double x = (i - 4.0) * 82.0 + Math.sin(i) * 28.0;
            final double y = 84.0 + hash01(q) * 150.0;
            final double z = 240.0 + hash01(q + 1L) * 560.0;
            final double r = 52.0 + hash01(q + 2L) * 88.0;
            drawGear(g, fx, camera, new Vec3(x, y, z), r, 10 + (int) (hash01(q + 3L) * 10.0),
                    time * (0.05 + hash01(q + 4L) * 0.11) * (i % 2 == 0 ? 1.0 : -1.0), p.edge, p.glow, 0.25 + s.intensity * 0.30);
            drawLine3(g, fx, camera, new Vec3(0, 110, 420), new Vec3(x, y, z), p.glow, 0.06 + s.barPulse * 0.08, 1.2);
        }
        drawBeam(g, fx, camera, new Vec3(0, 0, 360), new Vec3(0, 330, 520), 24.0 + s.low * 22.0, p.glow, 0.25 + s.phrasePulse * 0.20);
    }

    private void drawAscension(final Graphics2D g, final Graphics2D fx, final Signals s, final Camera camera, final Palette p) {
        drawFloor(g, fx, camera, p, -320, 1_380, 0.22 + s.release * 0.11, true);
        final double rise = smoothstep(0.06, 0.88, s.sectionProgress);
        drawBeam(g, fx, camera, new Vec3(0, -22, 390), new Vec3(0, 620.0 + rise * 320.0, 550), 52.0 + rise * 46.0,
                p.glow, 0.62 + s.release * 0.28);
        for (int i = 0; i < 9; i++) {
            final double a = i * Math.PI * 2.0 / 9.0 + time * 0.018;
            final double r = 188.0 + Math.sin(time * 0.12 + i) * 18.0;
            final double x = Math.cos(a) * r;
            final double z = 390.0 + Math.sin(a) * r;
            final double h = 180.0 + rise * 320.0 + Math.sin(i * 2.0) * 40.0;
            drawBox(g, fx, camera, new Vec3(x, h * 0.5, z), 42, h, 42, a, p.solid, p.edge, 0.44 + rise * 0.33);
            drawRing(g, fx, camera, new Vec3(x * 0.5, 168.0 + rise * 190.0, 390.0 + (z - 390.0) * 0.5),
                    72.0 + rise * 58.0, a + Math.PI * 0.5, p.glow, 0.20 + rise * 0.25, 2.7);
        }
        drawRing(g, fx, camera, new Vec3(0, 320.0 + rise * 180.0, 500), 230.0 + rise * 90.0 + s.low * 16.0,
                time * 0.04, p.glow, 0.52, 4.0);
    }

    private void drawFloor(final Graphics2D g, final Graphics2D fx, final Camera camera, final Palette p,
                           final double startZ, final double endZ, final double alpha, final boolean reflective) {
        for (int i = 0; i < 24; i++) {
            final double z = startZ + (endZ - startZ) * i / 23.0;
            final double width = 520.0 + i * 12.0;
            drawLine3(g, fx, camera, new Vec3(-width, 0, z), new Vec3(width, 0, z), p.edge, alpha * 0.26, 1.0);
        }
        for (int i = -7; i <= 7; i++) {
            final double x = i * 76.0;
            drawLine3(g, fx, camera, new Vec3(x, 0, startZ), new Vec3(x, 0, endZ), p.edge, alpha * 0.28, 1.0);
        }
        if (reflective) {
            final Screen a = project(camera, new Vec3(-800, 0, startZ));
            final Screen b = project(camera, new Vec3(800, 0, startZ));
            final Screen c = project(camera, new Vec3(900, 0, endZ));
            final Screen d = project(camera, new Vec3(-900, 0, endZ));
            if (a.visible && b.visible && c.visible && d.visible) {
                final Path2D.Double path = new Path2D.Double();
                path.moveTo(a.x, a.y);
                path.lineTo(b.x, b.y);
                path.lineTo(c.x, c.y);
                path.lineTo(d.x, d.y);
                path.closePath();
                g.setColor(withAlpha(p.haze, 0.09));
                g.fill(path);
                fx.setColor(withAlpha(p.glow, 0.035));
                fx.fill(path);
            }
        }
    }

    private void drawParticles(final Graphics2D g, final Graphics2D fx, final Signals s, final Camera camera, final Palette p) {
        final double blend = s.sectionBlend;
        final int next = (s.section + 1) % SECTIONS;
        for (int i = 0; i < particles.length; i++) {
            final Particle particle = particles[i];
            final Vec3 a = particlePosition(particle, visualFamily(s.section), s.loop, s.sectionProgress, time);
            final Vec3 b = particlePosition(particle, visualFamily(next), s.loop + (next == 0 ? 1 : 0), 0.0, time);
            final Vec3 pos = a.mix(b, blend);
            final Screen sp = project(camera, pos);
            if (!sp.visible || sp.depth > FAR) {
                continue;
            }
            final double fog = 1.0 - clamp(sp.depth / FAR, 0.0, 1.0);
            final double twinkle = 0.45 + 0.55 * Math.sin(time * particle.speed + particle.phase + s.high * 4.0);
            final double alpha = clamp((0.08 + twinkle * 0.36 + s.high * 0.20 + interactionEnergy * 0.018) * fog, 0.0, 0.82);
            final int size = Math.max(1, (int) (particle.size * sp.scale * 0.75 + s.low * 1.4));
            final Color color = particle.hot > 0.62 ? p.glow : p.spark;
            g.setColor(withAlpha(color, alpha));
            g.fillOval((int) sp.x - size / 2, (int) sp.y - size / 2, size, size);
            if (alpha > 0.24 && (i & 3) == 0) {
                fx.setColor(withAlpha(color, alpha * 0.22));
                fx.fillOval((int) sp.x - size * 3, (int) sp.y - size * 3, size * 6, size * 6);
            }
        }
    }

    private Vec3 particlePosition(final Particle p, final int section, final int loop, final double progress, final double t) {
        final double loopShift = loop * 0.713 + p.phase;
        return switch (section) {
            case 0 -> {
                final double angle = p.a * Math.PI * 8.0 + t * (0.26 + p.hot * 0.16);
                final double radius = 28.0 + p.b * (330.0 - smoothstep(0.0, 0.7, progress) * 130.0);
                final double y = 42.0 + (p.c - 0.5) * 190.0 * (1.0 - progress * 0.35) + Math.sin(angle * 0.7) * 24.0;
                yield new Vec3(Math.cos(angle) * radius, y, 245.0 + Math.sin(angle) * radius * 0.72);
            }
            case 1 -> {
                final double x = (p.a - 0.5) * 980.0;
                final double z = wrap(p.c * 1_250.0 - t * (18.0 + p.hot * 22.0), -230.0, 1_020.0);
                final double y = 5.0 + Math.abs(Math.sin(t * p.speed + p.phase)) * (8.0 + p.hot * 38.0);
                yield new Vec3(x, y, z);
            }
            case 2 -> {
                final double z = wrap(p.c * 1_700.0 + t * (76.0 + p.hot * 72.0), -240.0, 1_460.0);
                final double x = (p.a - 0.5) * 310.0 + Math.sin(t * 0.8 + p.phase) * 8.0;
                final double y = 12.0 + p.b * 135.0;
                yield new Vec3(x, y, z);
            }
            case 3 -> {
                final double gx = Math.floor(p.a * 11.0) - 5.0;
                final double gy = Math.floor(p.b * 8.0) - 2.0;
                final double gz = Math.floor(p.c * 11.0) - 5.0;
                final double spin = t * 0.21 + loopShift;
                final double x0 = gx * 38.0 + Math.sin(t * 0.7 + p.phase) * 7.0;
                final double z0 = 320.0 + gz * 38.0 + Math.cos(t * 0.6 + p.phase) * 7.0;
                final double x = x0 * Math.cos(spin * 0.18) - (z0 - 320.0) * Math.sin(spin * 0.18);
                final double z = 320.0 + x0 * Math.sin(spin * 0.18) + (z0 - 320.0) * Math.cos(spin * 0.18);
                yield new Vec3(x, 76.0 + gy * 34.0, z);
            }
            case 4 -> {
                final double angle = p.a * Math.PI * 2.0 + t * 0.08 + loopShift;
                final double radius = 120.0 + p.b * 620.0;
                final double z = -190.0 + p.c * 1_170.0 + Math.sin(angle * 2.0) * 36.0;
                final double y = 8.0 + Math.pow(p.hot, 1.7) * 255.0 + Math.sin(t * 0.31 + p.phase) * 18.0;
                yield new Vec3(Math.cos(angle) * radius * 0.72, y, z);
            }
            case 5 -> {
                final double angle = p.a * Math.PI * 2.0 + t * (0.10 + p.hot * 0.06) + loopShift;
                final double shell = 80.0 + Math.floor(p.b * 4.0) * 62.0 + smoothstep(0.1, 0.8, progress) * 80.0;
                final double wave = Math.sin(t * 0.42 + p.phase) * (22.0 + p.hot * 36.0);
                yield new Vec3(Math.cos(angle) * shell, 60.0 + (p.c - 0.5) * 280.0 + wave,
                        420.0 + Math.sin(angle) * shell * 0.74);
            }
            case CITY -> {
                final double lane = Math.floor(p.a * 9.0) - 4.0;
                final double z = wrap(p.c * 1_900.0 + t * (38.0 + p.hot * 45.0), -300.0, 1_600.0);
                yield new Vec3(lane * 92.0 + Math.sin(p.phase) * 18.0, 12.0 + p.b * 300.0, z);
            }
            case LAND -> {
                final double z = wrap(p.c * 1_650.0 - t * (12.0 + p.hot * 20.0), -300.0, 1_350.0);
                final double x = (p.a - 0.5) * 1_000.0;
                yield new Vec3(x, 10.0 + Math.sin(t * 0.25 + p.phase) * 22.0 + p.b * 120.0, z);
            }
            case SPACE -> {
                final double a = p.a * Math.PI * 2.0 + t * (0.018 + p.hot * 0.025);
                final double r = 120.0 + p.b * 680.0;
                yield new Vec3(Math.cos(a) * r, 40.0 + p.c * 420.0, 430.0 + Math.sin(a) * r);
            }
            case ORGANIC -> {
                final double a = p.a * Math.PI * 2.0 + Math.sin(t * 0.16 + p.phase) * 0.8;
                final double r = 70.0 + p.b * 260.0 + Math.sin(t * 0.31 + p.phase) * 34.0;
                yield new Vec3(Math.cos(a) * r, 70.0 + (p.c - 0.5) * 260.0, 430.0 + Math.sin(a) * r * 0.8);
            }
            case GEARS -> {
                final double a = p.a * Math.PI * 2.0 + t * (0.08 + p.hot * 0.06);
                final double ring = 90.0 + Math.floor(p.b * 5.0) * 52.0;
                yield new Vec3(Math.cos(a) * ring, 80.0 + (p.c - 0.5) * 250.0, 420.0 + Math.sin(a) * ring);
            }
            default -> {
                final double angle = p.a * Math.PI * 2.0 + t * (0.08 + p.hot * 0.08);
                final double radius = 34.0 + p.b * 260.0 * (1.0 - smoothstep(0.2, 1.0, progress) * 0.35);
                final double y = wrap(p.c * 680.0 + t * (68.0 + p.hot * 90.0), 0.0, 680.0);
                yield new Vec3(Math.cos(angle) * radius, y, 390.0 + Math.sin(angle) * radius);
            }
        };
    }

    private void drawInteractionPulses(final Graphics2D g, final Graphics2D fx, final Signals s, final Camera camera, final Palette p) {
        for (final Pulse pulse : pulses) {
            final double age = time - pulse.born;
            if (age < 0.0 || age > 3.0) {
                continue;
            }
            final double fade = 1.0 - age / 3.0;
            final double radius = 32.0 + age * (165.0 + pulse.strength * 58.0);
            final double y = pulse.section == 5 ? 165.0 + age * 90.0 : 34.0 + pulse.strength * 20.0;
            final double z = switch (pulse.section) {
                case 1 -> 430.0 + age * 170.0;
                case 4 -> 320.0;
                case 5 -> 430.0 + age * 90.0;
                default -> 260.0 + pulse.loop * 0.0;
            };
            drawRing(g, fx, camera, new Vec3(0, y, z), radius, time * 0.08 + pulse.strength, p.glow,
                    fade * (0.30 + pulse.strength * 0.12), 3.2 + pulse.strength * 1.4);
        }
        if (tapPulse > 0.05) {
            g.setColor(withAlpha(p.glow, clamp(tapPulse * 0.030, 0.0, 0.16)));
            g.fillRect(0, 0, RENDER_W, RENDER_H);
        }
    }

    private void drawAtmosphere(final Graphics2D g, final Graphics2D fx, final Signals s, final Palette p) {
        final double haze = 0.06 + s.tension * 0.055 + interactionEnergy * 0.004;
        for (int i = 0; i < 6; i++) {
            final int y = (int) (RENDER_H * (0.34 + i * 0.095) + Math.sin(time * 0.09 + i) * 6.0);
            g.setColor(withAlpha(p.haze, haze * (1.0 - i * 0.08)));
            g.fillRect(0, y, RENDER_W, 18 + i * 7);
        }
        fx.setColor(withAlpha(p.glow, 0.025 + s.high * 0.020));
        fx.fillRect(0, 0, RENDER_W, RENDER_H);
    }

    private void drawPost(final Graphics2D out, final Signals s) {
        final double bloom = 0.20 + s.intensity * 0.22 + s.low * 0.16 + interactionEnergy * 0.018;
        out.setComposite(AlphaComposite.SrcOver.derive(1.0f));
        out.drawImage(scene, 0, 0, null);
        out.setComposite(AlphaComposite.SrcOver.derive((float) clamp(bloom * 0.36, 0.0, 0.55)));
        out.drawImage(glow, -2, 0, RENDER_W + 4, RENDER_H, null);
        out.drawImage(glow, 2, 0, RENDER_W - 4, RENDER_H, null);
        out.setComposite(AlphaComposite.SrcOver.derive((float) clamp(bloom * 0.42, 0.0, 0.62)));
        out.drawImage(glow, -6, -4, RENDER_W + 12, RENDER_H + 8, null);
        out.setComposite(AlphaComposite.SrcOver.derive((float) clamp(0.08 + s.high * 0.10 + interactionEnergy * 0.012, 0.0, 0.28)));
        out.setColor(new Color(255, 40, 70));
        out.drawImage(glow, -3, 0, null);
        out.setColor(new Color(40, 240, 255));
        out.drawImage(glow, 3, 0, null);
        out.setComposite(AlphaComposite.SrcOver);

        final Point2D center = new Point2D.Double(RENDER_W * 0.5, RENDER_H * 0.52);
        out.setPaint(new RadialGradientPaint(center, RENDER_W * 0.72f,
                new float[]{0.0f, 0.72f, 1.0f},
                new Color[]{withAlpha(BLACK, 0.0), withAlpha(BLACK, 0.0), withAlpha(BLACK, 0.72)}));
        out.fillRect(0, 0, RENDER_W, RENDER_H);

        final int bar = 22;
        out.setColor(withAlpha(BLACK, 0.88));
        out.fillRect(0, 0, RENDER_W, bar);
        out.fillRect(0, RENDER_H - bar, RENDER_W, bar);

        if (s.barPulse > 0.55 || tapPulse > 0.28) {
            out.setColor(withAlpha(Color.WHITE, clamp((s.barPulse - 0.55) * 0.08 + tapPulse * 0.025, 0.0, 0.09)));
            out.fillRect(0, 0, RENDER_W, RENDER_H);
        }
    }

    private void drawBox(final Graphics2D g, final Graphics2D fx, final Camera camera, final Vec3 center,
                         final double sx, final double sy, final double sz, final double rotation,
                         final Color fill, final Color edge, final double alpha) {
        final Vec3[] corners = boxCorners(center, sx, sy, sz, rotation);
        final int[][] faces = {
                {0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1},
                {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}
        };
        for (int f = 0; f < faces.length; f++) {
            final Screen a = project(camera, corners[faces[f][0]]);
            final Screen b = project(camera, corners[faces[f][1]]);
            final Screen c = project(camera, corners[faces[f][2]]);
            final Screen d = project(camera, corners[faces[f][3]]);
            if (!(a.visible && b.visible && c.visible && d.visible)) {
                continue;
            }
            final double depth = (a.depth + b.depth + c.depth + d.depth) * 0.25;
            final double fog = 1.0 - clamp(depth / FAR, 0.0, 1.0);
            final Path2D.Double path = new Path2D.Double();
            path.moveTo(a.x, a.y);
            path.lineTo(b.x, b.y);
            path.lineTo(c.x, c.y);
            path.lineTo(d.x, d.y);
            path.closePath();
            final double shade = 0.32 + f * 0.055;
            g.setColor(mix(withAlpha(fill, alpha * fog * 0.58), withAlpha(edge, alpha * fog * 0.32), shade));
            g.fill(path);
            g.setStroke(new BasicStroke(1.0f));
            g.setColor(withAlpha(edge, alpha * fog * 0.70));
            g.draw(path);
            fx.setColor(withAlpha(edge, alpha * fog * 0.05));
            fx.fill(path);
        }
    }

    private Vec3[] boxCorners(final Vec3 c, final double sx, final double sy, final double sz, final double rotation) {
        final double hx = sx * 0.5;
        final double hy = sy * 0.5;
        final double hz = sz * 0.5;
        final double cr = Math.cos(rotation);
        final double sr = Math.sin(rotation);
        final Vec3[] out = new Vec3[8];
        int i = 0;
        for (int y = -1; y <= 1; y += 2) {
            for (int z = -1; z <= 1; z += 2) {
                for (int x = -1; x <= 1; x += 2) {
                    final double px = x * hx;
                    final double pz = z * hz;
                    out[i++] = new Vec3(c.x + px * cr - pz * sr, c.y + y * hy, c.z + px * sr + pz * cr);
                }
            }
        }
        return out;
    }

    private void drawWireCube(final Graphics2D g, final Graphics2D fx, final Camera camera, final Vec3 center,
                              final double size, final double rotation, final Color color, final double alpha) {
        final Vec3[] c = boxCorners(center, size, size, size, rotation);
        final int[][] edges = {
                {0, 1}, {0, 2}, {1, 3}, {2, 3}, {4, 5}, {4, 6}, {5, 7}, {6, 7},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        for (final int[] edge : edges) {
            drawLine3(g, fx, camera, c[edge[0]], c[edge[1]], color, alpha, 1.6);
        }
    }

    private void drawPyramid(final Graphics2D g, final Graphics2D fx, final Camera camera, final Vec3 base,
                             final double width, final double height, final Color fill, final Color edge, final double alpha) {
        final Vec3 p0 = new Vec3(base.x - width * 0.5, base.y, base.z - width * 0.5);
        final Vec3 p1 = new Vec3(base.x + width * 0.5, base.y, base.z - width * 0.5);
        final Vec3 p2 = new Vec3(base.x + width * 0.5, base.y, base.z + width * 0.5);
        final Vec3 p3 = new Vec3(base.x - width * 0.5, base.y, base.z + width * 0.5);
        final Vec3 top = new Vec3(base.x, base.y + height, base.z);
        drawTriangle(g, fx, camera, p0, p1, top, fill, edge, alpha);
        drawTriangle(g, fx, camera, p1, p2, top, fill, edge, alpha * 0.9);
        drawTriangle(g, fx, camera, p2, p3, top, fill, edge, alpha * 0.8);
        drawTriangle(g, fx, camera, p3, p0, top, fill, edge, alpha * 0.7);
    }

    private void drawTriangle(final Graphics2D g, final Graphics2D fx, final Camera camera, final Vec3 a3,
                              final Vec3 b3, final Vec3 c3, final Color fill, final Color edge, final double alpha) {
        final Screen a = project(camera, a3);
        final Screen b = project(camera, b3);
        final Screen c = project(camera, c3);
        if (!(a.visible && b.visible && c.visible)) {
            return;
        }
        final double fog = 1.0 - clamp((a.depth + b.depth + c.depth) / (3.0 * FAR), 0.0, 1.0);
        final Path2D.Double path = new Path2D.Double();
        path.moveTo(a.x, a.y);
        path.lineTo(b.x, b.y);
        path.lineTo(c.x, c.y);
        path.closePath();
        g.setColor(withAlpha(fill, alpha * fog * 0.46));
        g.fill(path);
        g.setColor(withAlpha(edge, alpha * fog * 0.78));
        g.draw(path);
        fx.setColor(withAlpha(edge, alpha * fog * 0.05));
        fx.fill(path);
    }

    private void drawRing(final Graphics2D g, final Graphics2D fx, final Camera camera, final Vec3 center,
                          final double radius, final double rotation, final Color color, final double alpha, final double width) {
        final int segments = 96;
        Screen previous = null;
        for (int i = 0; i <= segments; i++) {
            final double a = i * Math.PI * 2.0 / segments;
            final double x0 = Math.cos(a) * radius;
            final double y = Math.sin(a) * radius;
            final double x = x0 * Math.cos(rotation);
            final double z = center.z + x0 * Math.sin(rotation);
            final Screen current = project(camera, new Vec3(center.x + x, center.y + y, z));
            if (previous != null && previous.visible && current.visible) {
                final double fog = 1.0 - clamp((previous.depth + current.depth) / (2.0 * FAR), 0.0, 1.0);
                drawLine2(g, fx, previous.x, previous.y, current.x, current.y, color, alpha * fog, width);
            }
            previous = current;
        }
    }

    private void drawBeam(final Graphics2D g, final Graphics2D fx, final Camera camera, final Vec3 a3, final Vec3 b3,
                          final double width, final Color color, final double alpha) {
        final Screen a = project(camera, a3);
        final Screen b = project(camera, b3);
        if (!(a.visible && b.visible)) {
            return;
        }
        drawLine2(g, fx, a.x, a.y, b.x, b.y, color, alpha, width);
        drawLine2(g, fx, a.x, a.y, b.x, b.y, color, alpha * 0.24, width * 4.0);
    }

    private void drawLine3(final Graphics2D g, final Graphics2D fx, final Camera camera, final Vec3 a3, final Vec3 b3,
                           final Color color, final double alpha, final double width) {
        final Screen a = project(camera, a3);
        final Screen b = project(camera, b3);
        if (!(a.visible && b.visible)) {
            return;
        }
        final double fog = 1.0 - clamp((a.depth + b.depth) / (2.0 * FAR), 0.0, 1.0);
        drawLine2(g, fx, a.x, a.y, b.x, b.y, color, alpha * fog, width);
    }

    private void drawLine2(final Graphics2D g, final Graphics2D fx, final double ax, final double ay, final double bx,
                           final double by, final Color color, final double alpha, final double width) {
        if (alpha <= 0.0) {
            return;
        }
        final float line = (float) Math.max(0.8, width);
        final int split = (int) clamp(1.0 + interactionEnergy * 0.6 + tapPulse * 0.8, 1.0, 5.0);
        fx.setStroke(new BasicStroke(line * 4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        fx.setColor(withAlpha(color, alpha * 0.12));
        fx.drawLine((int) ax, (int) ay, (int) bx, (int) by);
        g.setStroke(new BasicStroke(line, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(new Color(255, 55, 90), alpha * 0.22));
        g.drawLine((int) ax - split, (int) ay, (int) bx - split, (int) by);
        g.setColor(withAlpha(new Color(40, 235, 255), alpha * 0.22));
        g.drawLine((int) ax + split, (int) ay, (int) bx + split, (int) by);
        g.setColor(withAlpha(color, alpha));
        g.drawLine((int) ax, (int) ay, (int) bx, (int) by);
    }

    private void drawOrb(final Graphics2D g, final Graphics2D fx, final Camera camera, final Vec3 center,
                         final double radius, final Color fill, final Color glowColor, final double alpha) {
        final Screen sp = project(camera, center);
        if (!sp.visible) {
            return;
        }
        final double fog = 1.0 - clamp(sp.depth / FAR, 0.0, 1.0);
        final int size = (int) Math.max(2.0, radius * sp.scale * 2.0);
        final int x = (int) sp.x - size / 2;
        final int y = (int) sp.y - size / 2;
        fx.setColor(withAlpha(glowColor, alpha * fog * 0.24));
        fx.fillOval(x - size / 2, y - size / 2, size * 2, size * 2);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(sp.x - size * 0.16, sp.y - size * 0.18), size * 0.68f,
                new float[]{0.0f, 0.62f, 1.0f},
                new Color[]{withAlpha(glowColor, alpha * fog * 0.68), withAlpha(fill, alpha * fog * 0.42), withAlpha(BLACK, alpha * fog * 0.08)}));
        g.fillOval(x, y, size, size);
        g.setColor(withAlpha(glowColor, alpha * fog * 0.55));
        g.setStroke(new BasicStroke(Math.max(1.0f, (float) (sp.scale * 2.0))));
        g.drawOval(x, y, size, size);
        drawRing(g, fx, camera, center, radius * 1.25, time * 0.04, glowColor, alpha * 0.20, 1.5);
    }

    private void drawGear(final Graphics2D g, final Graphics2D fx, final Camera camera, final Vec3 center,
                          final double radius, final int teeth, final double rotation, final Color edge, final Color glowColor, final double alpha) {
        final int n = teeth * 2;
        Screen first = null;
        Screen previous = null;
        for (int i = 0; i <= n; i++) {
            final double a = rotation + i * Math.PI * 2.0 / n;
            final double r = radius * (i % 2 == 0 ? 1.18 : 0.88);
            final Screen current = project(camera, new Vec3(center.x + Math.cos(a) * r, center.y + Math.sin(a) * r, center.z + Math.sin(a * 0.5) * radius * 0.10));
            if (i == 0) {
                first = current;
            }
            if (previous != null && previous.visible && current.visible) {
                drawLine2(g, fx, previous.x, previous.y, current.x, current.y, edge, alpha, 2.0);
            }
            previous = current;
        }
        if (first != null && previous != null && first.visible && previous.visible) {
            drawLine2(g, fx, previous.x, previous.y, first.x, first.y, edge, alpha, 2.0);
        }
        drawRing(g, fx, camera, center, radius * 0.46, rotation * -0.7, glowColor, alpha * 0.55, 2.4);
        drawRing(g, fx, camera, center, radius * 0.72, rotation * 0.4, edge, alpha * 0.34, 1.4);
    }

    private Camera cameraAt(final Signals s) {
        final double p = s.sectionProgress;
        final double pulse = s.low * 0.018 + interactionEnergy * 0.003;
        final Vec3 pos;
        final Vec3 target;
        final double fov;
        final double centerY;
        switch (visualFamily(s.section)) {
            case BIRTH -> {
                final double a = -0.65 + p * 1.15 + Math.sin(time * 0.08) * 0.08;
                pos = new Vec3(Math.sin(a) * 245.0, 82.0 + Math.sin(time * 0.13) * 24.0, -215.0 + p * 120.0);
                target = new Vec3(0, 58, 245.0);
                fov = 1.03 - p * 0.10;
                centerY = RENDER_H * 0.53;
            }
            case FLUID -> {
                pos = new Vec3(-430.0 + p * 820.0 + cameraVariant * 12.0, 48.0 + Math.sin(time * 0.10) * 12.0,
                        -210.0 + Math.sin(p * Math.PI) * 170.0);
                target = new Vec3(Math.sin(time * 0.07) * 72.0, 68.0 + s.low * 10.0, 470.0);
                fov = 1.00 + s.low * 0.025;
                centerY = RENDER_H * 0.56;
            }
            case CORRIDOR -> {
                final double z = -260.0 + p * 860.0;
                pos = new Vec3(Math.sin(time * 0.17 + cameraVariant) * 30.0, 48.0 + Math.sin(time * 0.23) * 11.0, z);
                target = new Vec3(Math.sin(time * 0.11) * 16.0, 64.0 + s.low * 14.0, z + 390.0);
                fov = 0.98 + s.low * 0.05;
                centerY = RENDER_H * 0.55;
            }
            case MACHINE -> {
                final double a = p * Math.PI * 2.35 + time * 0.10 + cameraVariant * 0.33;
                pos = new Vec3(Math.cos(a) * 380.0, 112.0 + Math.sin(a * 1.7) * 56.0, 320.0 + Math.sin(a) * 330.0);
                target = new Vec3(0, 96.0 + s.low * 28.0, 320.0);
                fov = 0.92 + s.low * 0.09;
                centerY = RENDER_H * 0.54;
            }
            case WORLD -> {
                final double a = 2.55 - p * (1.05 + cameraVariant * 0.035);
                pos = new Vec3(Math.cos(a) * 520.0, 118.0 + Math.sin(p * Math.PI) * 84.0, -140.0 + Math.sin(a) * 380.0);
                target = new Vec3(20.0, 92.0 + s.release * 18.0, 430.0);
                fov = 1.08 - p * 0.16;
                centerY = RENDER_H * 0.58;
            }
            case TRANSFORM -> {
                final double a = -0.95 + p * 1.95 + Math.sin(time * 0.07) * 0.08;
                pos = new Vec3(Math.sin(a) * 420.0, 84.0 + p * 115.0, -150.0 + Math.cos(a) * 160.0);
                target = new Vec3(0, 130.0 + p * 90.0, 430.0);
                fov = 1.02 - p * 0.08 + s.high * 0.02;
                centerY = RENDER_H * (0.56 - p * 0.04);
            }
            case CITY -> {
                final double z = -360.0 + p * 1_050.0;
                pos = new Vec3(Math.sin(time * 0.09 + cameraVariant) * 150.0, 75.0 + p * 90.0, z);
                target = new Vec3(0, 150.0 + s.low * 22.0, z + 560.0);
                fov = 0.94 + s.low * 0.05;
                centerY = RENDER_H * 0.56;
            }
            case LAND -> {
                final double a = 2.8 - p * 1.4;
                pos = new Vec3(Math.cos(a) * 660.0, 130.0 + Math.sin(p * Math.PI) * 120.0, -150.0 + Math.sin(a) * 520.0);
                target = new Vec3(0, 80.0 + s.release * 60.0, 540.0);
                fov = 1.10 - p * 0.13;
                centerY = RENDER_H * 0.60;
            }
            case SPACE -> {
                final double a = -1.8 + p * 2.6 + time * 0.018;
                pos = new Vec3(Math.cos(a) * 540.0, 170.0 + Math.sin(a * 1.3) * 130.0, 260.0 + Math.sin(a) * 520.0);
                target = new Vec3(0, 160.0, 520.0);
                fov = 1.15 - s.release * 0.18;
                centerY = RENDER_H * 0.52;
            }
            case ORGANIC -> {
                final double a = p * Math.PI * 1.7 + time * 0.035;
                pos = new Vec3(Math.cos(a) * 390.0, 95.0 + Math.sin(time * 0.2) * 45.0, 330.0 + Math.sin(a) * 300.0);
                target = new Vec3(0, 96.0 + s.low * 35.0, 420.0);
                fov = 1.02 + s.high * 0.03;
                centerY = RENDER_H * 0.55;
            }
            case GEARS -> {
                final double a = -1.1 + p * 2.4 + time * 0.03;
                pos = new Vec3(Math.cos(a) * 430.0, 110.0 + Math.sin(a * 1.5) * 70.0, 300.0 + Math.sin(a) * 360.0);
                target = new Vec3(0, 135.0 + s.low * 28.0, 470.0);
                fov = 0.96 + s.low * 0.05;
                centerY = RENDER_H * 0.54;
            }
            default -> {
                final double rise = smoothstep(0.05, 0.92, p);
                pos = new Vec3(Math.sin(time * 0.07) * 215.0, 46.0 + rise * 230.0, -285.0 + rise * 420.0);
                target = new Vec3(0, 190.0 + rise * 260.0, 470.0);
                fov = 1.05 - rise * 0.22 + s.low * 0.03;
                centerY = RENDER_H * (0.58 - rise * 0.11);
            }
        }

        final Vec3 influencedTarget = target.add(new Vec3(nudgeX * 82.0, -nudgeY * 56.0, 0.0));
        final Vec3 influencedPos = pos.add(new Vec3(nudgeX * 28.0, -nudgeY * 16.0 + Math.sin(breathing) * pulse * 120.0, 0.0));
        final Vec3 forward = influencedTarget.subtract(influencedPos).normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        if (right.length() < 0.001) {
            right = new Vec3(1, 0, 0);
        }
        final Vec3 up = right.cross(forward).normalize();
        final double roll = nudgeX * 0.045 + Math.sin(time * 0.19) * 0.010 + tapPulse * 0.004;
        final double focal = (RENDER_H * 0.5) / Math.tan(fov * 0.5);
        return new Camera(influencedPos, right, up, forward, roll, focal, centerY);
    }

    private Screen project(final Camera camera, final Vec3 point) {
        final Vec3 v = point.subtract(camera.position);
        final double x = v.dot(camera.right);
        final double y = v.dot(camera.up);
        final double z = v.dot(camera.forward);
        if (z < 2.0) {
            return Screen.HIDDEN;
        }
        final double sx = x * camera.focal / z;
        final double sy = y * camera.focal / z;
        final double cr = Math.cos(camera.roll);
        final double sr = Math.sin(camera.roll);
        final double rx = sx * cr - sy * sr;
        final double ry = sx * sr + sy * cr;
        final double px = RENDER_W * 0.5 + rx;
        final double py = camera.centerY - ry;
        if (px < -140.0 || px > RENDER_W + 140.0 || py < -140.0 || py > RENDER_H + 140.0) {
            return new Screen(px, py, z, camera.focal / z, false);
        }
        return new Screen(px, py, z, camera.focal / z, true);
    }

    private Palette paletteAt(final Signals s) {
        final Palette a = palette(visualFamily(s.section));
        final Palette b = palette(visualFamily((s.section + 1) % SECTIONS));
        final double blend = s.sectionBlend * 0.78;
        return new Palette(
                mix(a.skyTop, b.skyTop, blend),
                mix(a.skyBottom, b.skyBottom, blend),
                mix(a.solid, b.solid, blend),
                mix(a.edge, b.edge, blend),
                mix(a.glow, b.glow, blend),
                mix(a.haze, b.haze, blend),
                mix(a.core, b.core, blend),
                mix(a.spark, b.spark, blend)
        );
    }

    private Palette palette(final int section) {
        return switch (section) {
            case 0 -> new Palette(
                    new Color(2, 3, 10), new Color(11, 8, 24), new Color(13, 16, 28),
                    new Color(92, 210, 255), new Color(130, 240, 255), new Color(55, 50, 95),
                    new Color(155, 210, 255), new Color(230, 250, 255));
            case 1 -> new Palette(
                    new Color(2, 7, 12), new Color(7, 18, 22), new Color(10, 20, 25),
                    new Color(185, 210, 255), new Color(95, 215, 255), new Color(55, 78, 95),
                    new Color(220, 245, 255), new Color(245, 255, 255));
            case 2 -> new Palette(
                    new Color(5, 5, 8), new Color(14, 18, 22), new Color(18, 22, 27),
                    new Color(255, 78, 98), new Color(255, 135, 78), new Color(80, 38, 48),
                    new Color(255, 210, 145), new Color(255, 220, 185));
            case 3 -> new Palette(
                    new Color(8, 5, 11), new Color(17, 11, 22), new Color(21, 18, 28),
                    new Color(236, 92, 255), new Color(255, 70, 178), new Color(88, 50, 100),
                    new Color(255, 210, 255), new Color(255, 220, 245));
            case 4 -> new Palette(
                    new Color(4, 7, 10), new Color(14, 18, 18), new Color(18, 21, 21),
                    new Color(150, 220, 170), new Color(105, 255, 195), new Color(56, 76, 63),
                    new Color(215, 255, 196), new Color(210, 255, 220));
            case 5 -> new Palette(
                    new Color(4, 6, 12), new Color(13, 14, 21), new Color(18, 19, 27),
                    new Color(118, 255, 216), new Color(190, 140, 255), new Color(62, 70, 88),
                    new Color(210, 245, 255), new Color(238, 255, 246));
            case 6 -> new Palette(
                    new Color(5, 7, 9), new Color(18, 17, 14), new Color(18, 20, 22),
                    new Color(255, 232, 150), new Color(255, 245, 185), new Color(95, 84, 54),
                    new Color(255, 255, 232), new Color(255, 255, 245));
            case CITY -> new Palette(
                    new Color(3, 5, 9), new Color(10, 13, 18), new Color(13, 17, 23),
                    new Color(255, 180, 92), new Color(110, 220, 255), new Color(80, 62, 70),
                    new Color(255, 224, 170), new Color(220, 245, 255));
            case LAND -> new Palette(
                    new Color(3, 9, 10), new Color(12, 18, 15), new Color(18, 25, 22),
                    new Color(135, 255, 180), new Color(255, 220, 118), new Color(58, 82, 66),
                    new Color(210, 255, 188), new Color(245, 255, 215));
            case SPACE -> new Palette(
                    new Color(1, 2, 8), new Color(8, 7, 17), new Color(12, 13, 25),
                    new Color(145, 155, 255), new Color(255, 105, 205), new Color(48, 46, 88),
                    new Color(210, 220, 255), new Color(255, 225, 255));
            case GEARS -> new Palette(
                    new Color(5, 5, 6), new Color(17, 14, 13), new Color(23, 21, 20),
                    new Color(255, 164, 88), new Color(255, 220, 130), new Color(86, 62, 50),
                    new Color(255, 224, 180), new Color(255, 238, 210));
            default -> new Palette(
                    new Color(4, 5, 7), new Color(13, 16, 15), new Color(15, 20, 19),
                    new Color(120, 245, 205), new Color(205, 255, 160), new Color(58, 76, 70),
                    new Color(225, 255, 210), new Color(240, 255, 230));
        };
    }

    private Signals signalsAt(final double t) {
        final double beat = t / beatSeconds;
        final int loop = (int) Math.floor(beat / cycleBeats);
        final double cycleBeat = beat - loop * cycleBeats;
        int section = 0;
        double sectionStart = 0.0;
        double sectionBeats = sectionBeatLength(0);
        for (int i = 0; i < SECTIONS; i++) {
            sectionBeats = sectionBeatLength(i);
            if (cycleBeat < sectionStart + sectionBeats || i == SECTIONS - 1) {
                section = i;
                break;
            }
            sectionStart += sectionBeats;
        }
        final double sectionBeat = cycleBeat - sectionStart;
        final double sectionTime = sectionBeat * beatSeconds;
        final double sectionProgress = clamp01(sectionBeat / sectionBeats);
        final double beatPhase = beat - Math.floor(beat);
        final double bar = beat / 4.0;
        final double barPhase = bar - Math.floor(bar);
        final double sixteenth = beat * 4.0;
        final int step = (int) Math.floor(sixteenth) & 15;
        final int grooveStep = (step + beatShift) & 15;
        final double stepPhase = sixteenth - Math.floor(sixteenth);
        final int role = musicRole(section);
        final double sectionBase = SECTION_INTENSITY[role];
        final double drive = SECTION_DRIVE[role] * smoothstep(0.04, 0.90, sectionProgress);
        final double brightness = SECTION_BRIGHTNESS[role] * (0.72 + smoothstep(0.25, 0.92, sectionProgress) * 0.34);
        final double groove = drive * (0.62 + 0.38 * hit(kickMask(role), grooveStep));
        final double beatPulse = Math.exp(-beatPhase * (8.0 + sectionBase * 8.0));
        final double barPulse = Math.exp(-barPhase * 7.0);
        final double phrasePulse = Math.exp(-((bar / 4.0) - Math.floor(bar / 4.0)) * 5.0);
        final double lift = smoothstep(0.10, 0.82, sectionProgress);
        final double release = role == RELEASE || role == FINALE ? smoothstep(0.18, 0.92, sectionProgress) : smoothstep(0.84, 1.0, sectionProgress) * 0.25;
        final double low = beatPulse * (0.10 + drive * 0.82)
                + barPulse * (section >= 2 ? 0.16 + sectionBase * 0.16 : 0.04);
        final double high = brightness * (0.34 + 0.66 * Math.sin(t * (0.47 + section * 0.035) + section))
                + phrasePulse * 0.22;
        final double tension = clamp01(sectionBase * 0.68 + lift * 0.34 + (role == BUILD ? 0.18 : 0.0)
                + (role == DROP ? 0.28 : 0.0) - release * 0.30);
        final double intensity = clamp01(sectionBase * (0.64 + lift * 0.48) + beatPulse * 0.12 + groove * 0.08);
        final double sectionBlend = smoothstep(0.86, 1.0, sectionProgress);
        return new Signals(section, loop, sectionTime, sectionProgress, beatPhase, barPhase, beatPulse,
                barPulse, phrasePulse, clamp01(low), clamp01(high), tension, intensity, release, sectionBlend,
                clamp01(drive), clamp01(brightness), clamp01(groove), step, stepPhase);
    }

    private void runAudio() {
        final AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);
        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format, 16_384);
            line.start();
            final byte[] buffer = new byte[4_096];
            long sample = 0L;
            while (running.get()) {
                for (int i = 0; i < buffer.length; i += 4) {
                    final double t = sample / (double) SAMPLE_RATE;
                    final double control = Double.longBitsToDouble(interactionBits.get());
                    final Sample value = synth(t, signalsAt(t), control);
                    final short left = (short) (clamp(value.left, -1.0, 1.0) * 32767.0);
                    final short right = (short) (clamp(value.right, -1.0, 1.0) * 32767.0);
                    buffer[i] = (byte) (left & 0xff);
                    buffer[i + 1] = (byte) ((left >>> 8) & 0xff);
                    buffer[i + 2] = (byte) (right & 0xff);
                    buffer[i + 3] = (byte) ((right >>> 8) & 0xff);
                    sample++;
                }
                line.write(buffer, 0, buffer.length);
            }
            line.drain();
        } catch (final Exception ignored) {
            while (running.get()) {
                try {
                    Thread.sleep(250L);
                } catch (final InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    running.set(false);
                }
            }
        }
    }

    private Sample synth(final double t, final Signals s, final double control) {
        final int role = musicRole(s.section);
        final int[] progression = PROGRESSIONS[harmonyVariant];
        final int phrase = (int) Math.floor((t / beatSeconds) / 8.0);
        final int chord = progression[Math.floorMod(phrase, progression.length)];
        final int nextChord = progression[Math.floorMod(phrase + 1, progression.length)];
        final double phrasePhase = ((t / beatSeconds) / 8.0) - Math.floor((t / beatSeconds) / 8.0);
        final double chordMorph = smoothstep(0.72, 1.0, phrasePhase);
        final double sectionRise = smoothstep(0.05, 0.92, s.sectionProgress);
        final double sectionFall = 1.0 - smoothstep(0.82, 1.0, s.sectionProgress);
        final double stepEnv = Math.exp(-s.stepPhase * 8.0);
        final double stepAge = s.stepPhase * beatSeconds * 0.25;
        final double attack = smoothstep(0.0, 0.08, s.stepPhase);
        final int grooveStep = (s.step + beatShift) & 15;
        final double kickHit = hit(kickMask(role), grooveStep);
        final double kickEnv = kickHit * Math.exp(-s.stepPhase * (5.5 + s.drive * 6.0));
        final double snareEnv = hit(snareMask(role), grooveStep) * Math.exp(-s.stepPhase * 12.0) * attack;
        final double hatEnv = hit(hatMask(role), grooveStep) * Math.exp(-s.stepPhase * (14.0 + s.brightness * 8.0)) * attack;
        final double duck = 1.0 - kickEnv * (0.34 + s.drive * 0.22);
        final double bassClock = (t / beatSeconds) * 2.0;
        final int bassIndex = (int) Math.floor(bassClock);
        final double bassPhase = bassClock - bassIndex;
        final int bassStep = BASS_PATTERNS[bassVariant][Math.floorMod(bassIndex, 16)];
        final int nextBassStep = BASS_PATTERNS[bassVariant][Math.floorMod(bassIndex + 1, 16)];
        final double bassBlend = smoothstep(0.74, 1.0, bassPhase);
        final int leadStep = LEAD_PATTERNS[leadVariant][Math.floorMod((int) Math.floor((t / beatSeconds) * 4.0), 16)];

        final double padGain = switch (role) {
            case 0 -> 0.34 + sectionRise * 0.16;
            case 1 -> 0.24;
            case 2 -> 0.28 + sectionRise * 0.10;
            case 3 -> 0.18;
            case 4 -> 0.31;
            case 5 -> 0.42 * sectionFall + 0.14;
            default -> 0.34 + s.release * 0.18;
        };
        final double subGain = switch (role) {
            case 0 -> 0.04 + sectionRise * 0.05;
            case 1 -> 0.24 + sectionRise * 0.10;
            case 2 -> 0.32 + sectionRise * 0.18;
            case 3 -> 0.72 + kickEnv * 0.10;
            case 4 -> 0.54;
            case 5 -> 0.16 * sectionFall;
            default -> 0.48 + sectionRise * 0.14;
        };
        final double leadGain = switch (role) {
            case 0 -> 0.00;
            case 1 -> 0.03 + sectionRise * 0.04;
            case 2 -> 0.06 + sectionRise * 0.07;
            case 3 -> 0.12 + s.groove * 0.08;
            case 4 -> 0.10 + sectionRise * 0.06;
            case 5 -> 0.05 * sectionFall;
            default -> 0.14 + s.release * 0.08;
        };
        final double percGain = 0.12 + s.drive * 0.36;
        final double chordRoot = rootNote + chord + (nextChord - chord) * chordMorph * 0.12;
        final Sample pad = padCloud(t, chordRoot, s.tension, padGain * duck, 0.42 + s.brightness * 0.28);
        final double subFreq = note(rootNote - 24 + chord + bassStep);
        final double nextSubFreq = note(rootNote - 24 + chord + nextBassStep);
        final double subA = Math.sin(Math.PI * 2.0 * subFreq * t + Math.sin(t * 1.7) * 0.04);
        final double subB = Math.sin(Math.PI * 2.0 * nextSubFreq * t + Math.sin(t * 1.7) * 0.04);
        final double sub = (subA + (subB - subA) * bassBlend)
                * subGain * (0.72 + stepEnv * 0.28) * duck;
        final double reeseA = softSaw(subFreq * 1.005 * t) - softSaw(subFreq * 0.497 * t + 0.31);
        final double reeseB = softSaw(nextSubFreq * 1.005 * t) - softSaw(nextSubFreq * 0.497 * t + 0.31);
        final double reese = (reeseA + (reeseB - reeseA) * bassBlend) * subGain * 0.31 * s.drive * duck;
        final double kickAge = stepAge;
        final double kick = kickDrum(kickAge, kickHit, s.drive);
        final double punch = kick + Math.tanh(kick * 2.8) * 0.28;
        final double clapNoise = bandNoise(t, 11_000.0, 0.31) * snareEnv * percGain * (0.08 + s.brightness * 0.10);
        final double snap = Math.sin(Math.PI * 2.0 * 188.0 * stepAge) * snareEnv * percGain * 0.20;
        final double hats = metallicHat(t, chord) * hatEnv * (0.018 + s.brightness * 0.038 + control * 0.003)
                + bandNoise(t, 21_000.0, 0.91) * hatEnv * (0.003 + s.brightness * 0.006);
        final double tick = (hit(hatMask(role), (grooveStep + 2) & 15) * Math.exp(-s.stepPhase * 18.0) * attack)
                * polishedTone(note(rootNote + chord + 31) * t, s.brightness, 0.20) * s.brightness * 0.014;
        final double arpPhase = ((t / beatSeconds) * (role == DROP || role == FINALE ? 4.0 : 2.0));
        final double arpEnv = Math.exp(-(arpPhase - Math.floor(arpPhase)) * (5.0 + s.drive * 3.0));
        final double arp = polishedTone(note(rootNote + chord + leadStep) * t, s.brightness, 0.52)
                * arpEnv * leadGain;
        final double longLeadEnv = smoothstep(0.18, 0.58, s.sectionProgress) * (1.0 - smoothstep(0.90, 1.0, s.sectionProgress));
        final double lead = Math.sin(Math.PI * 2.0 * note(rootNote + chord + 24 + (leadStep % 12)) * t
                + Math.sin(t * 0.44) * 0.18) * leadGain * 0.62 * longLeadEnv;
        final double riser = transitionSweep(t, s, chord) * smoothstep(0.62, 0.98, s.sectionProgress)
                * (role == BUILD || role == RELEASE ? 0.042 : 0.010);
        final double shimmer = metallicShimmer(t, chord) * (0.0015 + s.high * 0.004 + control * 0.0015) * (0.35 + s.phrasePulse * 0.65)
                + Math.sin(Math.PI * 2.0 * note(rootNote + chord + 36) * t) * s.phrasePulse * (0.010 + s.brightness * 0.014);
        final double dropAir = role == DROP ? (1.0 - smoothstep(0.00, 0.08, s.sectionProgress)) * transitionSweep(t, s, chord) * 0.026 : 0.0;
        final double releaseTone = role == RELEASE || role == FINALE ? Math.sin(Math.PI * 2.0 * note(rootNote + chord + 31) * t)
                * s.release * (0.06 + sectionRise * 0.08) : 0.0;

        final double bassBus = Math.tanh((sub + reese) * 1.75) * (0.92 + s.drive * 0.20);
        final double center = punch + snap + bassBus + arp + lead + releaseTone;
        final double left = pad.left + center + clapNoise * 0.74 + hats * 0.58 + tick * 0.36 + riser * 0.62 + shimmer * 0.72 + dropAir;
        final double right = pad.right + center + clapNoise * 0.58 + hats * 0.92 - tick * 0.22 + riser * 0.86 - shimmer * 0.55 + dropAir * 0.82;
        return new Sample(Math.tanh(left * 1.55), Math.tanh(right * 1.55));
    }

    private Sample padCloud(final double t, final double semitone, final double tension, final double gain, final double brightness) {
        final int color = tension > 0.62 ? 16 : 15;
        final double root = chordTone(t, semitone, brightness, 0.34);
        final double fifth = chordTone(t + 0.013, semitone + 7, brightness, 0.24);
        final double octave = chordTone(t + 0.021, semitone + 12, brightness, 0.20);
        final double third = chordTone(t + 0.034, semitone + color, brightness, 0.18);
        final double ninth = chordTone(t + 0.055, semitone + 26, brightness, 0.10 + tension * 0.04);
        final double susp = chordTone(t + 0.089, semitone + 22, brightness, tension * 0.10);
        final double left = (root + fifth * 0.72 + third * 0.66 + ninth * 0.44 + susp * 0.38) * gain;
        final double right = (root * 0.88 + octave * 0.72 + third * 0.76 + ninth * 0.55 - susp * 0.26) * gain;
        return new Sample(left, right);
    }

    private double chordTone(final double t, final double semitone, final double brightness, final double gain) {
        final double f = note((int) Math.round(semitone));
        final double slow = 0.72 + 0.28 * Math.sin(t * 0.19 + semitone * 0.11);
        return (Math.sin(Math.PI * 2.0 * f * t)
                + Math.sin(Math.PI * 2.0 * f * 0.501 * t + 1.4) * (0.24 + brightness * 0.16)
                + polishedTone(f * 0.251 * t + 0.17, brightness, 0.28)) * gain * slow;
    }

    private boolean pressed(final int code) {
        return code >= 0 && code < keys.length && keys[code].get();
    }

    private void applyKey(final int code, final boolean down) {
        if (code >= 0 && code < keys.length) {
            keys[code].set(down);
        }
    }

    private void clearKeys() {
        for (final AtomicBoolean key : keys) {
            key.set(false);
        }
    }

    @Override
    public void keyPressed(final KeyEvent event) {
        applyKey(event.getKeyCode(), true);
    }

    @Override
    public void keyReleased(final KeyEvent event) {
        applyKey(event.getKeyCode(), false);
    }

    @Override
    public void keyTyped(final KeyEvent event) {
        // The demo uses physical keys only.
    }

    private static double note(final int semitone) {
        return 440.0 * Math.pow(2.0, (semitone - 69.0) / 12.0);
    }

    private int visualFamily(final int section) {
        return VISUAL_ORDERS[sequenceVariant][Math.floorMod(section, SECTIONS)];
    }

    private int musicRole(final int section) {
        return MUSIC_ORDERS[musicVariant][Math.floorMod(section, SECTIONS)];
    }

    private int kickMask(final int section) {
        return KICK_BANKS[grooveVariant][Math.floorMod(section, SECTIONS)];
    }

    private int snareMask(final int section) {
        return SNARE_BANKS[grooveVariant][Math.floorMod(section, SECTIONS)];
    }

    private int hatMask(final int section) {
        return HAT_BANKS[grooveVariant][Math.floorMod(section, SECTIONS)];
    }

    private double metallicHat(final double t, final int chord) {
        final double base = note(rootNote + chord + 43);
        return (Math.sin(Math.PI * 2.0 * base * 1.00 * t)
                + Math.sin(Math.PI * 2.0 * base * 1.41 * t + 0.7) * 0.60
                + Math.sin(Math.PI * 2.0 * base * 1.73 * t + 1.9) * 0.42) * 0.38;
    }

    private double metallicShimmer(final double t, final int chord) {
        final double base = note(rootNote + chord + 36);
        final double slow = 0.55 + 0.45 * Math.sin(t * 0.23 + harmonyVariant);
        return (Math.sin(Math.PI * 2.0 * base * t + Math.sin(t * 0.31) * 0.25)
                + Math.sin(Math.PI * 2.0 * base * 1.498 * t + 1.1) * 0.45
                + Math.sin(Math.PI * 2.0 * base * 2.004 * t + 2.0) * 0.25) * slow;
    }

    private double transitionSweep(final double t, final Signals s, final int chord) {
        final double rise = smoothstep(0.52, 1.0, s.sectionProgress);
        final double base = note(rootNote + chord + 24) * (1.0 + rise * 2.5);
        final double tone = Math.sin(Math.PI * 2.0 * base * t + rise * rise * 12.0)
                + Math.sin(Math.PI * 2.0 * base * 1.507 * t + 0.6) * 0.34;
        return tone * 0.72 + bandNoise(t, 8_000.0 + rise * 8_000.0, 0.73) * 0.10;
    }

    private static double kickDrum(final double age, final double hit, final double drive) {
        if (hit <= 0.0) {
            return 0.0;
        }
        final double sweep = 42.0 * age + (90.0 + drive * 85.0) * (1.0 - Math.exp(-age * 35.0)) / 35.0;
        final double body = Math.sin(Math.PI * 2.0 * sweep) * Math.exp(-age * (8.0 + drive * 4.0));
        final double thump = Math.sin(Math.PI * 2.0 * 48.0 * age) * Math.exp(-age * 13.0);
        final double click = Math.sin(Math.PI * 2.0 * 1_900.0 * age) * Math.exp(-age * 110.0);
        return Math.tanh(body * 1.65 + thump * 0.55 + click * 0.16) * hit * (0.62 + drive * 0.46);
    }

    private static double polishedTone(final double phase, final double brightness, final double fold) {
        final double sine = Math.sin(Math.PI * 2.0 * phase);
        final double shaped = Math.tanh((softSaw(phase) * brightness + sine * (1.2 - brightness * 0.35)) * (1.0 + fold));
        return shaped * 0.72 + sine * 0.28;
    }

    private static double softSaw(final double phase) {
        final double p = phase - Math.floor(phase);
        final double saw = p * 2.0 - 1.0;
        return Math.tanh(saw * 1.85);
    }

    private static double bandNoise(final double t, final double rate, final double salt) {
        final double a = highNoise(Math.floor(t * rate) / rate + salt);
        final double b = highNoise(Math.floor(t * rate * 0.517) / (rate * 0.517) + salt * 1.73);
        return (a - b) * 0.5;
    }

    private double computeCycleBeats() {
        double result = 0.0;
        for (int i = 0; i < SECTIONS; i++) {
            result += sectionBeatLength(i);
        }
        return result;
    }

    private double sectionBeatLength(final int section) {
        return SECTION_BEATS[section] * (section == 0 ? introStretch : 1.0);
    }

    private static double hit(final int mask, final int step) {
        return ((mask >>> step) & 1) == 1 ? 1.0 : 0.0;
    }

    private static double tri(final double phase) {
        final double p = phase - Math.floor(phase);
        return 4.0 * Math.abs(p - 0.5) - 1.0;
    }

    private static double highNoise(final double t) {
        final double x = Math.sin((t * 19_123.123 + 4.17) * 12.9898) * 43_758.5453;
        return (x - Math.floor(x)) * 2.0 - 1.0;
    }

    private static double wrap(final double value, final double min, final double max) {
        final double width = max - min;
        return value - Math.floor((value - min) / width) * width;
    }

    private static double smoothstep(final double edge0, final double edge1, final double x) {
        final double t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double clamp01(final double value) {
        return clamp(value, 0.0, 1.0);
    }

    private static double clamp(final double value, final double min, final double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Color withAlpha(final Color color, final double alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) clamp(alpha * 255.0, 0.0, 255.0));
    }

    private static Color mix(final Color a, final Color b, final double t) {
        final double m = clamp01(t);
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * m),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * m),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * m),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * m)
        );
    }

    private static double hash01(final long value) {
        long x = value;
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return (x >>> 11) * 0x1.0p-53;
    }

    private record Vec3(double x, double y, double z) {
        private Vec3 add(final Vec3 other) {
            return new Vec3(x + other.x, y + other.y, z + other.z);
        }

        private Vec3 subtract(final Vec3 other) {
            return new Vec3(x - other.x, y - other.y, z - other.z);
        }

        private Vec3 mix(final Vec3 other, final double t) {
            final double m = clamp01(t);
            return new Vec3(x + (other.x - x) * m, y + (other.y - y) * m, z + (other.z - z) * m);
        }

        private double dot(final Vec3 other) {
            return x * other.x + y * other.y + z * other.z;
        }

        private Vec3 cross(final Vec3 other) {
            return new Vec3(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x);
        }

        private double length() {
            return Math.sqrt(dot(this));
        }

        private Vec3 normalize() {
            final double length = length();
            if (length < 0.000001) {
                return new Vec3(0, 0, 0);
            }
            return new Vec3(x / length, y / length, z / length);
        }
    }

    private record Camera(Vec3 position, Vec3 right, Vec3 up, Vec3 forward, double roll, double focal, double centerY) {
    }

    private record Screen(double x, double y, double depth, double scale, boolean visible) {
        private static final Screen HIDDEN = new Screen(0, 0, 0, 0, false);
    }

    private record Signals(int section, int loop, double sectionTime, double sectionProgress, double beatPhase,
                           double barPhase, double beatPulse, double barPulse, double phrasePulse, double low,
                           double high, double tension, double intensity, double release, double sectionBlend,
                           double drive, double brightness, double groove, int step, double stepPhase) {
    }

    private record Palette(Color skyTop, Color skyBottom, Color solid, Color edge, Color glow, Color haze,
                           Color core, Color spark) {
    }

    private record Sample(double left, double right) {
    }

    private static final class Particle {
        private final double a;
        private final double b;
        private final double c;
        private final double phase;
        private final double speed;
        private final double size;
        private final double hot;

        private Particle(final long seed) {
            a = hash01(seed);
            b = hash01(seed + 17L);
            c = hash01(seed + 43L);
            phase = hash01(seed + 71L) * Math.PI * 2.0;
            speed = 1.5 + hash01(seed + 101L) * 6.5;
            size = 1.4 + hash01(seed + 131L) * 2.8;
            hot = hash01(seed + 181L);
        }
    }

    private static final class Pulse {
        private double born = -99.0;
        private double strength;
        private int section;
        private int loop;

        private boolean reset(final double time, final double strength, final int section, final int loop) {
            born = time;
            this.strength = strength;
            this.section = section;
            this.loop = loop;
            return true;
        }
    }
}
