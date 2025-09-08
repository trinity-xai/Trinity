{
  description = "Nix dev environment for Trinity/JavaFX with zsh + tmux";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable"; # bleeding
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };

        # Concrete JDK derivation (avoid pkgs.jdk indirection)
        jdk = pkgs.temurin-bin-21;

        trinityRun = pkgs.writeShellScriptBin "trinity-run" ''
          set -euo pipefail

          # Prefer shaded jar if present
          SHADED_JAR="$(ls target/*-shaded.jar 2>/dev/null | head -n1 || true)"
          if [ -n "$SHADED_JAR" ]; then
            CP="$SHADED_JAR"
          else
            if [ ! -d target/classes ]; then
              ./mvnw -q -DskipTests package
            fi
            if [ ! -d target/dependency ]; then
              ./mvnw -q dependency:copy-dependencies -DoutputDirectory=target/dependency
            fi
            CP="target/classes:$(echo target/dependency/*.jar | tr ' ' ':')"
          fi

          # Base libs JavaFX needs
          BASE_LD="${
            pkgs.lib.makeLibraryPath [
              pkgs.stdenv.cc.cc
              pkgs.mesa pkgs.libGL pkgs.wayland
              pkgs.xorg.libX11 pkgs.xorg.libXext pkgs.xorg.libXrandr pkgs.xorg.libXcursor pkgs.xorg.libXi
              pkgs.xorg.libXxf86vm pkgs.xorg.libXtst pkgs.xorg.libXrender pkgs.xorg.libXinerama
              pkgs.libxkbcommon pkgs.gtk3 pkgs.glib pkgs.pango pkgs.cairo pkgs.gdk-pixbuf pkgs.at-spi2-core
              pkgs.alsa-lib pkgs.fontconfig pkgs.freetype pkgs.zlib
            ]
          }"

          # NixOS system OpenGL driver paths to avoid GL context failures
          SYS_GL=""
          for d in /run/opengl-driver/lib /run/opengl-driver-32/lib; do
            if [ -d "$d" ]; then SYS_GL="$SYS_GL:$d"; fi
          done

          export LIBGL_DRIVERS_PATH="''${LIBGL_DRIVERS_PATH:-/run/opengl-driver/lib/dri}"
          export LD_LIBRARY_PATH="''${BASE_LD}''${SYS_GL}"
          export GDK_BACKEND="''${TRINITY_GDK_BACKEND:-x11}"

          PRISM_PROPS=()
          if [ "''${TRINITY_FORCE_SW:-0}" = "1" ]; then
            PRISM_PROPS+=(-Dprism.order=sw -Dprism.allowhidpi=false)
          else
            if [ "''${TRINITY_FORCE_ES2:-0}" = "1" ]; then
              PRISM_PROPS+=(-Dprism.es2.force=true -Dprism.order=es2)
            fi
          fi

          if [ -n "''${TRINITY_GLX_VENDOR:-}" ]; then
            export __GLX_VENDOR_LIBRARY_NAME="''${TRINITY_GLX_VENDOR}"
          fi

          if [ "''${TRINITY_HIDPI_OFF:-0}" = "1" ]; then
            PRISM_PROPS+=(-Dprism.allowhidpi=false -Dsun.java2d.uiScale=1)
            export GDK_SCALE=1
            export GDK_DPI_SCALE=1
          fi

          if [ "''${TRINITY_DEBUG:-0}" = "1" ]; then
            PRISM_PROPS+=(-Dprism.verbose=true -Djavafx.verbose=true)
            echo "[trinity] LD_LIBRARY_PATH=$LD_LIBRARY_PATH"
            echo "[trinity] LIBGL_DRIVERS_PATH=$LIBGL_DRIVERS_PATH"
          fi

          exec ${jdk}/bin/java \
            -Dprism.maxvram="''${TRINITY_MAXVRAM:-2G}" \
            -Dprism.forceGPU="''${TRINITY_FORCE_GPU:-true}" \
            -Djavafx.animation.fullspeed="''${TRINITY_JFX_FULLSPEED:-false}" \
            "''${PRISM_PROPS[@]}" \
            -cp "$CP" \
            edu.jhuapl.trinity.TrinityMain
        '';
      in {
        devShells.default = pkgs.mkShell {
          packages =
            [ jdk trinityRun ] ++
            (with pkgs; [
              gradle
              maven
              git
              zsh
              tmux

              # JavaFX / desktop native deps
              mesa
              libGL
              wayland
              xorg.libX11
              xorg.libXext
              xorg.libXrandr
              xorg.libXcursor
              xorg.libXi
              xorg.libXxf86vm
              xorg.libXtst
              xorg.libXrender
              xorg.libXinerama
              libxkbcommon
              gtk3
              glib
              pango
              cairo
              gdk-pixbuf
              at-spi2-core
              alsa-lib
              fontconfig
              freetype
              zlib

              # helpers
              pkg-config
              unzip
              which
              patchelf
            ]);

          JAVA_HOME = "${jdk}";
          MAVEN_OPTS = "-Xmx4g";
          GRADLE_OPTS = "-Xmx4g";
          _JAVA_AWT_WM_NONREPARENTING = "1";

          # NOTE: do NOT set LD_LIBRARY_PATH globally.
          shellHook = ''
            if [ -z "$IN_NIX_SHELL_ZSH" ]; then
              export IN_NIX_SHELL_ZSH=1
              export SHELL=${pkgs.zsh}/bin/zsh

              echo "[trinity] JAVA_HOME=$JAVA_HOME"
              command -v gradle >/dev/null && gradle --version | head -n 5 || true
              command -v mvn >/dev/null && mvn -v || true

              if [ -t 1 ] && [ -z "$TMUX" ] && [ -z "$NO_TMUX" ]; then
                session_name="''${TRINITY_TMUX_SESSION:-trinity}"
                exec ${pkgs.tmux}/bin/tmux -f "$HOME/.tmux.conf" new -A -s "$session_name"
              fi

              if [ -z "$TMUX" ]; then
                exec "$SHELL" -l
              fi
            fi
          '';
        };
      });
}

