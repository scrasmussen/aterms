#include <stdio.h>
#include <assert.h>
#include "aterm2.h"

int main(int argc, char * argv[])
{
   ATerm stack = NULL;
   Symbol sym_int, sym_add;
   ATermAppl app_add;
   ATermList list_term, list_reverse;
   ATermPlaceholder ph_int;
   ATerm trm;
   int i;

   ATinit(argc, argv, &stack);

   list_term = ATempty;
   assert(ATgetLength(list_term) == 0);
   assert(ATisEmpty(list_term) == ATtrue);

   sym_int = ATmakeSymbol("int", 0, ATfalse);
   ph_int = ATmakePlaceholder((ATerm)ATmakeAppl0(sym_int));
   sym_add = ATmakeSymbol("add", 2, ATfalse);
   app_add = ATmakeAppl2(sym_add, (ATerm)ph_int, (ATerm)ph_int);
   list_term = ATinsert(list_term, (ATerm)app_add);
   assert(ATgetLength(list_term) == 1);
   assert(ATisEmpty(list_term) == ATfalse);

   list_term = ATremoveElementAt(list_term, 0);
   assert(ATgetLength(list_term) == 0);
   assert(ATisEmpty(list_term) == ATtrue);

   for (i = 0; i < 14; i++) {
      list_term = ATinsert(list_term, (ATerm)ATmakeInt(i));
      assert(ATgetLength(list_term) == i+1);
      assert(ATisEmpty(list_term) == ATfalse);
   }
   assert(13 == ATgetInt(ATgetFirst(list_term)));
   assert( 0 == ATgetInt(ATgetLast (list_term)));

   printf("list is %s \n", ATwriteToString((ATerm)list_term));
   list_reverse = ATreverse(list_term);
   printf("list is %s \n", ATwriteToString((ATerm)list_reverse));

   for (i = 0; i < 14; i++) {
      trm = ATelementAt(list_reverse, i);
      assert(i == ATgetInt(trm));
   }

   trm = ATgetLast(list_reverse);
   assert(13 == ATgetInt(trm));

   assert(ATisEqual(list_term, list_reverse) == ATfalse);
   list_reverse = ATreverse(list_reverse);
   assert(ATisEqual(list_term, list_reverse) == ATtrue);

   return 0;
}
