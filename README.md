# Noxius64kDemo

`Noxius64kDemo` is a plain-Java audiovisual demo experiment inspired by demoscene 64k intros: procedural visuals, procedural music, one executable, and no framework pile hiding the machine.

## What "64k" originally means

In the original demoscene sense, a 64k intro is a size-limited audiovisual program where the executable must fit into 65,536 bytes. That size pressure forced people to generate visuals, geometry, textures, and music procedurally instead of shipping big assets. A reasonable starting point is the [Wikipedia overview of the demoscene and 64K intros](https://en.wikipedia.org/wiki/Demoscene#64K_intro).

This project is inspired by that constraint. It is **not** a real competition-grade 64k executable. The produced jar, app-image, and native binary are all much larger. The point here is the approach and the attitude, not fake marketing around byte counts.

## Why it exists

I like how much impressive, powerful tech is already in our hands without dragging in libraries, frameworks, engines, or fashionable ceremony. Plain Java, the standard tools, a single source tree, and a machine that still does what you tell it. Real engineering.

Could it be pushed further with more time? Obviously. Modern tooling also has a habit of rotting brains by making people forget that the platform underneath is already absurdly capable.

## Build and run

Use the root script. That is the real build.

```sh
sh ./Noxius64kDemo.sh
sh ./Noxius64kDemo.sh doctor
sh ./Noxius64kDemo.sh run
sh ./Noxius64kDemo.sh jpackage
sh ./Noxius64kDemo.sh native
```

The script:

- requires and targets Java 17 only
- downloads the latest Java LTS locally if no suitable JDK is installed
- builds with plain `javac`, `jar`, `jdeps`, `jlink`, `jpackage`, and optional GraalVM `native-image`
- writes a fast output index to `target/OUTPUTS.txt`
- has a `doctor` mode for OS/arch/tool/download diagnostics
- produces host-platform artifacts, so run it on the OS you want to package for

On Windows, you can use `Noxius64kDemo.cmd`. It looks for `sh.exe` from Git Bash, MSYS2, or Cygwin and forwards all arguments to the real script. The actual build logic still lives in the POSIX shell script.

## Maven

The build script remains the local build and packaging entrypoint. Maven is the CI and publishing bridge: it produces the
versioned JAR, sources, Javadoc, and native release assets without replacing the script's `jlink` and `jpackage` flow.

## Output locations

After each script action, the script prints the relevant output paths and refreshes `target/OUTPUTS.txt`.

Typical outputs are:

- `target/Noxius64kDemo.jar`
- `target/jpackage/`
- `target/native/`
- `target/OUTPUTS.txt`
- `target/assets/Noxius64kDemo-1024.png`
