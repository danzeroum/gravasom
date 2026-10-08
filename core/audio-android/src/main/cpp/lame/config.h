/* config.h minimo para compilar libmp3lame 3.100 no Android (NDK/clang), sem autotools. */
#ifndef GRAVASOM_LAME_CONFIG_H
#define GRAVASOM_LAME_CONFIG_H
#define STDC_HEADERS 1
#define HAVE_STDINT_H 1
#define HAVE_INTTYPES_H 1
#define HAVE_LIMITS_H 1
#define HAVE_STRING_H 1
#define HAVE_MEMORY_H 1
#define HAVE_STDLIB_H 1
#define HAVE_ERRNO_H 1
#define HAVE_FCNTL_H 1
#define HAVE_UNISTD_H 1
#define HAVE_SYS_TIME_H 1
#define HAVE_SYS_TYPES_H 1
#define HAVE_SYS_STAT_H 1
#define HAVE_STRCHR 1
#define HAVE_MEMCPY 1
#define HAVE_STRTOL 1
#define SIZEOF_DOUBLE 8
#define SIZEOF_FLOAT 4
#define SIZEOF_INT 4
#define SIZEOF_LONG 8
#define SIZEOF_LONG_LONG 8
#define SIZEOF_SHORT 2
#define SIZEOF_UNSIGNED_INT 4
#define SIZEOF_UNSIGNED_LONG 8
#define SIZEOF_UNSIGNED_LONG_LONG 8
#define SIZEOF_UNSIGNED_SHORT 2
#define LAME_LITTLE_ENDIAN 1
#define PACKAGE "lame"
#define VERSION "3.100"
#define HAVE_NAMESPACES 1
#define HAVE_FLOAT_H 1
#define HAVE_LOCALE_H 1
#define ieee754_float32_t float
#define ieee754_float64_t double
#define ieee854_float80_t long double
#endif
