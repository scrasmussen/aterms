#ifndef BAFIO_H
#define BAFIO_H

#include "aterm1.h"

void   AT_getBafVersion(int *major, int *minor);
ATbool AT_interpretBaf(FILE *input, FILE *output);

#endif /* BAFIO_H */
