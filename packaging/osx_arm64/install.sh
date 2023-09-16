#!/usr/bin/env sh

mkdir -p "${MESON_INSTALL_PREFIX}/Contents/MacOS"
cp -r "${DATA_DIR}"/* "${MESON_INSTALL_PREFIX}/Contents/MacOS"
