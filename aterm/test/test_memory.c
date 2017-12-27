#include <stdio.h>
#include <assert.h>
#include "aterm2.h"
#include "memory.h"

int main(int argc, char * argv[])
{
   ATerm stack = NULL;
   long n, num;

   ATinit(argc, argv, &stack);

   /* TODO need config.h so that AT_64BIT is defined in user code */
   printf("sizeof(header_type)=%ld sizeof(ATerm *)=%ld AT_64BIT=%d \n", sizeof(header_type), sizeof(ATerm), AT_64BIT);

   num = 1L << 0;
   n = AT_approximatepowerof2(num);
   /* printf("  ~powerof2(%ld)==%ld \n", num, n); */
   assert(n == 127);

   num = 1L << 1;
   n = AT_approximatepowerof2(num);
   assert(n == 127);

   num = 1L << 6;
   n = AT_approximatepowerof2(num);
   assert(n == 127);

   num = (1L << 7) - 1L;
   n = AT_approximatepowerof2(num);
   assert(n == num);

   num = (1L << 8) - 1L;
   n = AT_approximatepowerof2(num);
   assert(n == num);

   num = (1L << 16) - 1L;
   n = AT_approximatepowerof2(num);
   assert(n == num);

   num = (1L << 30) - 1L;
   n = AT_approximatepowerof2(num);
   assert(n == num);

   num = (1L << 31) - 1L;
   n = AT_approximatepowerof2(num);
   assert(n == num);

   num = (1L << 32) - 1L;
   n = AT_approximatepowerof2(num);
   assert(n == num);

   num = (1L << 33) - 1L;
   n = AT_approximatepowerof2(num);
   assert(n == num);

   return 0;
}
