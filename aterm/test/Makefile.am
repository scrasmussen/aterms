#include $(top_srcdir)/Makefile.meta

TESTS = test_memory test_lists primes randgen fib testsafio test_bafio stress
check_PROGRAMS = test_memory test_lists primes randgen termstats fib testsafio test_bafio stress

EXTRA_DIST = \
  error.trm \
  terms.dict \
  termstats.sh \
  test.trms \
  vararg1.c \
  vararg2.c \
  vararg3.c \
  testsafio.c \
  test_bafio.c \
  test_memory.c \
  test_lists.c

CLEANFILES = test.taf test.blob terms.c terms.h
BUILT_SOURCES = terms.c terms.h

AM_LDFLAGS  = -L$(top_builddir)/aterm
LDADD       = -lATerm
AM_CPPFLAGS = \
  -I$(top_srcdir) \
  -I$(top_srcdir)/aterm \
  -DSRCDIR="\"$(top_srcdir)/test/\""

test_memory_SOURCES = test_memory.c
test_memory_CFLAGS = -DNDEBUG -DAT_64BIT=${AT_64BIT} -DSIZEOF_INT=${SIZEOF_INT}

test_lists_SOURCES = test_lists.c
primes_SOURCES = primes.c
fib_SOURCES    = fib.c
randgen_SOURCES = randgen.c
termstats_SOURCES = termstats.c

stress_SOURCES = stress.c
nodist_stress_SOURCES = terms.c terms.h
stress_CFLAGS = -DNDEBUG -DAT_64BIT=${AT_64BIT} -DSIZEOF_INT=${SIZEOF_INT}

# To be used to find out which vararg-implementation is used on a new
# platform. Do not remove, but uncomment as needed.

#vararg1_SOURCES = vararg1.c
#vararg2_SOURCES = vararg2.c
#vararg3_SOURCES = vararg3.c

testsafio_SOURCES = testsafio.c
test_bafio_SOURCES = test_bafio.c

.dict.c:
	${top_builddir}/utils/dicttoc -file-prefix $* -dict $<
