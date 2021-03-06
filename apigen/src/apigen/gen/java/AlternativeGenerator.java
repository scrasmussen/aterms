package apigen.gen.java;

import java.util.Iterator;
import java.util.Set;

import apigen.adt.ADT;
import apigen.adt.Alternative;
import apigen.adt.Field;
import apigen.adt.Type;
import apigen.gen.GenerationParameters;
import apigen.gen.StringConversions;

public class AlternativeGenerator extends JavaGenerator {
	private Type type;
	private Alternative alt;
	private String className;
	private String superClassName;
	private boolean hasGlobalName;
	private ADT adt;
	private String traveler;

	protected AlternativeGenerator(ADT adt, JavaGenerationParameters params,
			Type type, Alternative alt) {
		super(params);
		this.adt = adt;
		this.type = type;
		this.alt = alt;
		this.hasGlobalName = !(params.getApiName()).equals("");
		this.className = className(type.getId(), alt.getId());
		this.superClassName = new TypeGenerator(params, type)
				.qualifiedClassName(params, type);
		this.traveler = params.getTravelerName();
	}

	public String getClassName() {
		return className;
	}

	public static String qualifiedClassName(JavaGenerationParameters params,
			Type type, Alternative alt) {
		StringBuffer buf = new StringBuffer();
		String pkg = params.getPackageName();

		if (pkg != null) {
			buf.append(pkg);
			buf.append('.');
		}

		buf.append(params.getApiExtName(type).toLowerCase());
		buf.append('.');
		buf.append(packageName(type));
		buf.append('.');
		buf.append(className(type, alt));
		return buf.toString();
	}

	private static String packageName(Type type) {
		return (TypeGenerator.packageName() + '.' + TypeGenerator
				.className(type)).toLowerCase();
	}

	public static String className(String type, String alt) {
		if (getConverter().isReserved(type)) {
			return alt;
		}
		return StringConversions.makeCapitalizedIdentifier(alt);
	}

	public static String className(Type type, Alternative alt) {
		return className(type.getId(), alt.getId());
	}

	protected void generate() {
		printPackageDecl();
		genAlternativeClass(type, alt);
	}

	private void genAlternativeClass(Type type, Alternative alt) {
		boolean visitable = getJavaGenerationParameters().isVisitable();
		print("public class " + className + " extends " + superClassName);

		if (visitable) {
			print(" implements " + traveler + ".Visitable");
		}
		println(" {");

		genAltConstructor();
		genAltFieldIndexMembers(type, alt);
		genAltDuplicateMethod(type, alt);
		genAltEquivalentMethod(type, alt);
		genAltMake(type, alt);
		genAltToTerm();
		genOverrideProperties(type, alt);
		genAltGetAndSetMethods(type, alt);
		genAltHashFunction(type, alt);

		if (visitable) {
			genAltVisitableInterface(type, alt);
		}

		println("}");
	}

	private void genAltConstructor() {
		JavaGenerationParameters params = getJavaGenerationParameters();
		println("  public "
				+ className
				+ "("
				+ FactoryGenerator.qualifiedClassName(params, type
						.getModuleName())
				+ " factory, aterm.ATermList annos, aterm.AFun fun, aterm.ATerm[] args) {");
		println("    super(factory, annos, fun, args);");
		println("  }");
		println();
	}

	private void genAltHashFunction(Type type, Alternative alt) {
		if (!hasReservedTypeFields(type, alt)) {
			int arity = type.getAltArity(alt);
			String goldenratio = "0x9e3779b9";
			int changingArg = guessChangingArgument(type, alt);
			String initval;

			if (changingArg > 0) {
				initval = "getArgument(" + changingArg + ").hashCode()";
			} else {
				initval = "0";
			}

			println("  protected int hashFunction() {");
			println("    int c = " + initval
					+ " + (getAnnotations().hashCode()<<8);");
			println("    int a = " + goldenratio + ";");
			println("    int b = (getAFun().hashCode()<<8);");

			/*
			 * This piece of code generates a number of shifts following the
			 * algorithm of the Doobs hash functions (Google: Doobs), you can
			 * find a general implementation in shared.HashFunctions of the
			 * shared-objects library. This code specializes the algorithm for a
			 * fixed number of arguments.
			 */
			for (int i = arity - 1; i >= 0; i--) {
				int shift = (i % 4) * 8;
				println("    " + "aaaabbbbcccc".toCharArray()[i % 12]
						+ " += (getArgument(" + i + ").hashCode() << " + shift
						+ ");");
			}

			println();
			println("    a -= b; a -= c; a ^= (c >> 13);");
			println("    b -= c; b -= a; b ^= (a << 8);");
			println("    c -= a; c -= b; c ^= (b >> 13);");
			println("    a -= b; a -= c; a ^= (c >> 12);");
			println("    b -= c; b -= a; b ^= (a << 16);");
			println("    c -= a; c -= b; c ^= (b >> 5);");
			println("    a -= b; a -= c; a ^= (c >> 3);");
			println("    b -= c; b -= a; b ^= (a << 10);");
			println("    c -= a; c -= b; c ^= (b >> 15);");
			println();
			println("    return c;");
			println("  }");
			println();
		}
	}

	private int guessChangingArgument(Type type, Alternative alt) {
		Iterator<Field> fields = type.altFieldIterator(alt.getId());

		/*
		 * if an argument has the same type as the result type, there exists a
		 * chance of building a tower of this constructor where only this
		 * argument changes. Therefore, this argument must be very important in
		 * the computation of the hash code in order to avoid collissions
		 */
		for (int i = 0; fields.hasNext(); i++) {
			Field field = fields.next();

			if (field.getType().equals(type.getId())) {
				return i;
			}
		}

		return -1;
	}

	private String buildGetFactoryMethodCall() {
		GenerationParameters params = getGenerationParameters();
		return AbstractTypeGenerator.getFactoryMethodName(params, type
				.getModuleName())
				+ "()";
	}

	private void genAltToTerm() {
		println("  public aterm.ATerm toTerm() {");
		println("    if (term == null) {");
		println("      term = " + buildGetFactoryMethodCall()
				+ ".toTerm(this);");
		println("    }");
		println("    return term;");
		println("  }");
		println();
	}

	private void genAltFieldIndexMembers(Type type, Alternative alt) {
		Iterator<Field> fields = type.altFieldIterator(alt.getId());
		int argnr = 0;

		while (fields.hasNext()) {
			Field field = fields.next();
			String fieldId = getFieldIndex(field.getId());

			println("  private static int " + fieldId + " = " + argnr + ";");
			argnr++;
		}
	}

	private void genAltGetAndSetMethod(Field field) {
		JavaGenerationParameters params = getJavaGenerationParameters();
		String fieldName = StringConversions.makeCapitalizedIdentifier(field
				.getId());
		String fieldId = getFieldId(field.getId());
		String fieldType = field.getType();
		String fieldClass = TypeGenerator.qualifiedClassName(params, fieldType);
		String fieldIndex = getFieldIndex(field.getId());

		genFieldGetterMethod(fieldName, fieldType, fieldClass, fieldIndex);
		genFieldSetterMethod(fieldName, fieldId, fieldType, fieldClass,
				fieldIndex);
	}

	private void genFieldSetterMethod(String fieldName, String fieldId,
			String fieldType, String fieldClass, String fieldIndex) {
		// setter
		println("  public " + superClassName + " set" + fieldName + "("
				+ fieldClass + " " + fieldId + ") {");
		print("    return (" + superClassName + ") super.setArgument(");

		if (fieldType.equals("str")) {
			print("getFactory().makeAppl(getFactory().makeAFun(" + fieldId
					+ ", 0, true))");
		} else if (fieldType.equals("int")) {
			print("getFactory().makeInt(" + fieldId + ")");
		} else if (fieldType.equals("real")) {
			print("getFactory().makeReal(" + fieldId + ")");
		} else if (fieldType.equals("chars")) {
			print(buildFactoryGetter() + ".stringToChars(" + fieldId + ")");
		} else if (fieldType.equals("char")) {
			print(buildFactoryGetter() + ".byteToChar(" + fieldId + ")");
		} else {
			print(fieldId);
		}

		println(", " + fieldIndex + ");");
		println("  }");
		println();
	}

	private String buildFactoryGetter() {
		return AbstractTypeGenerator.getFactoryMethodName(
				getGenerationParameters(), type.getModuleName())
				+ "()";
	}

	private void genFieldGetterMethod(String fieldName, String fieldType,
			String fieldClass, String fieldIndex) {
		println("  public " + fieldClass + " get" + fieldName + "() {");

		if (fieldType.equals("str")) {
			println("   return ((aterm.ATermAppl) getArgument(" + fieldIndex
					+ ")).getAFun().getName();");
		} else if (fieldType.equals("int")) {
			println("   return ((aterm.ATermInt) getArgument(" + fieldIndex
					+ ")).getInt();");
		} else if (fieldType.equals("real")) {
			println("   return ((aterm.ATermReal) getArgument(" + fieldIndex
					+ ")).getReal();");
		} else if (fieldType.equals("term")) {
			println("   return getArgument(" + fieldIndex + ");");
		} else if (fieldType.equals("chars")) {
			JavaGenerationParameters params = getJavaGenerationParameters();
			String factoryClass = FactoryGenerator.qualifiedClassName(params,
					type.getModuleName());
			println("   return " + factoryClass + ".charsToString(getArgument("
					+ fieldIndex + "));");
		} else if (fieldType.equals("char")) {
			JavaGenerationParameters params = getJavaGenerationParameters();
			String factoryClass = FactoryGenerator.qualifiedClassName(params,
					type.getModuleName());
			println("   return " + factoryClass + ".charToByte(((aterm.ATermInt) getArgument("
					+ fieldIndex + ")).getInt());");
		} else {
			println("    return (" + fieldClass + ") getArgument(" + fieldIndex
					+ ");");
		}

		println("  }");
		println();
	}

	private void genOverrideProperties(Type type, Alternative alt) {
		genOverrideIsMethod(alt);
		genOverrideHasMethods(type, alt);
	}

	private void genOverrideIsMethod(Alternative alt) {
		println("  public boolean is"
				+ StringConversions.makeCapitalizedIdentifier(alt.getId())
				+ "()");
		println("  {");
		println("    return true;");
		println("  }");
		println();
	}

	private void genOverrideHasMethods(Type type, Alternative alt) {
		Iterator<Field> fields = type.altFieldIterator(alt.getId());

		while (fields.hasNext()) {
			Field field = fields.next();
			genOverrideHasMethod(field);
		}
	}

	private void genOverrideHasMethod(Field field) {
		println("  public boolean has"
				+ StringConversions.makeCapitalizedIdentifier(field.getId())
				+ "() {");
		println("    return true;");
		println("  }");
		println();
	}

	private void genAltGetAndSetMethods(Type type, Alternative alt) {

		Iterator<Field> fields = type.altFieldIterator(alt.getId());
		while (fields.hasNext()) {
			Field field = fields.next();
			genAltGetAndSetMethod(field);
		}

		genOverrrideSetArgument(type, alt);
	}

	private void genOverrrideSetArgument(Type type, Alternative alt) {
		JavaGenerationParameters params = getJavaGenerationParameters();
		String altClassName = className(type, alt);

		println("  public aterm.ATermAppl setArgument(aterm.ATerm arg, int i) {");
		if (type.getAltArity(alt) > 0) {
			println("    switch(i) {");

			Iterator<Field> fields = type.altFieldIterator(alt.getId());
			for (int i = 0; fields.hasNext(); i++) {
				Field field = fields.next();
				String fieldType = field.getType();
				String fieldClass = TypeGenerator.qualifiedClassName(params,
						fieldType);

				boolean skip = false;
				String instance_of;

				if (fieldType.equals("str")) {
					instance_of = "aterm.ATermAppl";
				} else if (fieldType.equals("int")) {
					instance_of = "aterm.ATermInt";
				} else if (fieldType.equals("real")) {
					instance_of = "aterm.ATermReal";
				} else if (fieldType.equals("term")) {
					instance_of = "aterm.ATerm";
					skip = true;
				} else if (fieldType.equals("chars")) {
					instance_of = "aterm.ATermList";
				} else if (fieldType.equals("char")) {
					instance_of = "aterm.ATermInt";
				} else {
					instance_of = fieldClass;
				}

				println("      case " + i + ":");
				if (skip == true) {
					println("        // arg " + i
							+ " is always of type aterm.ATerm");
				} else {
					println("        if (!(arg instanceof " + instance_of
							+ ")) { ");
					println("          throw new RuntimeException(\"Argument "
							+ i + " of a " + altClassName
							+ " should have type " + fieldType + "\");");
					println("        }");
				}
				println("        break;");
			}
			println("      default: throw new RuntimeException(\""
					+ altClassName + " does not have an argument at \" + i );");
			println("    }");
			println("    return super.setArgument(arg, i);");
		} else {
			println("      throw new RuntimeException(\"" + altClassName
					+ " has no arguments\");");
		}
		println("  }");
		println();
	}

	private void genAltMake(Type type, Alternative alt) {
		String getFactoryMethod = buildGetFactoryMethodCall();
		String makeMethod = "make" + FactoryGenerator.concatTypeAlt(type, alt);

		println("  protected aterm.ATermAppl make(aterm.AFun fun, aterm.ATerm[] args,"
				+ " aterm.ATermList annos) {");
		println("    return " + getFactoryMethod + "." + makeMethod
				+ "(fun, args, annos);");
		println("  }");
		println();
	}

	private void genAltDuplicateMethod(Type type, Alternative alt) {
		println("  public shared.SharedObject duplicate() {");
		println("    return this;");
		println("  }");
		println();
	}

	private void genAltEquivalentMethod(Type type, Alternative alt) {
		println("  public boolean equivalent(shared.SharedObject peer) {");
		println("    if (peer instanceof " + className(type, alt) + ") {");
		println("      return super.equivalent(peer);");
		println("    }");
		println("    return false;");
		println("  }");
		println();
	}

	private void genAltVisitableInterface(Type type, Alternative alt) {
		Iterator<String> moduleIt;
		if (hasGlobalName) {
			moduleIt = adt.getModuleNameSet().iterator();
		} else {
			Set<String> moduleToGen = adt.getImportsClosureForModule(type
					.getModuleName());
			moduleIt = moduleToGen.iterator();
		}

		while (moduleIt.hasNext()) {
			String moduleName = moduleIt.next();
			String visitorPackage = VisitorGenerator.qualifiedClassName(
					getJavaGenerationParameters(), moduleName);
			String altClassName = FactoryGenerator.concatTypeAlt(type, alt);
			// String abstractTypePackage =
			// AbstractTypeGenerator.qualifiedClassName(getJavaGenerationParameters(),moduleName);
			String abstractTypePackage = AbstractTypeGenerator
					.qualifiedClassName(getJavaGenerationParameters(), type
							.getModuleName());

			println("  public " + abstractTypePackage + " accept("
					+ visitorPackage + " v) throws " + traveler
					+ ".VisitFailure {");
			println("    return v.visit_" + altClassName + "(this);");
			println("  }");
			println();
		}
	}

	private int computeAltArityNotReserved(Type type, Alternative alt) {
		Iterator<Field> fields = type.altFieldIterator(alt.getId());
		int count = 0;
		while (fields.hasNext()) {
			Field field = fields.next();
			if (!getConverter().isReserved(field.getType())) {
				count++;
			}
		}
		return count;
	}

	private boolean hasReservedTypeFields(Type type, Alternative alt) {
		return computeAltArityNotReserved(type, alt) < type.getAltArity(alt);
	}

	public String getPackageName() {
		StringBuffer buf = new StringBuffer();
		buf.append(getGenerationParameters().getApiExtName(type));
		buf.append('.');
		buf.append(TypeGenerator.packageName());
		buf.append('.');
		buf.append(TypeGenerator.className(type));
		return buf.toString().toLowerCase();
	}

	public String getQualifiedClassName() {
		return getPackageName() + '.' + getClassName();
	}
}
