#include "aterm1.h"
#include <stdio.h>

int main(int argc, char **argv){
   ATerm bottomOfStack;
   ATerm program;
   FILE * file;

   ATinit(argc, argv, &bottomOfStack);
  	
   file = fopen("test.baf", "rb");
   if (!file) {
      printf("ERROR: in test_bafio, path is %s\n", SRCDIR "/test/" "test.baf");
      ATerror("cannot open file \"test.baf\"");
   }

   program = ATreadFromBinaryFile(file);
   /*printf("program term is:\n%s\n\n", ATwriteToString(program));*/

   return 0;
}
