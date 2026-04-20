#!/bin/sh
set -eu

APP_NAME="Noxius64kDemo"
MAIN_CLASS="berlin.yuna.Noxius64kDemo"
JAVA_RELEASE="17"
REQUIRED_JAVA="17"
LATEST_LTS_JAVA="25"
GRAALVM_VERSION_DEFAULT="25.0.2"
# GraalVM 25.0.2 dropped macOS x64; keep the last supported release for Intel Macs.
GRAALVM_VERSION_FALLBACK_MACOS_X64="25.0.1"
APP_VENDOR="berlin.yuna"
APP_COPYRIGHT="Copyright 2026 berlin.yuna"
APP_DESCRIPTION="Cinematic audiovisual demo with procedural music, seeded scene variation, and interactive camera energy."
APP_MAC_ID="berlin.yuna.Noxius64kDemo"
APP_MAC_NAME="Noxius64kDemo"
APP_MAC_CATEGORY="entertainment"

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_DIR="$SCRIPT_DIR"
SRC_DIR="$PROJECT_DIR/src/main/java"
TARGET_DIR="$PROJECT_DIR/target"
ASSETS_DIR="$TARGET_DIR/assets"
CLASSES_DIR="$TARGET_DIR/classes"
TMP_DIR="$TARGET_DIR/tmp"
JAR_FILE="$TARGET_DIR/$APP_NAME.jar"
JLINK_DIR="$TARGET_DIR/jlink"
JPACKAGE_DIR="$TARGET_DIR/jpackage"
NATIVE_DIR="$TARGET_DIR/native"
OUTPUTS_FILE="$TARGET_DIR/OUTPUTS.txt"
TOOLS_DIR="$PROJECT_DIR/.noxius-tools"
DOWNLOAD_DIR="$TOOLS_DIR/downloads"
TEMURIN_HOME="$TOOLS_DIR/temurin-jdk-$LATEST_LTS_JAVA"
ICON_WORK_DIR="$TARGET_DIR/tmp/icon"

ACTION=""
PACKAGE_TYPE="app-image"
JAVA_HOME_DIR=""
JAVA_BIN=""
JAVAC_BIN=""
JAR_BIN=""
JDEPS_BIN=""
JLINK_BIN=""
JPACKAGE_BIN=""
GRAALVM_HOME=""
NATIVE_IMAGE_BIN=""
COLOR_RESET=""
COLOR_BOLD=""
COLOR_DIM=""
COLOR_RED=""
COLOR_GREEN=""
COLOR_YELLOW=""
COLOR_BLUE=""
COLOR_MAGENTA=""
COLOR_CYAN=""

setup_colors() {
  if [ -t 1 ] && [ "${TERM:-}" != "dumb" ]; then
    COLOR_RESET=$(printf '\033[0m')
    COLOR_BOLD=$(printf '\033[1m')
    COLOR_DIM=$(printf '\033[2m')
    COLOR_RED=$(printf '\033[31m')
    COLOR_GREEN=$(printf '\033[32m')
    COLOR_YELLOW=$(printf '\033[33m')
    COLOR_BLUE=$(printf '\033[34m')
    COLOR_MAGENTA=$(printf '\033[35m')
    COLOR_CYAN=$(printf '\033[36m')
  fi
}

say() {
  printf '%s\n' "$*"
}

info() {
  printf '%s[info]%s %s\n' "$COLOR_CYAN" "$COLOR_RESET" "$*"
}

success() {
  printf '%s[done]%s %s\n' "$COLOR_GREEN" "$COLOR_RESET" "$*"
}

warn() {
  printf '%s[warn]%s %s\n' "$COLOR_YELLOW" "$COLOR_RESET" "$*"
}

die() {
  printf '%s[fail]%s %s\n' "$COLOR_RED" "$COLOR_RESET" "$*" >&2
  exit 1
}

headline() {
  printf '\n%s%s%s\n' "$COLOR_BOLD" "$*" "$COLOR_RESET"
}

subtle() {
  printf '%s%s%s\n' "$COLOR_DIM" "$*" "$COLOR_RESET"
}

have_cmd() {
  command -v "$1" >/dev/null 2>&1
}

project_rel() {
  path=$1
  case "$path" in
    "$PROJECT_DIR"/*) printf '%s\n' "${path#$PROJECT_DIR/}" ;;
    *) printf '%s\n' "$path" ;;
  esac
}

tool_path_in_home() {
  home=$1
  tool=$2
  if [ -x "$home/bin/$tool" ]; then
    printf '%s\n' "$home/bin/$tool"
    return 0
  fi
  if [ -x "$home/bin/$tool.exe" ]; then
    printf '%s\n' "$home/bin/$tool.exe"
    return 0
  fi
  return 1
}

host_os() {
  os=$(uname -s)
  case "$os" in
    Darwin) printf '%s\n' "mac" ;;
    Linux) printf '%s\n' "linux" ;;
    CYGWIN*|MINGW*|MSYS*) printf '%s\n' "windows" ;;
    FreeBSD|OpenBSD|NetBSD|DragonFly|AIX|SunOS)
      printf '%s\n' "unix"
      ;;
    *)
      die "Unsupported OS: $os"
      ;;
  esac
}

host_arch() {
  arch=$(uname -m)
  case "$arch" in
    x86_64|amd64) printf '%s\n' "x64" ;;
    arm64|aarch64) printf '%s\n' "aarch64" ;;
    *)
      die "Unsupported CPU architecture: $arch"
      ;;
  esac
}

windows_shell_environment() {
  case "$(uname -s)" in
    CYGWIN*|MINGW*|MSYS*) return 0 ;;
    *) return 1 ;;
  esac
}

archive_extension_for_os() {
  os=$1
  case "$os" in
    windows) printf '%s\n' "zip" ;;
    mac|macos|linux) printf '%s\n' "tar.gz" ;;
    *)
      die "No bundled archive format is configured for OS: $os"
      ;;
  esac
}

native_binary_suffix() {
  case "$(host_os)" in
    windows) printf '%s\n' ".exe" ;;
    *) printf '%s\n' "" ;;
  esac
}

temurin_download_url() {
  set -- $(runtime_platform)
  os=$1
  arch=$2
  printf '%s\n' "https://api.adoptium.net/v3/binary/latest/$LATEST_LTS_JAVA/ga/$os/$arch/jdk/hotspot/normal/eclipse?project=jdk"
}

graalvm_download_url() {
  set -- $(graalvm_platform)
  os=$1
  arch=$2
  version=$(graalvm_version)
  case "$os/$arch" in
    linux/x64|linux/aarch64|macos/x64|macos/aarch64|windows/x64)
      ;;
    *)
      die "No official GraalVM Community Native Image download is configured for $os/$arch."
      ;;
  esac
  archive_ext=$(archive_extension_for_os "$os")
  printf '%s\n' "https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-$version/graalvm-community-jdk-$version"_"$os-$arch"_bin.$archive_ext
}

shell_compatibility_text() {
  case "$(host_os)" in
    windows)
      printf '%s\n' "POSIX shell required. Use Git Bash, MSYS2, Cygwin, or the Noxius64kDemo.cmd wrapper."
      ;;
    *)
      printf '%s\n' "POSIX shell OK."
      ;;
  esac
}

extract_archive() {
  archive=$1
  destination=$2
  case "$archive" in
    *.tar.gz|*.tgz)
      tar -xzf "$archive" -C "$destination"
      ;;
    *.zip)
      if have_cmd unzip; then
        unzip -q "$archive" -d "$destination"
        return 0
      fi
      if windows_shell_environment && have_cmd powershell.exe && have_cmd cygpath; then
        powershell.exe -NoProfile -Command "Expand-Archive -Force -LiteralPath '$(cygpath -w "$archive")' -DestinationPath '$(cygpath -w "$destination")'" >/dev/null
        return 0
      fi
      die "Missing unzip support for $archive. Install unzip or run from a shell that provides it."
      ;;
    *)
      die "Unsupported archive format: $archive"
      ;;
  esac
}

java_major() {
  "$1" -version 2>&1 | awk -F '"' '/version/ { split($2, parts, "."); if (parts[1] == "1") print parts[2]; else print parts[1]; exit }'
}

java_home_from_bin() {
  "$1" -XshowSettings:properties -version 2>&1 | awk -F'= ' '/^[[:space:]]*java.home = / { print $2; exit }'
}

jdk_ok_home() {
  home=$1
  [ -n "$home" ] || return 1
  java_bin=$(tool_path_in_home "$home" java || true)
  javac_bin=$(tool_path_in_home "$home" javac || true)
  jar_bin=$(tool_path_in_home "$home" jar || true)
  jdeps_bin=$(tool_path_in_home "$home" jdeps || true)
  jlink_bin=$(tool_path_in_home "$home" jlink || true)
  jpackage_bin=$(tool_path_in_home "$home" jpackage || true)
  [ -n "${java_bin:-}" ] || return 1
  [ -n "${javac_bin:-}" ] || return 1
  [ -n "${jar_bin:-}" ] || return 1
  [ -n "${jdeps_bin:-}" ] || return 1
  [ -n "${jlink_bin:-}" ] || return 1
  [ -n "${jpackage_bin:-}" ] || return 1
  major=$(java_major "$java_bin" || printf '0')
  [ "${major:-0}" -ge "$REQUIRED_JAVA" ] 2>/dev/null
}

use_jdk() {
  JAVA_HOME_DIR=$1
  JAVA_BIN=$(tool_path_in_home "$JAVA_HOME_DIR" java || die "java tool missing under $JAVA_HOME_DIR")
  JAVAC_BIN=$(tool_path_in_home "$JAVA_HOME_DIR" javac || die "javac tool missing under $JAVA_HOME_DIR")
  JAR_BIN=$(tool_path_in_home "$JAVA_HOME_DIR" jar || die "jar tool missing under $JAVA_HOME_DIR")
  JDEPS_BIN=$(tool_path_in_home "$JAVA_HOME_DIR" jdeps || die "jdeps tool missing under $JAVA_HOME_DIR")
  JLINK_BIN=$(tool_path_in_home "$JAVA_HOME_DIR" jlink || die "jlink tool missing under $JAVA_HOME_DIR")
  JPACKAGE_BIN=$(tool_path_in_home "$JAVA_HOME_DIR" jpackage || die "jpackage tool missing under $JAVA_HOME_DIR")
  export JAVA_HOME="$JAVA_HOME_DIR"
}

runtime_platform() {
  os=$(host_os)
  arch=$(host_arch)
  case "$os" in
    mac|linux|windows)
      ;;
    unix)
      die "Automatic Temurin download is not configured for this Unix platform. Install a local JDK >= $REQUIRED_JAVA first."
      ;;
  esac
  printf '%s %s\n' "$os" "$arch"
}

graalvm_platform() {
  os=$(host_os)
  arch=$(host_arch)
  case "$os" in
    mac) os="macos" ;;
    linux|windows) ;;
    unix)
      die "Automatic GraalVM download is not configured for this Unix platform. Install a local GraalVM Native Image toolchain first."
      ;;
  esac
  printf '%s %s\n' "$os" "$arch"
}

graalvm_version() {
  set -- $(graalvm_platform)
  os=$1
  arch=$2
  if [ "$os" = "macos" ] && [ "$arch" = "x64" ]; then
    printf '%s\n' "$GRAALVM_VERSION_FALLBACK_MACOS_X64"
    return 0
  fi
  printf '%s\n' "$GRAALVM_VERSION_DEFAULT"
}

download() {
  url=$1
  output=$2
  if have_cmd curl; then
    curl -fL --retry 3 --connect-timeout 20 -o "$output" "$url"
    return 0
  fi
  if have_cmd wget; then
    wget -O "$output" "$url"
    return 0
  fi
  die "Missing curl or wget; cannot download required runtime."
}

discover_home_for_tool() {
  root=$1
  tool_name=$2
  tool_path=$(find "$root" -type f \( -path "*/bin/$tool_name" -o -path "*/bin/$tool_name.exe" \) | head -n 1)
  [ -n "${tool_path:-}" ] || return 1
  dirname "$(dirname "$tool_path")"
}

install_temurin() {
  mkdir -p "$DOWNLOAD_DIR"
  set -- $(runtime_platform)
  os=$1
  arch=$2
  tmp="$DOWNLOAD_DIR/temurin.$$"
  archive_ext=$(archive_extension_for_os "$os")
  archive="$tmp/jdk.$archive_ext"
  url=$(temurin_download_url)

  rm -rf "$tmp" "$TEMURIN_HOME"
  mkdir -p "$tmp"
  info "Downloading Eclipse Temurin JDK $LATEST_LTS_JAVA for $os/$arch..."
  download "$url" "$archive"
  extract_archive "$archive" "$tmp"
  extracted_home=$(discover_home_for_tool "$tmp" java || true)
  [ -n "${extracted_home:-}" ] || die "Downloaded Temurin archive did not contain a JDK."
  mv "$extracted_home" "$TEMURIN_HOME"
  rm -rf "$tmp"
}

ensure_build_jdk() {
  if jdk_ok_home "$TEMURIN_HOME"; then
    use_jdk "$TEMURIN_HOME"
    return 0
  fi
  if [ -n "${JAVA_HOME:-}" ] && jdk_ok_home "$JAVA_HOME"; then
    use_jdk "$JAVA_HOME"
    return 0
  fi
  if have_cmd java; then
    system_home=$(java_home_from_bin "$(command -v java)" || true)
    if [ -n "${system_home:-}" ] && jdk_ok_home "$system_home"; then
      use_jdk "$system_home"
      return 0
    fi
  fi
  install_temurin
  jdk_ok_home "$TEMURIN_HOME" || die "Installed Temurin JDK is not usable."
  use_jdk "$TEMURIN_HOME"
}

prompt_action() {
  if [ ! -t 0 ]; then
    die "No action provided. Use: doctor | run | jpackage | native"
  fi
  headline "Select action"
  say "  ${COLOR_BOLD}1${COLOR_RESET}) Doctor"
  say "  ${COLOR_BOLD}2${COLOR_RESET}) Run jar"
  say "  ${COLOR_BOLD}3${COLOR_RESET}) Build jpackage app-image"
  say "  ${COLOR_BOLD}4${COLOR_RESET}) Build GraalVM native executable"
  say "  ${COLOR_BOLD}q${COLOR_RESET}) Quit"
  printf '> '
  IFS= read -r choice
  case "$choice" in
    1|d|D|doctor) ACTION="doctor" ;;
    2|a|A|run) ACTION="run" ;;
    3|b|B|jpackage) ACTION="jpackage" ;;
    4|c|C|native) ACTION="native" ;;
    q|Q|quit|exit) exit 0 ;;
    *) die "Unknown selection: $choice" ;;
  esac
}

parse_args() {
  if [ "$#" -eq 0 ]; then
    prompt_action
    return 0
  fi

  ACTION=$1
  shift

  while [ "$#" -gt 0 ]; do
    case "$1" in
      --type|--package-type)
        [ "$#" -ge 2 ] || die "Missing value for $1"
        PACKAGE_TYPE=$2
        shift 2
        ;;
      -h|--help|help)
        ACTION="help"
        shift
        ;;
      *)
        die "Unknown argument: $1"
        ;;
    esac
  done

  case "$ACTION" in
    doctor|d|D) ACTION="doctor" ;;
    run|a|A) ACTION="run" ;;
    jpackage|b|B) ACTION="jpackage" ;;
    native|c|C) ACTION="native" ;;
    help) ACTION="help" ;;
    *) die "Unknown action: $ACTION" ;;
  esac
}

usage() {
  headline "Noxius64kDemo"
  say "Usage:"
  say "  ${COLOR_BOLD}./Noxius64kDemo.sh doctor${COLOR_RESET}"
  say "  ${COLOR_BOLD}./Noxius64kDemo.sh run${COLOR_RESET}"
  say "  ${COLOR_BOLD}./Noxius64kDemo.sh jpackage --type app-image|dmg|pkg|deb|rpm|msi|exe${COLOR_RESET}"
  say "  ${COLOR_BOLD}./Noxius64kDemo.sh native${COLOR_RESET}"
  say ""
  say "Actions:"
  say "  ${COLOR_BOLD}doctor${COLOR_RESET}    Print host OS, arch, shell compatibility, chosen download URLs, and current tool paths."
  say "  ${COLOR_BOLD}run${COLOR_RESET}       Compile with --release $JAVA_RELEASE, build $(project_rel "$JAR_FILE"), and run it."
  say "  ${COLOR_BOLD}jpackage${COLOR_RESET}  Compile, detect modules with jdeps, build jlink, then build jpackage output."
  say "  ${COLOR_BOLD}native${COLOR_RESET}    Compile, build the jar, then attempt a GraalVM native-image build."
  say ""
  say "Notes:"
  say "  - Plain Java tools only. Maven is not involved."
  say "  - If no usable JDK >= $REQUIRED_JAVA exists, the script downloads Eclipse Temurin JDK $LATEST_LTS_JAVA into $(project_rel "$TOOLS_DIR")."
  say "  - The script requires and targets Java $JAVA_RELEASE."
  say "  - GraalVM native builds for AWT/Swing remain best-effort."
  say "  - Build outputs are host-platform artifacts. Run the script on each target OS."
}

ensure_layout() {
  mkdir -p "$TARGET_DIR" "$ASSETS_DIR" "$TMP_DIR" "$TOOLS_DIR" "$DOWNLOAD_DIR"
}

write_output_index() {
  ensure_layout
  {
    printf '%s outputs\n' "$APP_NAME"
    printf 'Generated: %s\n' "$(date '+%Y-%m-%d %H:%M:%S %Z')"
    printf '\n'
    if [ -f "$JAR_FILE" ]; then
      printf 'jar: %s\n' "$(project_rel "$JAR_FILE")"
    fi
    if [ -d "$JLINK_DIR" ]; then
      printf 'jlink runtime: %s\n' "$(project_rel "$JLINK_DIR")"
    fi
    if [ -d "$JPACKAGE_DIR" ]; then
      find "$JPACKAGE_DIR" -maxdepth 2 \( -name '*.app' -o -name '*.dmg' -o -name '*.pkg' -o -name '*.deb' -o -name '*.rpm' -o -name '*.msi' -o -name '*.exe' -o -name "$APP_NAME" \) | sort | while IFS= read -r path; do
        [ -n "$path" ] && printf 'jpackage: %s\n' "$(project_rel "$path")"
      done
    fi
    if [ -f "$NATIVE_DIR/$APP_NAME" ]; then
      printf 'native: %s\n' "$(project_rel "$NATIVE_DIR/$APP_NAME")"
    fi
    if [ -f "$NATIVE_DIR/$APP_NAME.exe" ]; then
      printf 'native: %s\n' "$(project_rel "$NATIVE_DIR/$APP_NAME.exe")"
    fi
    if [ -f "$ASSETS_DIR/$APP_NAME-1024.png" ]; then
      printf 'icon: %s\n' "$(project_rel "$ASSETS_DIR/$APP_NAME-1024.png")"
    fi
  } > "$OUTPUTS_FILE"
}

show_outputs() {
  write_output_index
  headline "Output files"
  if [ ! -s "$OUTPUTS_FILE" ]; then
    warn "No output index found."
    return 0
  fi
  while IFS= read -r line; do
    case "$line" in
      "$APP_NAME outputs")
        say "  ${COLOR_BOLD}$line${COLOR_RESET}"
        ;;
      Generated:*)
        subtle "  $line"
        ;;
      "")
        say ""
        ;;
      *)
        say "  $line"
        ;;
    esac
  done < "$OUTPUTS_FILE"
  subtle "  Fast path: $(project_rel "$OUTPUTS_FILE")"
}

announce_action() {
  headline "Selected action"
  case "$ACTION" in
    doctor)
      say "  ${COLOR_BOLD}doctor${COLOR_RESET} -> host check, shell check, download URLs, current tool paths"
      ;;
    run)
      say "  ${COLOR_BOLD}run${COLOR_RESET} -> $(project_rel "$JAR_FILE")"
      ;;
    jpackage)
      say "  ${COLOR_BOLD}jpackage${COLOR_RESET} -> $(project_rel "$JAR_FILE"), $(project_rel "$JLINK_DIR"), $(project_rel "$JPACKAGE_DIR")"
      ;;
    native)
      say "  ${COLOR_BOLD}native${COLOR_RESET} -> $(project_rel "$JAR_FILE"), $(project_rel "$NATIVE_DIR")"
      ;;
  esac
  case "$ACTION" in
    doctor)
      subtle "  No artifacts are built in doctor mode."
      ;;
    *)
      subtle "  A fresh artifact index will be written to $(project_rel "$OUTPUTS_FILE")."
      ;;
  esac
}

doctor() {
  headline "Doctor"
  say "  host os: $(host_os)"
  say "  host arch: $(host_arch)"
  say "  shell: $(shell_compatibility_text)"
  say "  java release target: $JAVA_RELEASE"
  say "  build jdk home: $JAVA_HOME_DIR"
  say "  java binary: $JAVA_BIN"
  say "  javac binary: $JAVAC_BIN"
  say "  jpackage binary: $JPACKAGE_BIN"
  say "  temurin lts: $LATEST_LTS_JAVA"
  say "  temurin url: $(temurin_download_url)"
  say "  graalvm version: $(graalvm_version)"
  say "  graalvm url: $(graalvm_download_url)"
  if graalvm_ok_home "$GRAALVM_HOME"; then
    say "  graalvm home: $GRAALVM_HOME"
    say "  native-image: $(tool_path_in_home "$GRAALVM_HOME" native-image)"
  elif [ -n "$JAVA_HOME_DIR" ] && graalvm_ok_home "$JAVA_HOME_DIR"; then
    say "  graalvm home: $JAVA_HOME_DIR"
    say "  native-image: $(tool_path_in_home "$JAVA_HOME_DIR" native-image)"
  else
    say "  graalvm home: not installed locally yet"
    subtle "  Native build will download GraalVM on demand."
  fi
  subtle "  Host-platform builds only. Run this script on each OS you want to package."
}

generate_icon_java_source() {
  tool_src_dir="$ICON_WORK_DIR/src/berlin/yuna"
  mkdir -p "$tool_src_dir"
  cat > "$tool_src_dir/NoxiusIconTool.java" <<'EOF'
package berlin.yuna;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;

public final class NoxiusIconTool {

    private NoxiusIconTool() {
    }

    public static void main(final String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: NoxiusIconTool <size> <output-png>");
        }
        final int size = Integer.parseInt(args[0]);
        final File output = new File(args[1]);
        final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setPaint(new GradientPaint(0, 0, new Color(6, 8, 18), 0, size, new Color(18, 8, 24)));
            g.fillRect(0, 0, size, size);

            final Point2D center = new Point2D.Double(size * 0.5, size * 0.46);
            final float glowRadius = (float) (size * 0.48);
            g.setPaint(new RadialGradientPaint(center, glowRadius,
                    new float[]{0.0f, 0.45f, 1.0f},
                    new Color[]{
                            new Color(255, 98, 162, 92),
                            new Color(92, 232, 255, 48),
                            new Color(0, 0, 0, 0)
                    }));
            g.fillRect(0, 0, size, size);

            g.setComposite(AlphaComposite.SrcOver);
            final double pad = size * 0.055;
            g.setColor(new Color(14, 16, 28, 230));
            g.fillRoundRect((int) pad, (int) pad, (int) (size - pad * 2), (int) (size - pad * 2), size / 5, size / 5);

            final double cx = size * 0.5;
            final double cy = size * 0.43;
            final double outer = size * 0.28;
            final double inner = size * 0.17;

            g.setStroke(new BasicStroke((float) (size * 0.030), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(95, 238, 255, 205));
            g.draw(new Ellipse2D.Double(cx - outer, cy - outer, outer * 2, outer * 2));

            g.setStroke(new BasicStroke((float) (size * 0.014), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 106, 176, 170));
            g.draw(new Ellipse2D.Double(cx - outer * 0.78, cy - outer * 0.78, outer * 1.56, outer * 1.56));

            g.setColor(new Color(8, 9, 14, 255));
            g.fill(new Ellipse2D.Double(cx - inner, cy - inner, inner * 2, inner * 2));

            final Path2D.Double beam = new Path2D.Double();
            beam.moveTo(cx - size * 0.055, cy + size * 0.05);
            beam.lineTo(cx + size * 0.055, cy + size * 0.05);
            beam.lineTo(cx + size * 0.17, size * 0.88);
            beam.lineTo(cx - size * 0.17, size * 0.88);
            beam.closePath();
            g.setPaint(new GradientPaint((float) cx, (float) (cy + size * 0.05), new Color(120, 240, 255, 180),
                    (float) cx, (float) (size * 0.88), new Color(255, 120, 172, 84)));
            g.fill(beam);

            g.setStroke(new BasicStroke((float) (size * 0.010), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 244, 198, 188));
            g.drawLine((int) (cx - size * 0.13), (int) (size * 0.78), (int) (cx + size * 0.13), (int) (size * 0.78));
        } finally {
            g.dispose();
        }
        output.getParentFile().mkdirs();
        ImageIO.write(image, "png", output);
    }
}
EOF
}

compile_icon_tool() {
  tool_classes_dir="$ICON_WORK_DIR/classes"
  tool_source="$ICON_WORK_DIR/src/berlin/yuna/NoxiusIconTool.java"
  generate_icon_java_source
  rm -rf "$tool_classes_dir"
  mkdir -p "$tool_classes_dir"
  "$JAVAC_BIN" --release "$JAVA_RELEASE" -d "$tool_classes_dir" "$tool_source"
}

render_icon_png() {
  size=$1
  out_png=$2
  tool_classes_dir="$ICON_WORK_DIR/classes"
  [ -f "$tool_classes_dir/berlin/yuna/NoxiusIconTool.class" ] || compile_icon_tool
  "$JAVA_BIN" -Djava.awt.headless=true -cp "$tool_classes_dir" berlin.yuna.NoxiusIconTool "$size" "$out_png"
}

prepare_icon() {
  ensure_layout
  rm -rf "$ICON_WORK_DIR"
  mkdir -p "$ICON_WORK_DIR"
  base_png="$ASSETS_DIR/$APP_NAME-1024.png"
  render_icon_png 1024 "$base_png"
  printf '%s\n' "$base_png"
}

apply_macos_bundle_icon() {
  icon_png=$1
  app_bundle=$2
  [ -f "$icon_png" ] || return 0
  [ -d "$app_bundle" ] || return 0
  have_cmd swift || return 0

  swift_dir="$ICON_WORK_DIR/swift"
  swift_script="$swift_dir/set_file_icon.swift"
  mkdir -p "$swift_dir/modcache"
  cat > "$swift_script" <<'EOF'
import Foundation
import AppKit

let args = CommandLine.arguments
if args.count != 3 {
    fatalError("usage: set_file_icon.swift icon.png app-bundle")
}

guard let image = NSImage(contentsOfFile: args[1]) else {
    fatalError("icon image could not be loaded")
}

let ok = NSWorkspace.shared.setIcon(image, forFile: args[2], options: [])
if !ok {
    fatalError("setIcon failed")
}
EOF
  swift -module-cache-path "$swift_dir/modcache" "$swift_script" "$icon_png" "$app_bundle"
}

compile_sources() {
  ensure_layout
  rm -rf "$CLASSES_DIR"
  mkdir -p "$CLASSES_DIR"
  sources_file="$TMP_DIR/sources.txt"
  find "$SRC_DIR" -name '*.java' | sort > "$sources_file"
  [ -s "$sources_file" ] || die "No Java source files found under $SRC_DIR"
  "$JAVAC_BIN" --release "$JAVA_RELEASE" -encoding UTF-8 -d "$CLASSES_DIR" @"$sources_file"
}

build_jar() {
  compile_sources
  rm -f "$JAR_FILE"
  "$JAR_BIN" --create --file "$JAR_FILE" --main-class "$MAIN_CLASS" -C "$CLASSES_DIR" .
  success "Built jar: $(project_rel "$JAR_FILE")"
  write_output_index
}

detect_modules() {
  "$JDEPS_BIN" --multi-release "$JAVA_RELEASE" --ignore-missing-deps --print-module-deps "$JAR_FILE"
}

build_jlink() {
  modules=$(detect_modules)
  [ -n "$modules" ] || die "jdeps did not return any modules."
  rm -rf "$JLINK_DIR"
  "$JLINK_BIN" \
    --add-modules "$modules" \
    --output "$JLINK_DIR" \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --compress=zip-6
  success "Built runtime image: $(project_rel "$JLINK_DIR")"
  write_output_index
}

build_jpackage() {
  build_jar
  build_jlink
  icon_file=$(prepare_icon)
  rm -rf "$JPACKAGE_DIR"
  mkdir -p "$JPACKAGE_DIR"
  set -- $(runtime_platform)
  os=$1
  case "$os" in
    mac)
      "$JPACKAGE_BIN" \
        --type "$PACKAGE_TYPE" \
        --input "$TARGET_DIR" \
        --dest "$JPACKAGE_DIR" \
        --name "$APP_NAME" \
        --main-jar "$APP_NAME.jar" \
        --main-class "$MAIN_CLASS" \
        --runtime-image "$JLINK_DIR" \
        --app-version "1.0.0" \
        --vendor "$APP_VENDOR" \
        --description "$APP_DESCRIPTION" \
        --copyright "$APP_COPYRIGHT" \
        --mac-package-identifier "$APP_MAC_ID" \
        --mac-package-name "$APP_MAC_NAME" \
        --mac-app-category "$APP_MAC_CATEGORY"
      apply_macos_bundle_icon "$icon_file" "$JPACKAGE_DIR/$APP_NAME.app"
      ;;
    windows)
      "$JPACKAGE_BIN" \
        --type "$PACKAGE_TYPE" \
        --input "$TARGET_DIR" \
        --dest "$JPACKAGE_DIR" \
        --name "$APP_NAME" \
        --main-jar "$APP_NAME.jar" \
        --main-class "$MAIN_CLASS" \
        --runtime-image "$JLINK_DIR" \
        --app-version "1.0.0" \
        --vendor "$APP_VENDOR" \
        --description "$APP_DESCRIPTION" \
        --copyright "$APP_COPYRIGHT"
      ;;
    *)
      "$JPACKAGE_BIN" \
        --type "$PACKAGE_TYPE" \
        --input "$TARGET_DIR" \
        --dest "$JPACKAGE_DIR" \
        --name "$APP_NAME" \
        --main-jar "$APP_NAME.jar" \
        --main-class "$MAIN_CLASS" \
        --runtime-image "$JLINK_DIR" \
        --app-version "1.0.0" \
        --vendor "$APP_VENDOR" \
        --description "$APP_DESCRIPTION" \
        --copyright "$APP_COPYRIGHT" \
        --icon "$icon_file"
      ;;
  esac
  success "Built jpackage output in: $(project_rel "$JPACKAGE_DIR")"
  write_output_index
  show_outputs
}

graalvm_ok_home() {
  home=$1
  [ -n "$home" ] || return 1
  native_image=$(tool_path_in_home "$home" native-image || true)
  [ -n "${native_image:-}" ] || return 1
  version=$("$native_image" --version 2>/dev/null | awk 'NR==1 {print $2}')
  [ -n "${version:-}" ] || return 1
  expected=$(graalvm_version)
  [ "$version" = "$expected" ]
}

install_graalvm() {
  mkdir -p "$DOWNLOAD_DIR"
  set -- $(graalvm_platform)
  os=$1
  arch=$2
  version=$(graalvm_version)
  GRAALVM_HOME="$TOOLS_DIR/graalvm-jdk-$version-$os-$arch"
  tmp="$DOWNLOAD_DIR/graalvm.$$"
  archive_ext=$(archive_extension_for_os "$os")
  archive="$tmp/graalvm.$archive_ext"
  url=$(graalvm_download_url)

  rm -rf "$tmp" "$GRAALVM_HOME"
  mkdir -p "$tmp"
  info "Downloading GraalVM Community JDK $version for $os/$arch..."
  download "$url" "$archive"
  extract_archive "$archive" "$tmp"
  extracted_home=$(discover_home_for_tool "$tmp" native-image || true)
  [ -n "${extracted_home:-}" ] || die "Downloaded GraalVM archive did not contain native-image."
  mv "$extracted_home" "$GRAALVM_HOME"
  rm -rf "$tmp"
}

ensure_graalvm() {
  expected=$(graalvm_version)

  if graalvm_ok_home "$GRAALVM_HOME"; then
    NATIVE_IMAGE_BIN=$(tool_path_in_home "$GRAALVM_HOME" native-image || die "native-image tool missing under $GRAALVM_HOME")
    return 0
  fi

  if [ -n "$JAVA_HOME_DIR" ] && graalvm_ok_home "$JAVA_HOME_DIR"; then
    GRAALVM_HOME="$JAVA_HOME_DIR"
    NATIVE_IMAGE_BIN=$(tool_path_in_home "$GRAALVM_HOME" native-image || die "native-image tool missing under $GRAALVM_HOME")
    return 0
  fi

  install_graalvm
  graalvm_ok_home "$GRAALVM_HOME" || die "Installed GraalVM $expected is not usable."
  NATIVE_IMAGE_BIN=$(tool_path_in_home "$GRAALVM_HOME" native-image || die "native-image tool missing under $GRAALVM_HOME")
}

build_native() {
  build_jar
  ensure_graalvm
  rm -rf "$NATIVE_DIR"
  mkdir -p "$NATIVE_DIR"
  native_tmp="$NATIVE_DIR/tmp"
  native_output="$APP_NAME$(native_binary_suffix)"
  mkdir -p "$native_tmp"
  warn "Native build note: GraalVM documents that AWT/Swing apps may need reachability metadata."
  (
    cd "$NATIVE_DIR"
    TMPDIR="$native_tmp" \
    TMP="$native_tmp" \
    TEMP="$native_tmp" \
      "$NATIVE_IMAGE_BIN" \
      -J-Djava.io.tmpdir="$native_tmp" \
      --no-fallback \
      -Os \
      -march=compatibility \
      -jar "$JAR_FILE" \
      -o "$native_output"
  )
  success "Built native executable in: $(project_rel "$NATIVE_DIR")"
  write_output_index
  show_outputs
}

run_jar() {
  build_jar
  show_outputs
  info "Launching $(project_rel "$JAR_FILE")"
  exec "$JAVA_BIN" -jar "$JAR_FILE"
}

setup_colors
ensure_build_jdk
parse_args "$@"
[ "$ACTION" = "help" ] || announce_action

case "$ACTION" in
  help)
    usage
    ;;
  doctor)
    doctor
    ;;
  run)
    run_jar
    ;;
  jpackage)
    build_jpackage
    ;;
  native)
    build_native
    ;;
  *)
    die "Unhandled action: $ACTION"
    ;;
esac
