
/**
 * aterm1.h: Definition of the level 1 interface
 * of the ATerm library.
 */

#ifndef ATERM1_H
#define ATERM1_H

#include <stdio.h>
#include <stdarg.h>
#include "encoding.h"
#include "abool.h"

#define AT_INT          0
#define AT_REAL         1
#define AT_APPL         2
#define AT_LIST         3
#define AT_PLACEHOLDER  4
#define AT_BLOB         5

typedef struct ATerm
{
	header_type   header;
	struct ATerm *next;
} *ATerm;

/** The following functions implement the operations of
  * the 'standard' ATerm interface, and should appear
  * in some form in every implementation of the ATerm
  * datatype.
  * The prefix AT is used to make these functions
  * relatively unique.
  */

ATerm ATmake(char *pattern, ...);
ATerm ATmatch(ATerm t, char *pattern, ...);
ATerm ATreadFromString(char *string);
ATerm ATreadFromTextFile(FILE *file);
ATerm ATreadFromBinaryFile(FILE *file);

/* int    ATgetType(ATerm t); */
#define ATgetType(t) GET_TYPE((t)->header)

ATbool ATwriteToTextFile(ATerm t, FILE *file);
ATbool ATwriteToBinaryFile(ATerm t, FILE *file);
char  *ATwriteToString(ATerm t);
void   ATsetAnnotation(ATerm t, ATerm label, ATerm anno);
ATerm  ATgetAnnotation(ATerm t, ATerm label);
void   ATremoveAnnotation(ATerm t, ATerm label);

void ATprotect(ATerm *atp);
void ATunprotect(ATerm *atp);

/**
  * We also define some functions that are specific
  * for the C implementation of ATerms, but are part
  * of the level 1 interface nevertheless.
  */

void ATinit(int argc, char *argv[],
            void (*error)(const char *format, va_list args),
			int *bottomOfStack);
void ATerror(const char *format, ...);
int  ATprintf(const char *format, ...);
int  ATfprintf(FILE *stream, const char *format, ...);
int  ATvfprintf(FILE *stream, const char *format, va_list args);

#endif
