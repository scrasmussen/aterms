package apigen.gen.c;

import java.io.*;
import aterm.pure.PureFactory;

import apigen.adt.*;
import apigen.gen.TomSignatureGenerator;

public class Main {
	private static boolean verbose = false;
	private static boolean jtom = false;
	private static boolean make_term_compatibility = false;
	private static String output = null;
	private static String prologue = null;
	private static String prefix = "";

	private static void usage() {
		System.err.println("usage: CGen [options]");
		System.err.println("options:");
		System.err.println("\t-prefix <prefix>          [\"\"]");
		System.err.println("\t-input <in>               [-]");
		System.err.println("\t-output <out>");
		System.err.println("\t-prologue <prologue>");
		System.err.println("\t-jtom");
		System.exit(1);
	}

	public final static void main(String[] args) throws IOException {
		String input = "-";

		InputStream inputStream;

		if (args.length == 0) {
			usage();
		}

		for (int i = 0; i < args.length; i++) {
			if ("-help".startsWith(args[i])) {
				usage();
			} else if ("-verbose".startsWith(args[i])) {
				verbose = true;
			} else if ("-prefix".startsWith(args[i])) {
				prefix = args[++i];
			} else if ("-output".startsWith(args[i])) {
				output = args[++i];
			} else if ("-prologue".startsWith(args[i])) {
				prologue = args[++i];
			} else if ("-input".startsWith(args[i])) {
				input = args[++i];
			} else if ("-compatible:term".equals(args[i])) {
				make_term_compatibility = true;
			} else if ("-jtom".startsWith(args[i])) {
				jtom = true;
			} else {
				usage();
			}
		}

		if (input.equals("-")) {
			inputStream = System.in;
			if (output == null) {
				usage();
			}
		} else {
			inputStream = new FileInputStream(input);
			if (output == null) {
				int extIndex = input.lastIndexOf((int) '.');
				output = input.substring(0, extIndex);
			}

			run(inputStream);
		}
	}

	static private void run(InputStream input) {
		ADT adt;

		try {
			adt = new ADT(new PureFactory().readFromFile(input));
			new APIGenerator(adt, output, prefix, verbose, true);
			new TomSignatureGenerator(adt,new CTomSignatureImplementation(),".",output,verbose,true).run();
			new CDictionaryGenerator(adt, ".", output, prefix, verbose, true).run();

		} catch (IOException e) {
			System.out.println("Failed to read ADT from file");
			System.exit(1);
		}

	}
}
