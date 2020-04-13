package edu.scripps.yates.glycomsquant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.scripps.yates.utilities.proteomicsmodel.enums.AmountType;

public class GlycoPTMAnalyzerIterator {

	private static final int MAX_ITERATIONS = 20;

	public static void main(String[] args) {
		FileWriter fw = null;
		try {
			fw = new FileWriter(
					new File("C:\\Users\\salvador\\Desktop\\HIV project\\HighMan_500ng_HFX_labelfree\\iterations.txt"));
			Double maxAverageOf_203 = -Double.MAX_VALUE;
			boolean calculateProportionsByPeptidesFirst = true;
			int numIterations = 0;
			double intensityThrehold = 1;
			double optimalThreshold = intensityThrehold;
			fw.write("Iteration\t# valid Peptides\tAverage % of " + PTMCode._203 + "\toptimalThreshold\n");
			while (numIterations < MAX_ITERATIONS) {
				try {
					intensityThrehold = intensityThrehold * 10;
					GlycoPTMAnalyzer analyzer = runAnalysis(intensityThrehold, calculateProportionsByPeptidesFirst);
					double average = analyzer.getAveragePercentage(PTMCode._203);

					double d = average - maxAverageOf_203;
					if (d > 0.0001) {
						maxAverageOf_203 = average;
						optimalThreshold = intensityThrehold;
						System.out.println(
								"Average " + PTMCode._203 + " of " + average + " with threshold " + optimalThreshold);
					} else {
						analyzer.deleteGraphs();
					}

					fw.write(numIterations + "\t" + analyzer.getValidPeptides() + "\t" + average + "\t"
							+ maxAverageOf_203 + "\t" + intensityThrehold + "\n");
				} finally {
					numIterations++;
					fw.flush();
				}

			}
			fw.write("Optimal threshold\t" + optimalThreshold);

		} catch (

		Exception e) {
			e.printStackTrace();
		} finally {
			try {
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static GlycoPTMAnalyzer runAnalysis(double intensityThreshold, boolean calculateProportionsByPeptidesFirst)
			throws IOException {
		System.out.println("Running analysis with threshold=" + intensityThreshold);
		File inputFile = new File(
				"C:\\Users\\salvador\\Desktop\\HIV project\\HighMan_500ng_HFX_labelfree\\HighMan_500ng_HFX_labelfree.txt");
		File fastaFile = new File(
				"C:\\Users\\salvador\\Desktop\\HIV project\\IP2_claired_database__UniProt_Bos_Taurus_BG505_SOSIP_gp140_01-20-2015_reversed.fasta");
		String prefix = "HighMan_500ng_HFX_labelfree";
		String suffix = "";
		String proteinOfInterestACC = GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST;
		boolean normalizeExperimentsByProtein = true;

		GlycoPTMAnalyzer analyzer = new GlycoPTMAnalyzer(inputFile, proteinOfInterestACC, fastaFile, 0.1234, prefix, suffix,
				intensityThreshold, AmountType.INTENSITY, normalizeExperimentsByProtein,
				calculateProportionsByPeptidesFirst);
		analyzer.run();
		return analyzer;

	}

}
