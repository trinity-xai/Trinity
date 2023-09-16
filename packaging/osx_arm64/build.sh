#!/usr/bin/env bash
set -eux -o pipefail

app_version=${1:-$(date +%Y.%m.%d)}

# Setup Directories
script_dir=$(cd "$(dirname "$0")" ; pwd -P)
project_root_dir=$(cd "${script_dir}/../.." ; pwd -P)
image_dir="${project_root_dir}/build/image/Trinity-osx-arm64"
output_dir="${project_root_dir}/build/release/Trinity.app"
build_dir="${script_dir}/build_tmp"

echo "Copying from: ${image_dir}"
echo "Building Output: ${output_dir}"

# Cleanup previous build (if any)
rm -rf "${output_dir}"
rm -rf "${build_dir}"
mkdir "${build_dir}"

# Setup Meson Build
pushd "${script_dir}"
export DATA_DIR=${image_dir}
sed "s#YYYY.MM.DD#${app_version}#" "${script_dir}/Info.plist" > "${script_dir}/Info.plist.tmp"
meson rewrite kwargs set project / version "${app_version}"
meson setup --buildtype=release --prefix="${output_dir}" --bindir=Contents/MacOS "${build_dir}"
meson rewrite kwargs set project / version "YYYY.MM.DD"
popd

# Build it...
ninja -C "${build_dir}" install

# Cleanup
rm "${script_dir}/Info.plist.tmp"
rm -rf "${build_dir}"
