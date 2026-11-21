#!/usr/bin/env sh
set -eu

NDK="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}"
if [ -z "$NDK" ] || [ ! -d "$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin" ]; then
  echo "Android NDK Linux toolchain not found: ANDROID_NDK_HOME=$NDK" >&2
  exit 1
fi

CC="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android29-clang"
OUT="app/src/main/jniLibs/arm64-v8a"
mkdir -p "$OUT"

for variant in gb kk; do
  target="$OUT/libdhdrun_$variant.so"
  "$CC" -Os -fPIE -pie -Wl,--build-id=none -Wl,-z,relro,-z,now \
    -o "$target" native/hostjni/dhdrun.c
  chmod 755 "$target"
  readelf -h "$target" | grep -E 'Class:.*ELF64|Type:.*DYN'
  echo "Built $target"
done
