package apigen.gen.java;

import java.util.Iterator;
import java.util.Set;

import apigen.adt.ADT;
import apigen.adt.Type;
import apigen.adt.api.types.Module;
import apigen.gen.GenerationParameters;
import apigen.gen.StringConversions;
import apigen.gen.java.FactoryGenerator;

public class AbstractTypeGenerator extends JavaGenerator {
	private boolean visitable;
	private String apiName;
	private String factoryClassName;
	private Module module;
	private boolean hasGlobalName;
	private ADT adt;
	private String traveler;

	public AbstractTypeGenerator(ADT adt, JavaGenerationParameters params,
			Module module) {
		super(params);
		this.adt = adt;
		this.apiName = params.getApiExtName(module);
		this.hasGlobalName = !(params.getApiName()).equals("");
		this.module = module;
		this.factoryClassName = FactoryGenerator
				.qualifiedClassName(getJavaGenerationParameters(), module
						.getModulename().getName());
		this.visitable = params.isVisitable();
		this.traveler = params.getTravelerName();
	}

	public String getClassName() {
		return className();
	}

	public String className() {
		return AbstractTypeGenerator
				.className(module.getModulename().getName());
	}

	public static String className(String moduleName) {
		return moduleName + "AbstractType";
	}

	public String qualifiedClassName(JavaGenerationParameters params) {
		return AbstractTypeGenerator.qualifiedClassName(params, module
				.getModulename().getName());
	}

	public static String qualifiedClassName(JavaGenerationParameters params,
			String moduleName) {
		StringBuffer buf = new StringBuffer();
		String pkg = params.getPackageName();

		if (pkg != null) {
			buf.append(pkg);
			buf.append('.');
		}
		buf.append(params.getApiExtName(moduleName).toLowerCase());
		buf.append('.');
		buf.append(AbstractTypeGenerator.className(moduleName));
		return buf.toString();
	}

	protected void generate() {
		printPackageDecl();
		genAbstractTypeClass();
	}

	private void genAbstractTypeClass() {
		println("abstract public class " + getClassName()
				+ " extends aterm.pure.ATermApplImpl {");
		genClassVariables();
		genConstructor();
		genToTermMethod();
		genToStringMethod();
		genSetTermMethod();
		genGetFactoryMethod();
		genDefaultTypePredicates();

		if (visitable) {
			genAccept();
		}
		println("}");
	}

	private void genClassVariables() {
		println("  protected aterm.ATerm term;");
		println();
		println("  private " + factoryClassName + " abstractTypeFactory;");
		println();
	}

	private void genConstructor() {
		println("  public "
				+ getClassName()
				+ "("
				+ factoryClassName
				+ " abstractTypeFactory, aterm.ATermList annos, aterm.AFun fun, aterm.ATerm[] args) {");
		println("    super(abstractTypeFactory.getPureFactory(), annos, fun, args);");
		println("    this.abstractTypeFactory = abstractTypeFactory;");
		println("  }");
		println();
	}

	private void genGetFactoryMethod() {
		GenerationParameters params = getGenerationParameters();
		println("  public "
				+ factoryClassName
				+ ' '
				+ getFactoryMethodName(params, module.getModulename().getName())
				+ "() {");
		println("    return abstractTypeFactory;");
		println("  }");
		println();
	}

	private void genSetTermMethod() {
		println("  protected void setTerm(aterm.ATerm term) {");
		println("    this.term = term;");
		println("  }");
		println();
	}

	private void genToStringMethod() {
		println("  public String toString() {");
		println("    return toTerm().toString();");
		println("  }");
		println();
	}

	private void genToTermMethod() {
		println("  abstract public aterm.ATerm toTerm();");
		println();
	}

	private void genAccept() {
		Iterator<String> moduleIt;
		if (hasGlobalName) {
			moduleIt = adt.getModuleNameSet().iterator();
		} else {
			Set<String> moduleToGen = adt.getImportsClosureForModule(module
					.getModulename().getName());
			moduleIt = moduleToGen.iterator();
		}

		while (moduleIt.hasNext()) {
			String moduleName = moduleIt.next();
			String visitorPackage = VisitorGenerator.qualifiedClassName(
					getJavaGenerationParameters(), moduleName);
			println("  abstract public " + getClassName() + " accept("
					+ visitorPackage + " v) throws " + traveler
					+ ".VisitFailure;");
			println();
		}
	}

	private void genDefaultTypePredicates() {
		Iterator<Type> types = adt.typeIterator();
		while (types.hasNext()) {
			Type type = types.next();
			genDefaultTypePredicate(type);
		}
	}

	private void genDefaultTypePredicate(Type type) {
		println("  public boolean isSort" + TypeGenerator.className(type)
				+ "() {");
		println("    return false;");
		println("  }");
		println();
	}

	public String getPackageName() {
		return apiName.toLowerCase();
	}

	public String getQualifiedClassName() {
		return getClassName();
	}

	public static String getFactoryMethodName(GenerationParameters params,
			String moduleName) {
		String apiName = params.getApiName();
		if (!apiName.equals("")) {
			apiName = StringConversions.capitalize(params.getApiName());
		}
		return "get" + apiName + FactoryGenerator.className(moduleName);
	}
}
