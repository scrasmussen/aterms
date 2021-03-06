package apigen.gen.java;

import java.io.File;
import java.util.Iterator;

import apigen.adt.ADT;
import apigen.adt.Alternative;
import apigen.adt.ListType;
import apigen.adt.Type;
import apigen.gen.Generator;
import apigen.gen.StringConversions;

public class MakeRulesGenerator extends Generator {
	private static final int MAX_FILES_IN_MAKEFILE_VARIABLE = 50;

	private JavaGenerationParameters params;
	private ADT adt;
	private String name;
	private int bucket;
	private String prefix;

	public MakeRulesGenerator(ADT adt, JavaGenerationParameters params) {
		super(params);
		this.params = params;
		this.adt = adt;
		this.name = StringConversions.makeCapitalizedIdentifier(params
				.getApiName());
		this.prefix = name + "API";
		setDirectory(buildOutputDirectory());
		setFileName(name + "MakeRules");
		setExtension("");
	}

	private String buildOutputDirectory() {
		StringBuffer buf = new StringBuffer();
		String pkg = params.getPackageName();

		buf.append(params.getOutputDirectory());
		buf.append(File.separatorChar);

		if (pkg != null) {
			buf.append(pkg.replace('.', File.separatorChar));
		}

		return buf.toString();
	}

	private static String getClassFileName(String className) {
		return className + ".java";
	}

	protected void generate() {
		Iterator<Type> types = adt.typeIterator();
		bucket = 0;
		int i = 0;

		while (types.hasNext()) {
			Type type = types.next();

			makeNewBucket(i);

			printTypeClassFiles(type);
			i++;

			if (!(type instanceof ListType)) {
				i = printAlternativesClassFiles(i, type);
			}
		}

		println();
		println();
		printAccumulatedVariable();
	}

	private int printAlternativesClassFiles(int i, Type type) {
		Iterator<Alternative> alts = type.alternativeIterator();
		while (alts.hasNext()) {
			Alternative alt = alts.next();
			makeNewBucket(i);
			printAlternativeClassFiles(type, alt);
			i++;
		}

		return i;
	}

	protected void printAccumulatedVariable() {
		print(prefix
				+ "=\\\n"
				// + getClassFileName(FactoryGenerator.className())
				+ getClassFileName(FactoryGenerator.className("BUG_HERE"))
				+ " "
				// + getClassFileName(AbstractTypeGenerator.className())
				+ getClassFileName(AbstractTypeGenerator.className("BUG_HERE"))
				+ "\\\n");

		while (--bucket >= 0) {
			print("${" + prefix + bucket + "} ");
		}
	}

	protected void printAlternativeClassFiles(Type type, Alternative alt) {
		print("\\\n"
				+ getClassFileName(AlternativeGenerator.className(type, alt)));
	}

	protected void printTypeClassFiles(Type type) {
		print("\\\n" + getClassFileName(TypeGenerator.className(type.getId())));
	}

	protected void makeNewBucket(int i) {
		if (i % MAX_FILES_IN_MAKEFILE_VARIABLE == 0) {
			println();
			println();
			print(prefix + bucket + "=");
			bucket++;
		}
	}

}
