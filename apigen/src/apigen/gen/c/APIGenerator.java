package apigen.gen.c;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import apigen.adt.ADT;
import apigen.adt.Alternative;
import apigen.adt.AlternativeList;
import apigen.adt.Field;
import apigen.adt.ListType;
import apigen.adt.Location;
import apigen.adt.SeparatedListType;
import apigen.adt.Step;
import apigen.adt.Type;
import apigen.adt.api.types.Separators;
import apigen.gen.GenerationException;
import apigen.gen.StringConversions;
import apigen.gen.TypeConverter;
import aterm.AFun;
import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermInt;
import aterm.ATermList;
import aterm.ATermPlaceholder;
import aterm.ATermReal;

public class APIGenerator extends CGenerator {
	private ADT adt;

	private String apiName;

	private String prefix;

	private String macro;

	public AFunRegister afunRegister;

	public APIGenerator(CGenerationParameters params, ADT adt) {
		super(params);
		this.adt = adt;
		this.apiName = params.getApiName();
		this.prefix = params.getPrefix();
		afunRegister = new AFunRegister();
	}

	protected void generate() throws GenerationException {
		genPrologue();
		genStaticConversions();
		genTypes(adt);
		//		genBottomSorts();
		genInitFunction();
		genProtectFunctions(adt);
		genTermConversions(adt);
		genListsApi(adt);
		genConstructors(adt);
		genIsEquals(adt);
		genAccessors(adt);
		genSortVisitors(adt);
		genEpilogue();
	}

	private void genProtectFunctions(ADT adt) {
		bothPrintFoldOpen("(un)protect functions");
		Iterator types = adt.typeIterator();

		while (types.hasNext()) {
			Type type = (Type) types.next();
			String id = StringConversions.makeIdentifier(type.getId());
			String typeName = buildTypeName(type.getId());

			genProtect(id, typeName);
			genUnprotect(id, typeName);
		}

		bothPrintFoldClose();
	}

	private void genProtect(String id, String typeName) {
		String functionName = prefix + "protect" + id;
		bothPrint("void " + functionName + "(" + typeName + " *arg)");
		hprintln(";");
		//println();
		println(" {");
		println("  ATprotect((ATerm*)((void*) arg));");
		println("}");
		println();
	}

	private void genUnprotect(String id, String typeName) {
		String functionName = prefix + "unprotect" + id;
		bothPrint("void " + functionName + "(" + typeName + " *arg)");
		hprintln(";");
		//println();
		println(" {");
		println("  ATunprotect((ATerm*)((void*) arg));");
		println("}");
		println();
	}

	private void genStaticConversions() {
		printFoldOpen("conversion functions");
		genStaticStringToChars();
		genStaticByteToChar();
		genStaticCharsToString();
		genStaticCharToByte();
		printFoldClose();
		println();
	}

	private void genStaticCharToByte() {
		println("char " + prefix + "charToByte(ATerm arg) {");
		//		println("{");
		println("    return (char) ATgetInt((ATermInt) arg);");
		println("}");
		println();
	}

	private void genStaticCharsToString() {
		println("char *" + prefix + "charsToString(ATerm arg) {");
		//		println("{");
		println("  ATermList list = (ATermList) arg;");
		println("  int len = ATgetLength(list);");
		println("  int i;");
		println("  char *str;");
		println();
		println("  str = (char *) malloc(len+1);");
		println("  if (str == NULL) {");
		println("      return NULL;");
		println("  }");
		println();
		println("  for (i = 0; !ATisEmpty(list); list = ATgetNext(list), i++) {");
		println("    str[i] = (char) ATgetInt((ATermInt) ATgetFirst(list));");
		println("  }");
		println("  str[i] = '\\0';");
		println();
		println("  return str;");
		println("}");
		println();
	}

	private void genStaticStringToChars() {
		println("ATerm " + prefix + "stringToChars(const char *str) {");
		//		println("{");
		println("  int len = strlen(str);");
		println("  int i;");
		println("  ATermList result = ATempty;");
		println();
		println("  for (i = len - 1; i >= 0; i--) {");
		println("    result = ATinsert(result, (ATerm) ATmakeInt(str[i]));");
		println("  }");
		println();
		println("  return (ATerm) result;");
		println("}");
		println();
	}

	private void genStaticByteToChar() {
		println("ATerm " + prefix + "byteToChar(char ch) {");
		//		println("{");
		println("    return (ATerm) ATmakeInt(ch);");
		println("}");
		println();
	}

	private void genBottomSorts() {
		// disabled because it clashes (rather than generating a default
		// implementation) with prologue.
		Iterator types = adt.bottomTypeIterator();
		TypeConverter conv = new TypeConverter(new CTypeConversions(prefix));

		if (types.hasNext()) {
			hprintFoldOpen("definition of bottom types");
			while (types.hasNext()) {
				String type = (String) types.next();

				if (!conv.isReserved(type)) {
					System.err.println("Warning: undefined (bottom) type "
							+ type + " defaults to ATerm");
					hprintln("typedef ATerm " + buildTypeName(type) + ";");
				}
			}

			hprintFoldClose();
			hprintln();
		}
	}

	private void genListsApi(ADT adt) {
		Iterator types = adt.typeIterator();

		bothPrintFoldOpen("list functions");
		while (types.hasNext()) {
			Type type = (Type) types.next();

			if (type instanceof ListType) {
				if (type instanceof SeparatedListType) {
					genSeparatedListApi((SeparatedListType) type);
				} else {
					genListApi((ListType) type);
				}
			}
		}
		bothPrintFoldClose();
	}

	private void genListApi(ListType type) {
		String typeName = buildTypeName(type.getId());
		String typeId = StringConversions.makeIdentifier(type.getId());
		String elementTypeName = buildTypeName(type.getElementType());
		String elementTypeId = StringConversions.makeIdentifier(type
				.getElementType());
		genGetLength(typeId, typeName);
		genReverse(typeId, typeName);
		genAppend(type, typeId, typeName, elementTypeName);
		genConcat(typeId, typeName);
		genSlice(typeId, typeName, "start", "end");
		genGetElementAt(type, typeId, typeName, elementTypeId, elementTypeName,
				"index");
		genReplaceElementAt(type, typeId, typeName, elementTypeId,
				elementTypeName, "index");
		genListMakes(type, typeId, typeName, elementTypeName);
	}

	private void genSeparatedListApi(SeparatedListType type) {
		String typeName = buildTypeName(type.getId());
		String typeId = StringConversions.makeIdentifier(type.getId());
		String elementTypeName = buildTypeName(type.getElementType());
		String elementTypeId = StringConversions.makeIdentifier(type
				.getElementType());
		int seps = type.getSeparators().getLength();

		genGetSeparatedLength(typeId, typeName, type.getSeparators());
		genSeparatedReverse(type, typeId, typeName);
		genSeparatedAppend(type, typeId, typeName, elementTypeName);
		genSeparatedConcat(type, typeId, typeName, elementTypeName);
		genSlice(typeId, typeName, genConvertSeparatedIndex("start", seps),
				genConvertSeparatedIndex("end", seps));
		genGetElementAt(type, typeId, typeName, elementTypeId, elementTypeName,
				genConvertSeparatedIndex("index", seps));
		genReplaceElementAt(type, typeId, typeName, elementTypeId,
				elementTypeName, genConvertSeparatedIndex("index", seps));
		genSeparatedListMakes(type, typeId, typeName, elementTypeName);
	}

	private void genSeparatedReverse(SeparatedListType type, String typeId,
			String typeName) {

		String decl = typeName + " " + prefix + "reverse" + typeId + "("
				+ typeName + " arg)";
		String listNext = "      list = ATgetNext(list);";
		int sepCount = type.getSeparators().getLength();

		hprintln(decl + ";");
		println(decl + " {");
		println("  ATermList list = (ATermList) arg;");
		println("  ATerm head;");

		for (int i = 0; i < sepCount; i++) {
			println("  ATerm sep" + i + ";");
		}

		println("  ATermList result;");
		println();
		println(" if (ATisEmpty(list) || ATgetLength(list) == 1) {");
		println("    return arg;");
		println("  }");
		println();
		println("  result = ATmakeList1(ATgetFirst(list));");
		println(listNext);
		for (int i = 0; i < sepCount; i++) {
			println("  sep" + i + " = ATgetFirst(list);");
			println("  list = ATgetNext(list);");
		}
		println();
		println("  while (!ATisEmpty(list)) {");
		for (int i = sepCount - 1; i >= 0; i--) {
			println("    result = ATinsert(result, sep" + i + ");");
		}
		println();
		println("   head = ATgetFirst(list);");
		println("   result = ATinsert(result, head);");
		println("    list = ATgetNext(list);");
		println();
		println("   if (!ATisEmpty(list)) {");
		for (int i = 0; i < sepCount; i++) {
			println("  sep" + i + " = ATgetFirst(list);");
			println("  list = ATgetNext(list);");
		}
		println("   }");
		println("  }");
		println();
		println("  return (" + typeName + ") result;");
		println("}");

	}

	private void genListMakes(ListType type, String typeId, String typeName,
			String elementTypeName) {
		for (int arity = 2; arity <= 6; arity++) {
			genListMake(type, arity, typeId, typeName, elementTypeName);
		}
	}

	private void genSeparatedListMakes(SeparatedListType type, String typeId,
			String typeName, String elementTypeName) {
		for (int arity = 2; arity <= 6; arity++) {
			genSeparatedListMake(type, arity, typeId, typeName, elementTypeName);
		}
	}

	private void genListMake(ListType type, int arity, String typeId,
			String typeName, String elementTypeName) {
		String decl = typeName + " " + prefix + "make" + typeId + arity + "(";
		for (int i = 1; i < arity; i++) {
			decl = decl + elementTypeName + " elem" + i + ", ";
		}
		decl = decl + elementTypeName + " elem" + arity + ")";
		hprintln(decl + ";");
		println(decl + " {");
		print("  return (" + typeName + ") ATmakeList" + arity + "(");

		String conversion;
		for (int i = 1; i < arity; i++) {
			conversion = genBuiltinToATerm(type.getElementType(), "elem"
					+ arity);
			print("(ATerm) " + conversion + ", ");
		}
		conversion = genBuiltinToATerm(type.getElementType(), "elem" + arity);
		println("(ATerm) " + conversion + ");");
		println("}");
	}

	private void genSeparatedListMake(SeparatedListType type, int arity,
			String typeId, String typeName, String elementTypeName) {

		String decl = typeName + " " + prefix + "make" + typeId + arity + "(";

		decl += buildFormalSeparatorArgs(type);

		for (int i = 1; i < arity; i++) {
			decl = decl + elementTypeName + " elem" + i + ", ";
		}
		decl = decl + elementTypeName + " elem" + arity + ")";
		hprintln(decl + ";");
		println(decl + " {");

		String manyFunction = prefix + "make" + typeId + "Many";
		if (arity == 2) {
			String singleFunction = prefix + "make" + typeId + "Single";
			println("  return " + manyFunction + "(elem1, "
					+ buildActualSeparatorArgs(type) + singleFunction
					+ "(elem2));");
		} else {
			String prevFunction = prefix + "make" + typeId + (arity - 1);
			print("  return " + manyFunction + "(elem1, "
					+ buildActualSeparatorArgsForMakeMany(type) + prevFunction
					+ "(" + buildActualSeparatorArgsForMakeMany(type));

			for (int i = 2; i < arity; i++) {
				print("elem" + i + ", ");
			}
			println("elem" + arity + "));");
		}
		println("}");
	}

	private void genGetElementAt(ListType type, String typeId, String typeName,
			String elementTypeId, String elementTypeName, String index) {
		// TODO: remove superfluous arguments
		String conversion = genATermToBuiltin(type.getElementType(),
				"ATelementAt((ATermList) arg," + index + ")");
		String decl = elementTypeName + " " + prefix + "get" + typeId
				+ elementTypeId + "At(" + typeName + " arg, int index)";
		hprintln(decl + ";");
		println(decl + " {");
		println(" return (" + elementTypeName + ")" + conversion + ";");
		println("}");
	}

	private void genReplaceElementAt(ListType type, String typeId,
			String typeName, String elementTypeId, String elementTypeName,
			String index) {
		String conversion = genBuiltinToATerm(type.getElementType(), "elem");
		String decl = typeName + " " + prefix + "replace" + typeId
				+ elementTypeId + "At(" + typeName + " arg, " + elementTypeName
				+ " elem, int index)";
		hprintln(decl + ";");
		println(decl + " {");
		println(" return (" + typeName
				+ ") ATreplace((ATermList) arg, (ATerm) " + conversion + ", "
				+ index + ");");
		println("}");
	}

	private void genReverse(String typeId, String typeName) {
		String decl = typeName + " " + prefix + "reverse" + typeId + "("
				+ typeName + " arg)";
		hprintln(decl + ";");
		println(decl + " {");
		println("  return (" + typeName + ") ATreverse((ATermList) arg);");
		println("}");
	}

	private void genAppend(ListType type, String typeId, String typeName,
			String elementTypeName) {
		// TODO: remove superfluous arguments
		String conversion = genBuiltinToATerm(type.getElementType(), "elem");

		String decl = typeName + " " + prefix + "append" + typeId + "("
				+ typeName + " arg, " + elementTypeName + " elem)";
		hprintln(decl + ";");
		println(decl + " {");
		println("  return (" + typeName
				+ ") ATappend((ATermList) arg, (ATerm) " + conversion + ");");
		println("}");
	}

	private void genConcat(String typeId, String typeName) {
		String decl = typeName + " " + prefix + "concat" + typeId + "("
				+ typeName + " arg0, " + typeName + " arg1)";
		hprintln(decl + ";");
		println(decl + " {");
		println("  return (" + typeName
				+ ") ATconcat((ATermList) arg0, (ATermList) arg1);");
		println("}");
	}

	private void genSeparatedConcat(SeparatedListType type, String typeId,
			String typeName, String elementTypeName) {
		String decl = typeName + " " + prefix + "concat" + typeId + "("
				+ typeName + " arg0, " + buildFormalSeparatorArgs(type)
				+ typeName + " arg1)";
		hprintln(decl + ";");
		println(decl + " {");
		println("  if (ATisEmpty((ATermList) arg0)) {");
		println("    return arg1;");
		println("  }");
		// First we 'fake' a new list insertion using the first element of arg0
		// as a dummy value
		String conversion = genATermToBuiltin(type.getElementType(),
				"ATgetFirst((ATermList) arg0)");

		println("  arg1 = " + prefix + "make" + typeId + "Many(("
				+ elementTypeName + ")" + conversion + ", "
				+ buildActualSeparatorArgsForMakeMany(type) + " arg1);");
		// Then we remove the dummy value in type unsafe mode to get a list
		// that starts with the expected separators
		println("  arg1 = (" + typeName + ") ATgetNext((ATermList) arg1);");
		// Now we can concatenate
		println("  return (" + typeName
				+ ") ATconcat((ATermList) arg0, (ATermList) arg1);");
		println("}");
	}

	private void genSeparatedAppend(SeparatedListType type, String typeId,
			String typeName, String elementTypeName) {
		String decl = typeName + " " + prefix + "append" + typeId + "("
				+ typeName + " arg0, " + buildFormalSeparatorArgs(type)
				+ elementTypeName + " arg1)";
		hprintln(decl + ";");
		println(decl + " {");
		println("  return " + prefix + "concat" + typeId + "(arg0, "
				+ buildActualSeparatorArgsForMakeMany(type) + prefix + "make"
				+ typeId + "Single(arg1));");
		println("}");
	}

	private void genSlice(String typeId, String typeName, String startIndex,
			String endIndex) {
		String decl = typeName + " " + prefix + "slice" + typeId + "("
				+ typeName + " arg, int start, int end)";
		hprintln(decl + ";");
		println(decl + " {");
		println("  return (" + typeName + ") ATgetSlice((ATermList) arg, "
				+ startIndex + ", " + endIndex + ");");
		println("}");
	}

	private String genConvertSeparatedIndex(String varname, int countSeps) {
		return varname + " * " + (countSeps + 1);
	}

	private void genGetSeparatedLength(String typeId, String typeName,
			Separators seps) {
		String decl = "int " + prefix + "get" + typeId + "Length (" + typeName
				+ " arg)";

		hprintln(decl + ";");
		println(decl + " {");
		println("  if (ATisEmpty((ATermList) arg)) {");
		println("    return 0;");
		println("  }");
		println("  return (ATgetLength((ATermList) arg) / "
				+ (seps.getLength() + 1) + ") + 1;");
		println("}");
	}

	private void genPrologue() {
		genHeaderIfDefWrapper();
		genHeaderIncludes();
		copyPrologueFileToHeader();
		genSourceIncludes();
	}

	private void genSourceIncludes() {
		println("#include <assert.h>");
		println();
		println("#include <aterm2.h>");
		println("#include <deprecated.h>");
		println("#include \"" + apiName + ".h\"");
		println();
	}

	private void copyPrologueFileToHeader() {
		String prologue = getCGenerationParameters().getPrologue();
		if (prologue != null) {
			hprintFoldOpen("prologue");
			InputStream stream;
			try {
				stream = new FileInputStream(prologue);
				while (true) {
					int b = stream.read();
					if (b == -1) {
						break;
					}
					hwrite(b);
				}
			} catch (FileNotFoundException e) {
				System.out.println("Could not find prologue file");
				System.exit(1);
			} catch (IOException e) {
				System.out.println("Could not read from prologue file");
				System.exit(1);
			}

			hprintFoldClose();
		}
	}

	private void genHeaderIncludes() {
		hprintln();
		hprintFoldOpen("includes");
		hprintln("#include <stdlib.h>");
		hprintln("#include <string.h>");
		hprintln("#include <aterm1.h>");
		hprintln("#include \"" + apiName + "_dict.h\"");
		hprintFoldClose();
		hprintln();
	}

	private void genHeaderIfDefWrapper() {
		macro = "_" + apiName.toUpperCase() + "_H";
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < macro.length(); i++) {
			if (Character.isLetterOrDigit(macro.charAt(i))) {
				buf.append(macro.charAt(i));
			} else {
				buf.append('_');
			}
		}
		macro = buf.toString();

		hprintln("#ifndef " + macro);
		hprintln("#define " + macro);
	}

	private void genTypes(ADT api) {
		Iterator types = api.typeIterator();
		bothPrintFoldOpen("typedefs");
		while (types.hasNext()) {
			Type type = (Type) types.next();
			String id = buildTypeName(type);
			hprintln("typedef struct _" + id + " *" + id + ";");
			println("typedef struct ATerm _" + id + ";");
		}
		bothPrintFoldClose();
		bothPrintln();
	}

	private void genInitFunction() {
		String decl = "void " + prefix + "init"
				+ StringConversions.makeCapitalizedIdentifier(apiName)
				+ "Api(void)";
		hprintln(decl + ";");

		printFoldOpen(decl);
		println(decl + " {");
		//		println("{");
		String dictInitfunc = buildDictInitFunc(apiName);
		println("  init_" + dictInitfunc + "_dict();");
		println("}");
		printFoldClose();
		println();
		hprintln();
	}

	private String buildDictInitFunc(String name) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isLetterOrDigit(c)) {
				buf.append(c);
			} else {
				buf.append('_');
			}
		}
		return buf.toString();
	}

	private void genTermConversions(ADT api) {
		Iterator types = api.typeIterator();
		bothPrintFoldOpen("term conversion functions");
		while (types.hasNext()) {
			Type type = (Type) types.next();
			String type_id = StringConversions.makeIdentifier(type.getId());
			String type_name = buildTypeName(type);

			genFromTerm(type_id, type_name);
			genToTerm(type_id, type_name);
		}
		bothPrintFoldClose();
	}

	private void genToTerm(String type_id, String type_name) {
		String decl;

		if (getCGenerationParameters().isTermCompatibility()) {
			String old_macro = "#define " + prefix + "makeTermFrom" + type_id
					+ "(t)" + " (" + prefix + type_id + "ToTerm(t))";
			hprintln(old_macro);
		}
		decl = "ATerm " + prefix + type_id + "ToTerm(" + type_name + " arg)";
		hprintln(decl + ";");
		printFoldOpen(decl);
		println(decl + " {");
		//		println("{");
		println("  return (ATerm)arg;");
		println("}");
		printFoldClose();
	}

	private void genFromTerm(String type_id, String type_name) {
		if (getCGenerationParameters().isTermCompatibility()) {
			String old_macro = "#define " + prefix + "make" + type_id
					+ "FromTerm(t)" + " (" + prefix + type_id + "FromTerm(t))";
			hprintln(old_macro);
		}
		String decl = type_name + " " + prefix + type_id + "FromTerm(ATerm t)";
		hprintln(decl + ";");
		printFoldOpen(decl);
		println(decl + " {");
		//		println("{");
		println("  return (" + type_name + ")t;");
		println("}");
		printFoldClose();
	}

	private void genIsEquals(ADT api) {
		Iterator types = api.typeIterator();
		bothPrintFoldOpen("equality functions");

		while (types.hasNext()) {
			Type type = (Type) types.next();
			String type_id = StringConversions.makeIdentifier(type.getId());
			String type_name = buildTypeName(type);

			String decl = "ATbool " + prefix + "isEqual" + type_id + "("
					+ type_name + " arg0, " + type_name + " arg1)";

			hprintln(decl + ";");

			println(decl + " {");
			//			println("{");
			println("  return ATisEqual((ATerm)arg0, (ATerm)arg1);");
			println("}");
			if (types.hasNext()) {
				println();
			}
		}

		bothPrintFoldClose();
	}

	private String genConstructorImpl(ATerm pattern) throws GenerationException {
		String result = null;

		switch (pattern.getType()) {
		case ATerm.REAL:
			result = buildRealConstructorImpl(pattern);
			break;
		case ATerm.INT:
			result = buildIntConstructorImpl(pattern);
			break;
		case ATerm.BLOB:
			throw new GenerationException("blobs are not supported");
		case ATerm.LIST:
			result = buildListConstructorImpl(pattern);
			break;
		case ATerm.APPL:
			result = buildApplConstructorImpl(pattern);
			break;
		case ATerm.PLACEHOLDER:
			result = buildPlaceholderSubstitution(pattern);
			break;
		}

		return result;
	}

	private String buildPlaceholderSubstitution(ATerm pattern) {
		ATermAppl hole = (ATermAppl) ((ATermPlaceholder) pattern)
				.getPlaceholder();
		String name = StringConversions.makeIdentifier(hole.getName());
		String type = hole.getArgument(0).toString();
		TypeConverter converter = new TypeConverter(
				new CTypeConversions(prefix));

		return "(ATerm) " + converter.makeBuiltinToATermConversion(type, name);
	}

	private String buildApplConstructorImpl(ATerm pattern)
			throws GenerationException {
		String result;
		ATermAppl appl = (ATermAppl) pattern;
		int arity = appl.getArity();
		if (arity > 0) {
			ATerm last = appl.getArgument(arity - 1);
			if (last.getType() == ATerm.PLACEHOLDER) {
				ATerm ph = ((ATermPlaceholder) last).getPlaceholder();
				if (ph.getType() == ATerm.LIST) {
					throw new RuntimeException(
							"list placeholder not supported in"
									+ " argument list of function application");
				}
			}
		}
		result = "(ATerm)ATmakeAppl"
				+ (arity <= 6 ? String.valueOf(arity) : "") + "(";
		result += prefix + afunRegister.lookup(appl.getAFun());
		for (int i = 0; i < arity; i++) {
			ATerm arg = appl.getArgument(i);
			result += ", " + genConstructorImpl(arg);
		}
		result += ")";
		return result;
	}

	private String buildListConstructorImpl(ATerm pattern)
			throws GenerationException {
		String result = null;
		ATermList list = (ATermList) pattern;
		int length = list.getLength();
		if (length == 0) {
			result = "ATempty";
		} else {
			ATerm last = list.elementAt(length - 1);
			if (last.getType() == ATerm.PLACEHOLDER) {
				ATerm ph = ((ATermPlaceholder) last).getPlaceholder();
				if (ph.getType() == ATerm.LIST) {
					ATermAppl field = (ATermAppl) (((ATermList) ph).getFirst());
					result = "(ATermList)"
							+ StringConversions.makeIdentifier(field.getName());
				}
			}
			if (result == null || result.length() == 0) {
				result = "ATmakeList1(" + genConstructorImpl(last) + ")";
			}
			for (int i = length - 2; i >= 0; i--) {
				ATerm elem = list.elementAt(i);
				result = "ATinsert(" + result + ", " + genConstructorImpl(elem)
						+ ")";
			}
		}
		result = "(ATerm)" + result;
		return result;
	}

	private String buildIntConstructorImpl(ATerm pattern) {
		return "(ATerm)ATmakeInt(" + ((ATermInt) pattern).getInt() + ")";
	}

	private String buildRealConstructorImpl(ATerm pattern) {
		return "(ATerm)ATmakeReal(" + ((ATermReal) pattern).getReal() + ")";
	}

	private void genConstructors(ADT api) throws GenerationException {
		Iterator types = api.typeIterator();
		bothPrintFoldOpen("constructors");

		while (types.hasNext()) {
			Type type = (Type) types.next();
			String type_name = buildTypeName(type);

			Iterator alts = type.alternativeIterator();
			while (alts.hasNext()) {
				Alternative alt = (Alternative) alts.next();
				String decl = buildConstructorDecl(type, alt);
				hprintln(decl + ";");

				printFoldOpen(decl.toString());
				println(decl + " {");
				//				println("{");

				genDealWithSeparatedListException(type, StringConversions
						.makeCapitalizedIdentifier(type.getId()), alt);
				genAlternativeConstructorBody(type_name, alt);

				println("}");
				printFoldClose();
			}
		}
		bothPrintFoldClose();
	}

	private void genDealWithSeparatedListException(Type type, String type_name,
			Alternative alt) {
		if (type instanceof SeparatedListType && alt.getId() == "many") {
			println("  if (" + prefix + "is" + type_name + "Empty(tail)) {");
			println("    return " + prefix + "make" + type_name
					+ "Single(head);");
			println("  }");
		}
	}

	private void genAlternativeConstructorBody(String type_name, Alternative alt)
			throws GenerationException {
		println("  return (" + type_name + ")"
				+ genConstructorImpl(alt.getPattern()) + ";");
	}

	private void genAccessors(ADT api) throws GenerationException {
		Iterator types = api.typeIterator();
		while (types.hasNext()) {
			Type type = (Type) types.next();
			String type_name = buildTypeName(type);
			bothPrintFoldOpen(type_name + " accessors");

			genTypeIsValid(type);
			genIsAlts(type);
			genHasFields(type);
			genGetFields(type);
			genSetFields(type);

			bothPrintFoldClose();
		}
	}

	private void genSetFields(Type type) {
		Iterator fields = type.fieldIterator();
		while (fields.hasNext()) {
			Field field = (Field) fields.next();
			genSetField(type, field);
		}
	}

	private void genGetFields(Type type) {
		if (type instanceof SeparatedListType) {
			genSeparatedListGetters((SeparatedListType) type);
		} else {
			Iterator fields = type.fieldIterator();
			while (fields.hasNext()) {
				Field field = (Field) fields.next();
				genGetField(type, field);
			}
		}
	}

	private void genHasFields(Type type) {
		Iterator fields = type.fieldIterator();
		while (fields.hasNext()) {
			Field field = (Field) fields.next();
			genHasField(type, field);
		}
	}

	private void genIsAlts(Type type) throws GenerationException {
		Iterator alts = type.alternativeIterator();
		while (alts.hasNext()) {
			Alternative alt = (Alternative) alts.next();
			genIsAlt(type, alt);
		}
	}

	private void genSortVisitors(ADT api) {
		Iterator types = api.typeIterator();
		bothPrintFoldOpen("sort visitors");

		while (types.hasNext()) {
			Type type = (Type) types.next();
			String type_id = StringConversions.makeIdentifier(type.getId());
			String type_name = buildTypeName(type);

			StringBuffer decl_buf = new StringBuffer();
			String visitor_name = prefix + "visit" + type_id;
			decl_buf.append(type_name);
			decl_buf.append(" ");
			decl_buf.append(visitor_name);
			decl_buf.append("(");
			decl_buf.append(type_name);
			decl_buf.append(" arg");
			Iterator fields = type.fieldIterator();
			while (fields.hasNext()) {
				Field field = (Field) fields.next();
				if (!field.getType().equals(type.getId())) {
					decl_buf.append(", ");
					decl_buf.append(genAcceptor(field));
				}
			}
			decl_buf.append(")");
			String decl = decl_buf.toString();

			hprintln(decl + ";");

			printFoldOpen(decl);
			println(decl + " {");
			//			println("{");

			Iterator alts = type.alternativeIterator();
			while (alts.hasNext()) {
				Alternative alt = (Alternative) alts.next();
				genSortVisitorAltImpl(type, alt);
			}

			println("  ATabort(\"not a " + type_id + ": %t\\n\", arg);");
			println("  return (" + type_name + ")NULL;");

			println("}");
			printFoldClose();
		}

		bothPrintFoldClose();
	}

	private String genAcceptor(Field field) {
		String type = buildTypeName(field.getType());
		String name = "accept"
				+ StringConversions.makeCapitalizedIdentifier(field.getId());

		return type + " (*" + name + ")(" + type + ")";
	}

	private void genSortVisitorAltImpl(Type type, Alternative alt) {
		String type_id = StringConversions.makeIdentifier(type.getId());
		String cons_name = buildConstructorName(type, alt);

		println("  if (" + buildIsAltName(type, alt) + "(arg)) {");
		print("    return " + cons_name + "(");
		Iterator fields = type.altFieldIterator(alt.getId());
		while (fields.hasNext()) {
			Field field = (Field) fields.next();
			println("");
			print("        ");
			String getter_name = buildGetterName(type, field);
			if (field.getType().equals(type.getId())) {
				String visitor_name = prefix + "visit" + type_id;
				print(visitor_name + "(" + getter_name + "(arg)");
				Iterator params = type.fieldIterator();
				while (params.hasNext()) {
					Field param = (Field) params.next();
					if (!param.getType().equals(type.getId())) {
						print(", ");
						print("accept"
								+ StringConversions
										.makeCapitalizedIdentifier(param
												.getId()));
					}
				}
				print(")");
			} else {
				String acceptor_name = "accept"
						+ StringConversions.makeCapitalizedIdentifier(field
								.getId());
				print(acceptor_name + " ? " + acceptor_name + "(" + getter_name
						+ "(arg))");
				print(" : " + getter_name + "(arg)");
			}
			if (fields.hasNext()) {
				print(",");
			}
		}
		println(");");
		println("  }");
	}

	private void genSeparatedListGetters(SeparatedListType type) {
		genSeparatedListGetTail(type);
		genGetField(type, type.getManyField(type.getHeadFieldId()));

		Iterator seps = type.separatorFieldIterator();
		while (seps.hasNext()) {
			Field sep = (Field) seps.next();
			genGetField(type, sep);
		}
	}

	private void genSeparatedListGetTail(SeparatedListType type) {
		Alternative alt = type.getManyAlternative();
		String type_name = buildTypeName(type);
		String isSingle = buildIsAltName(type, type.getSingleAlternative());
		String isMany = buildIsAltName(type, type.getManyAlternative());
		String isEmpty = buildIsAltName(type, type.getEmptyAlternative());

		String decl = type_name
				+ " "
				+ buildGetterName(type, type
						.getManyField(type.getTailFieldId())) + "(" + type_name
				+ " arg)";

		hprintln(decl + ";");

		printFoldOpen(decl);
		println(decl + " {");
		//		println("{");
		println("  assert(!" + isEmpty
				+ "(arg) && \"getTail on an empty list\");");
		println("  if (" + isSingle + "(arg)) {");
		println("    return (" + type_name + ") "
				+ buildConstructorName(type, type.getEmptyAlternative())
				+ "();");
		println("  }");
		println("  else {");
		Field tail = type.getManyField(type.getTailFieldId());
		genGetFieldBody(type, tail, buildTypeName(tail.getType()));
		println("  }");
		println("}");
		printFoldClose();
	}

	private void genGetField(Type type, Field field) {
		String type_name = buildTypeName(type);
		String field_type_name = buildTypeName(field.getType());
		String decl = field_type_name + " " + buildGetterName(type, field)
				+ "(" + type_name + " arg)";

		hprintln(decl + ";");

		printFoldOpen(decl);
		println(decl + " {");
		//		println("{");
		genGetFieldBody(type, field, field_type_name);

		println("}");
		printFoldClose();
	}

	/**
	 * @param type
	 * @param field
	 * @param field_type_name
	 */
	private void genGetFieldBody(Type type, Field field, String field_type_name) {
		Iterator locs = field.locationIterator();
		boolean first = true;

		while (locs.hasNext()) {
			Location loc = (Location) locs.next();
			print("  ");
			if (first) {
				first = false;
			} else {
				print("else ");
			}
			if (locs.hasNext()) {
				println("if (" + buildIsAltName(type, loc.getAltId())
						+ "(arg)) {");
			} else {
				println("");
			}
			print("    return (" + field_type_name + ")");
			Iterator steps = loc.stepIterator();
			String type_getter = genATermToBuiltin(field.getType(),
					genGetterSteps(steps, "arg"));
			print(type_getter);
			println(";");
			if (locs.hasNext()) {
				println("  }");
			}
		}
	}

	private void genEpilogue() {
		hprintln();
		hprintln("#endif /* " + macro + " */");
	}

	private void genSetField(Type type, Field field) {
		String typeId = StringConversions.makeIdentifier(type.getId());
		String typeName = buildTypeName(type);
		String fieldId = StringConversions.makeIdentifier(field.getId());

		String fieldType = field.getType();
		String fieldTypeName = buildTypeName(fieldType);
		if (fieldType.equals(TypeConverter.CHARS_TYPE)
				|| fieldType.equals(TypeConverter.STR_TYPE)) {
			fieldTypeName = "const " + fieldTypeName;
		}

		String decl = typeName + " " + prefix + "set" + typeId
				+ StringConversions.capitalize(fieldId) + "(" + typeName
				+ " arg, " + fieldTypeName + " " + fieldId + ")";

		hprintln(decl + ";");

		printFoldOpen(decl);
		println(decl + " {");
		//		println("{");
		Iterator locs = field.locationIterator();
		boolean first = true;
		while (locs.hasNext()) {
			Location loc = (Location) locs.next();
			print("  ");
			if (first) {
				first = false;
			} else {
				print("else ");
			}
			println("if (" + buildIsAltName(type, loc.getAltId()) + "(arg)) {");
			print("    return (" + typeName + ")");
			Iterator steps = loc.stepIterator();
			String arg = genBuiltinToATerm(field.getType(), StringConversions
					.makeIdentifier(field.getId()));
			genSetterSteps(steps, new LinkedList(), arg);
			println(";");
			println("  }");
		}
		println();
		println("  ATabort(\"" + typeId + " has no "
				+ StringConversions.capitalize(fieldId) + ": %t\\n\", arg);");
		println("  return (" + typeName + ")NULL;");

		println("}");
		printFoldClose();
	}

	private void genSetterSteps(Iterator steps, List parentPath, String arg) {
		if (steps.hasNext()) {
			Step step = (Step) steps.next();
			switch (step.getType()) {
			case Step.ARG:
				print("ATsetArgument((ATermAppl)");
				break;
			case Step.ELEM:
				print("ATreplace((ATermList)");
				break;
			case Step.TAIL:
				print("ATreplaceTail((ATermList)");
				break;
			}
			print(genGetterSteps(parentPath.iterator(), "arg"));
			if (step.getType() == Step.TAIL) {
				print(", (ATermList)");
			} else {
				print(", (ATerm)");
			}
			parentPath.add(step);
			genSetterSteps(steps, parentPath, arg);
			print(", " + step.getIndex() + ")");
		} else {
			print(arg);
		}
	}

	private String genGetterSteps(Iterator steps, String arg) {
		if (steps.hasNext()) {
			Step step = (Step) steps.next();
			int index = step.getIndex();
			switch (step.getType()) {
			case Step.ARG:
				return genGetterSteps(steps, "ATgetArgument((ATermAppl)" + arg
						+ ", " + step.getIndex() + ")");
			case Step.ELEM:
				if (index == 0) {
					return genGetterSteps(steps, "ATgetFirst((ATermList)" + arg
							+ ")");
				} else {
					return genGetterSteps(steps, "ATelementAt((ATermList)"
							+ arg + ", " + step.getIndex() + ")");
				}
			case Step.TAIL:
				if (index == 0) {
					return genGetterSteps(steps, arg);
				} else if (index == 1) {
					return genGetterSteps(steps, "ATgetNext((ATermList)" + arg
							+ ")");
				} else {
					return genGetterSteps(steps, "ATgetTail((ATermList)" + arg
							+ ", " + step.getIndex() + ")");
				}
			}
		}

		return arg;
	}

	private void genHasField(Type type, Field field) {
		String type_id = StringConversions.makeIdentifier(type.getId());
		String type_name = buildTypeName(type);
		String decl = "ATbool " + prefix + "has" + type_id
				+ StringConversions.makeCapitalizedIdentifier(field.getId())
				+ "(" + type_name + " arg)";

		hprintln(decl + ";");

		printFoldOpen(decl);
		println(decl + " {");
		//		println("{");
		Iterator locs = field.locationIterator();
		boolean first = true;
		while (locs.hasNext()) {
			Location loc = (Location) locs.next();
			print("  ");
			if (first) {
				first = false;
			} else {
				print("else ");
			}
			println("if (" + buildIsAltName(type, loc.getAltId()) + "(arg)) {");
			println("    return ATtrue;");
			println("  }");
		}
		println("  return ATfalse;");
		println("}");
		printFoldClose();
	}

	private String buildConstructorName(Type type, Alternative alt) {
		String type_id = StringConversions.makeIdentifier(type.getId());
		String alt_id = StringConversions
				.makeCapitalizedIdentifier(alt.getId());
		return prefix + "make" + type_id + alt_id;
	}

	private String buildConstructorDecl(Type type, Alternative alt) {
		String type_name = buildTypeName(type);

		StringBuffer decl = new StringBuffer();
		decl.append(type_name + " " + buildConstructorName(type, alt) + "(");

		String fieldArgumentList = buildFieldArgumentList(type, alt);
		if (fieldArgumentList.equals("")) {
			fieldArgumentList = "void";
		}
		decl.append(fieldArgumentList);

		decl.append(")");
		return decl.toString();
	}

	private String buildFieldArgumentList(Type type, Alternative alt) {
		String args = "";
		Iterator fields = type.altFieldIterator(alt.getId());
		boolean first = true;
		while (fields.hasNext()) {
			Field field = (Field) fields.next();
			if (first) {
				first = false;
			} else {
				args += ", ";
			}
			String fieldType = field.getType();
			String typeName = buildTypeName(fieldType);
			if (fieldType.equals(TypeConverter.CHARS_TYPE)
					|| fieldType.equals(TypeConverter.STR_TYPE)) {
				typeName = "const " + typeName;
			}
			args += typeName + " "
					+ StringConversions.makeIdentifier(field.getId());
		}

		return args;
	}

	private String buildActualSeparatorArgsForMakeMany(SeparatedListType type) {
		Iterator fields = type.altFieldIterator("many");
		String result = "";
		fields.next(); // skip head

		while (fields.hasNext()) {
			Field field = (Field) fields.next();

			if (!fields.hasNext()) {
				break; // skip tail
			}

			result += StringConversions.makeIdentifier(field.getId());
			result += ", ";
		}

		return result;
	}

	private String buildActualSeparatorArgs(SeparatedListType type) {
		Iterator fields = type.altFieldIterator("many");
		String result = "";
		fields.next(); // skip head

		while (fields.hasNext()) {
			Field field = (Field) fields.next();

			if (!fields.hasNext()) {
				break; // skip tail
			}

			result += StringConversions.makeIdentifier(field.getId());
			result += ", ";
		}

		return result;
	}

	private void genTypeIsValid(Type type) {
		String type_id = StringConversions.makeIdentifier(type.getId());
		String type_name = buildTypeName(type);
		String decl = "ATbool " + prefix + "isValid" + type_id + "("
				+ type_name + " arg)";

		printFoldOpen(decl);

		hprintln(decl + ";");
		println(decl + " {");
		//		println("{");
		Iterator alts = type.alternativeIterator();
		boolean first = true;
		while (alts.hasNext()) {
			Alternative alt = (Alternative) alts.next();
			print("  ");
			if (first) {
				first = false;
			} else {
				print("else ");
			}
			println("if (" + buildIsAltName(type, alt) + "(arg)) {");
			println("    return ATtrue;");
			println("  }");
		}
		println("  return ATfalse;");
		println("}");

		printFoldClose();
	}

	private void genIsAlt(Type type, Alternative alt)
			throws GenerationException {
		String type_name = buildTypeName(type);
		String decl = "inline ATbool " + buildIsAltName(type, alt) + "("
				+ type_name + " arg)";
		String pattern;
		StringBuffer match_code = new StringBuffer();
		boolean contains_placeholder = alt.containsPlaceholder();
		int alt_count = type.getAlternativeCount();
		boolean inverted = false;

		pattern = prefix + "pattern"
				+ StringConversions.makeIdentifier(type.getId())
				+ StringConversions.makeCapitalizedIdentifier(alt.getId());

		match_code.append("ATmatchTerm((ATerm)arg, " + pattern);
		Iterator fields = type.altFieldIterator(alt.getId());
		while (fields.hasNext()) {
			fields.next();
			match_code.append(", NULL");
		}
		match_code.append(")");

		hprintln(decl + ";");

		printFoldOpen(decl);
		println(decl + " {");
		//		println("{");
		if (false && inverted) {
			println("  return !(" + match_code + ");");
		} else if (false && alt_count != 1 && !contains_placeholder) {
			println("  return " + match_code + ";");
		} else {
			AlternativeList alts_left = type.getAlternatives();
			alts_left.remove(alt);

			alt_count = alts_left.size();
			int pat_type = alt.getPatternType();
			alts_left.keepByType(pat_type);
			if (alts_left.size() != alt_count) {
				println("  if (ATgetType((ATerm)arg) != "
						+ getATermTypeName(pat_type) + ") {");
				println("    return ATfalse;");
				println("  }");
			}
			if (pat_type == ATerm.APPL) {
				if (alt instanceof ATermAppl) {
					AFun afun = ((ATermAppl) alt.buildMatchPattern()).getAFun();
					alt_count = alts_left.size();
					alts_left.keepByAFun(afun);
					if (alts_left.size() < alt_count) {
						println("  if (ATgetAFun((ATermAppl)arg) != ATgetAFun("
								+ pattern + ")) {");
						println("    return ATfalse;");
						println("  }");
					}
				}
			} else if (pat_type == ATerm.LIST) {
				ATermList matchPattern = (ATermList) alt.buildMatchPattern();
				if (matchPattern.isEmpty()) {
					alts_left.clear();
					println("  if (!ATisEmpty((ATermList)arg)) {");
					println("    return ATfalse;");
					println("  }");
				} else if (!matchPattern.equals(matchPattern.getFactory()
						.parse("[<list>]"))) {
					alts_left.removeEmptyList();
					println("  if (ATisEmpty((ATermList)arg)) {");
					println("    return ATfalse;");
					println("  }");
				}
			}

			if (alts_left.size() == 0) {
				println("#ifndef DISABLE_DYNAMIC_CHECKING");
				println("  assert(arg != NULL);");
				println("  assert(" + match_code + ");");
				println("#endif");
				println("  return ATtrue;");
			} else {
				println("  {");
				println("    static ATerm last_arg = NULL;");
				println("    static int last_gc = -1;");
				println("    static ATbool last_result;");
				println();
				println("    assert(arg != NULL);");
				println();
				println("    if (last_gc != ATgetGCCount() || "
						+ "(ATerm)arg != last_arg) {");
				println("      last_arg = (ATerm)arg;");
				println("      last_result = " + match_code + ";");
				println("      last_gc = ATgetGCCount();");
				println("    }");
				println();
				println("    return last_result;");
				println("  }");
			}
		}
		println("}");

		printFoldClose();
	}

	private String genATermToBuiltin(String type, String arg) {
		TypeConverter converter = new TypeConverter(
				new CTypeConversions(prefix));

		return converter.makeATermToBuiltinConversion(type, arg);
	}

	private String genBuiltinToATerm(String type, String id) {
		TypeConverter converter = new TypeConverter(
				new CTypeConversions(prefix));

		return "((ATerm) " + converter.makeBuiltinToATermConversion(type, id)
				+ ")";
	}

	private String getATermTypeName(int type) {
		switch (type) {
		case ATerm.INT:
			return "AT_INT";
		case ATerm.REAL:
			return "AT_REAL";
		case ATerm.APPL:
			return "AT_APPL";
		case ATerm.LIST:
			return "AT_LIST";
		case ATerm.PLACEHOLDER:
			return "AT_PLACEHOLDER";
		case ATerm.BLOB:
			return "AT_BLOB";
		default:
			throw new RuntimeException("illegal ATerm type: " + type);
		}
	}

	private String buildIsAltName(Type type, Alternative alt) {
		return buildIsAltName(type, alt.getId());
	}

	private String buildIsAltName(Type type, String altId) {
		return prefix + "is" + StringConversions.makeIdentifier(type.getId())
				+ StringConversions.makeCapitalizedIdentifier(altId);
	}

	private String buildGetterName(Type type, Field field) {
		String type_id = StringConversions.makeIdentifier(type.getId());
		String fieldId = StringConversions.makeCapitalizedIdentifier(field
				.getId());

		return prefix + "get" + type_id + fieldId;
	}

	private String buildTypeName(Type type) {
		return buildTypeName(type.getId());
	}

	private String buildTypeName(String typeId) {
		TypeConverter conv = new TypeConverter(new CTypeConversions(prefix));

		String name = conv.getType(typeId);

		if (conv.isReserved(typeId)) {
			return name;
		} else {
			return prefix + StringConversions.makeCapitalizedIdentifier(name);
		}
	}

	public AFunRegister getAFunRegister() {
		return afunRegister;
	}

	private void genGetLength(String typeId, String typeName) {
		String decl = "int " + prefix + "get" + typeId + "Length (" + typeName
				+ " arg)";

		hprintln(decl + ";");
		println(decl + " {");

		println("  return ATgetLength((ATermList) arg);");
		println("}");
	}

	private String buildFormalSeparatorArgs(SeparatedListType type) {
		Iterator fields = type.separatorFieldIterator();
		String result = "";

		while (fields.hasNext()) {
			Field field = (Field) fields.next();

			result += buildTypeName(field.getType()) + " "
					+ StringConversions.makeIdentifier(field.getId());
			result += ", ";
		}
		return result;
	}

}