/**
 * Combine approach 1 by Gabor Kecskemeti (g.kecskemeti@ljmu.ac.uk)
 */
package hu.mta.sztaki.lpds.cloud.simulator.combineazuredataset;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class CombineFilesGabor {
	public static class StringPair {
		public final String hashes;
		public final String rest;

		public StringPair(String h, String r) {
			hashes = h;
			rest = r;
		}
	}

	public static TreeMap<String, String> execution = new TreeMap<>();
	public static TreeMap<String, String> memory = new TreeMap<>();

	public static void readAllLines(String file, Consumer<String> singleLineProcessor) throws IOException {
		try (Stream<String> lineStream = Files.lines(new File(file).toPath())) {
			lineStream.forEach(singleLineProcessor);
		}
	}

	public static StringPair splitToPair(String line, int commasForSplit) {
		int split = -1;
		for (int commas = 0; commas < commasForSplit; commas++) {
			split = line.indexOf(',', split + 1);
		}
		return new StringPair(line.substring(0, split), line.substring(split));
	}

	public static void splitPut(String line, TreeMap<String, String> container, int commasForSplit) {
		StringPair pair = splitToPair(line, commasForSplit);
		container.put(pair.hashes, pair.rest);
	}

	public static void mergeLine(String line, BufferedWriter bw) {
		StringPair execPair = splitToPair(line, 3);
		StringPair memPair = splitToPair(line, 2);
		String execRest = execution.get(execPair.hashes);
		String memRest = memory.get(memPair.hashes);
		if (execRest != null && memRest != null) {
			try {
				bw.write(line);
				bw.write(execRest);
				bw.write(memRest);
				bw.write("\n");
			} catch (IOException ioex) {
				throw new RuntimeException(ioex);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		long currTime = System.currentTimeMillis();
		readAllLines("../execution.csv", s -> splitPut(s, execution, 3));
		readAllLines("../memory.csv", s -> splitPut(s, memory, 2));
		try (BufferedWriter bw = new BufferedWriter(new FileWriter("../merged.csv"))) {
			readAllLines("../invocation.csv", s -> mergeLine(s, bw));
		}
		System.out.println((System.currentTimeMillis() - currTime) + "ms");
	}

}
