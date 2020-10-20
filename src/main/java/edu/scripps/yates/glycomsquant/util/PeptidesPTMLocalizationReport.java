package edu.scripps.yates.glycomsquant.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class PeptidesPTMLocalizationReport {
	private final Set<QuantifiedPeptideInterface> peptidesWithPTMsAndNoMotifs = new THashSet<QuantifiedPeptideInterface>();
	private final Map<QuantifiedPeptideInterface, QuantifiedPeptideInterface> fixedPeptides = new THashMap<QuantifiedPeptideInterface, QuantifiedPeptideInterface>();
	private final Set<QuantifiedPeptideInterface> notFixablePeptides = new THashSet<QuantifiedPeptideInterface>();
	private final Set<QuantifiedPeptideInterface> peptidesWithCorrectPTMs = new THashSet<QuantifiedPeptideInterface>();
	private final Set<QuantifiedPeptideInterface> peptidesDiscardedByWrongProtein = new THashSet<QuantifiedPeptideInterface>();
	private final Set<QuantifiedPeptideInterface> peptidesDiscardedByIntensityThreshold = new THashSet<QuantifiedPeptideInterface>();
	private final Set<QuantifiedPeptideInterface> peptidesDiscardedForNotHavingMotif = new THashSet<QuantifiedPeptideInterface>();

	public Set<QuantifiedPeptideInterface> getPeptidesDiscardedByWrongProtein() {
		return peptidesDiscardedByWrongProtein;
	}

	private final static DecimalFormat formatter = new DecimalFormat("#.##%");

	public PeptidesPTMLocalizationReport() {

	}

	public Map<QuantifiedPeptideInterface, QuantifiedPeptideInterface> getFixedPeptides() {
		return fixedPeptides;
	}

	public void addPeptideWithPTMAndNoMotif(QuantifiedPeptideInterface peptide) {
		this.peptidesWithPTMsAndNoMotifs.add(peptide);
	}

	public void addPeptideWithCorrectPTM(QuantifiedPeptideInterface peptide) {
		this.peptidesWithCorrectPTMs.add(peptide);
	}

	public Set<QuantifiedPeptideInterface> getPeptidesWithPTMsAndNoMotifs() {
		return peptidesWithPTMsAndNoMotifs;
	}

	public void addFixedPeptide(QuantifiedPeptideInterface originalPeptide, QuantifiedPeptideInterface fixedPeptide) {
		fixedPeptides.put(originalPeptide, fixedPeptide);
	}

	public void addNotFixablePeptide(QuantifiedPeptideInterface peptide) {
		this.notFixablePeptides.add(peptide);
	}

	public Set<QuantifiedPeptideInterface> getNotFixablePeptides() {
		return notFixablePeptides;
	}

	public void printToFile(File outputFile) throws IOException {
		FileWriter fw = null;
		try {
			fw = new FileWriter(outputFile);
			// summary
			fw.write("\tPeptides\tPSMs\n");
			fw.write("Total with PTMs of interest:\t" + getTotalPeptides() + "\t" + getTotalPSMs() + "\n");
			fw.write("With PTMs in correct motifs:\t" + peptidesWithCorrectPTMs.size() + "\t"
					+ getNumPSMs(peptidesWithCorrectPTMs) + "\n");

			fw.write("With PTMs in non-valid motif and no possible alternative location:\t"
					+ peptidesWithPTMsAndNoMotifs.size() + "\t" + getNumPSMs(peptidesWithPTMsAndNoMotifs) + "\n");

			fw.write("With PTMs in non-valid motif and multiple alternative locations (ambiguous):\t"
					+ notFixablePeptides.size() + "\t" + getNumPSMs(notFixablePeptides) + "\n");

			fw.write(
					"With PTMs in non-valid motif but valid non-ambiguous alternative locations present (locations can be shifted):\t"
							+ fixedPeptides.size() + "\t" + getNumPSMs(fixedPeptides.keySet()) + "\n");

			fw.write("\nPSM False Localization Rate:\t" + formatter.format(getPSMFalseLocalizationRate()) + "\n\n");

			fw.write("Peptides with PTMs in non-valid motif and no possible alternative location:\n");
			fw.write("Peptide\tNum PSMs\n");
			for (final QuantifiedPeptideInterface pep : getSortedByPSMs(peptidesWithPTMsAndNoMotifs)) {
				fw.write(pep.getFullSequence() + "\t" + pep.getQuantifiedPSMs().size() + "\n");
			}
			fw.write("\nPeptides with PTMs in non-valid motif and multiple alternative locations (ambiguous):\n");
			fw.write("Peptide\tNum PSMs\n");
			for (final QuantifiedPeptideInterface pep : getSortedByPSMs(notFixablePeptides)) {
				fw.write(pep.getFullSequence() + "\t" + pep.getQuantifiedPSMs().size() + "\n");
			}
			fw.write(
					"\nPeptides with PTMs in non-valid motif but valid non-ambiguous alternative locations present (locations can be shifted):\n");
			fw.write("Peptide\tNum PSMs\tPeptide with shifted PTM\n");
			for (final QuantifiedPeptideInterface pep : getSortedByPSMs(fixedPeptides.keySet())) {
				final QuantifiedPeptideInterface fixed = fixedPeptides.get(pep);
				fw.write(pep.getFullSequence() + "\t" + pep.getQuantifiedPSMs().size() + "\t" + fixed.getFullSequence()
						+ "\n");
			}
		} finally

		{
			if (fw != null) {
				fw.close();
			}
		}

	}

	private int getTotalPeptides() {
		final int total = peptidesWithPTMsAndNoMotifs.size() + peptidesWithCorrectPTMs.size() + fixedPeptides.size()
				+ notFixablePeptides.size();
		return total;
	}

	private int getTotalPSMs() {
		final int total = getNumPSMs(peptidesWithPTMsAndNoMotifs) + getNumPSMs(peptidesWithCorrectPTMs)
				+ getNumPSMs(fixedPeptides.keySet()) + getNumPSMs(notFixablePeptides);
		return total;
	}

	public double getPSMFalseLocalizationRate() {
		final int f = getNumPSMs(peptidesWithPTMsAndNoMotifs);
		final int total = getTotalPSMs();
		final double flr = 1.0 * f / total;
		return flr;
	}

	private int getNumPSMs(Set<QuantifiedPeptideInterface> peps) {
		return peps.stream().map(pep -> pep.getQuantifiedPSMs().size()).reduce(0, Integer::sum);
	}

	private List<QuantifiedPeptideInterface> getSortedByPSMs(Set<QuantifiedPeptideInterface> peps) {
		return peps.stream().sorted(getComparator()).collect(Collectors.toList());
	}

	private Comparator<QuantifiedPeptideInterface> getComparator() {
		return new Comparator<QuantifiedPeptideInterface>() {

			@Override
			public int compare(QuantifiedPeptideInterface o1, QuantifiedPeptideInterface o2) {
				return Integer.compare(o2.getQuantifiedPSMs().size(), o1.getQuantifiedPSMs().size());
			}
		};
	}

	public Set<QuantifiedPeptideInterface> getPeptidesWithCorrectPTMs() {
		return peptidesWithCorrectPTMs;
	}

	public void addPeptideDiscardedByWrongProtein(QuantifiedPeptideInterface peptide) {
		peptidesDiscardedByWrongProtein.add(peptide);

	}

	public void addPeptideDiscardedByIntensityThreshold(QuantifiedPeptideInterface peptide) {
		peptidesDiscardedByIntensityThreshold.add(peptide);

	}

	public Set<QuantifiedPeptideInterface> getPeptidesDiscardedByIntensityThreshold() {
		return peptidesDiscardedByIntensityThreshold;
	}

	public void addPeptideDiscardedForNotHavingMotif(QuantifiedPeptideInterface peptide) {
		peptidesDiscardedForNotHavingMotif.add(peptide);

	}

	public Set<QuantifiedPeptideInterface> getPeptidesDiscardedForNotHavingMotif() {
		return peptidesDiscardedForNotHavingMotif;
	}
}
