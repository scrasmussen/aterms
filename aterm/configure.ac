AC_INIT([aterm], [3.0])
AC_PREREQ([2.59])

dnl +--------------------------------------------------------------------------+
dnl | Autotools setup
dnl +--------------------------------------------------------------------------+
AC_CONFIG_AUX_DIR([config])

AM_INIT_AUTOMAKE([-Wall -Werror foreign])

AM_CONFIG_HEADER([aterm_config.h])

AC_PROG_CC
AM_PROG_CC_C_O

AM_PROG_AR
AC_PROG_LIBTOOL

AC_SUBST([AT_64BIT],1)
AC_SUBST([SIZEOF_INT],4)

dnl AX_C_COMPILE_VALUE(sizeof(int), sizeof_int)

dnl m4_include(float-order.m4)
dnl m4_pattern_forbid([^META_])

dnl CURDATE=`date`
dnl AC_SUBST([CURDATE])

dnl AC_C_BIGENDIAN
dnl AX_C_FLOAT_WORD_ORDER_BIG

dnl AC_CHECK_SIZEOF([int])

dnl AC_CHECK_FUNCS([strdup])
dnl AC_CHECK_FUNCS([lrand48 srand48])

dnl AC_CONFIG_FILES([aterm.spec])

AC_CONFIG_HEADERS([config.h])
AC_CONFIG_FILES([
 Makefile
 aterm/Makefile
 utils/Makefile
 test/Makefile
])

AC_OUTPUT
