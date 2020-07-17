package edu.scripps.yates.glycomsquant;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import edu.scripps.yates.census.read.QuantCompareParser;
import edu.scripps.yates.census.read.QuantParserException;
import edu.scripps.yates.census.read.model.interfaces.QuantifiedPSMInterface;
import edu.scripps.yates.utilities.luciphor.LuciphorReader;
import edu.scripps.yates.utilities.proteomicsmodel.PSM;
import edu.scripps.yates.utilities.proteomicsmodel.Peptide;
import edu.scripps.yates.utilities.proteomicsmodel.Score;
import edu.scripps.yates.utilities.sequence.PTMInPeptide;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class LuciphorReadingTest {

	private final File luciphorFile = new File(
			"C:\\Users\\salvador\\Desktop\\HIV project\\luciphor\\luciphor_ptm_out_final_EndoH2x_HK_PNG100.txt");
	private final File quantCompareFile = new File(
			"C:\\Users\\salvador\\Desktop\\HIV project\\luciphor\\census_labelfree_out_24092_pep_EndoH2x_HK_PNG100.txt");

	@Test
	public void readingLuciphor() throws QuantParserException {
		final LuciphorReader luciphorReader = new LuciphorReader(luciphorFile);
		final Map<String, PSM> luciphorPSMs = luciphorReader.getPSMs();
		for (final PSM psm : luciphorPSMs.values()) {
			System.out.println(psm.getScanNumber() + "\t" + psm.getSequence() + "\t" + psm.getFullSequence());
		}

		// read quant compare
		final QuantCompareParser parser = new QuantCompareParser(quantCompareFile);
		final Map<String, QuantifiedPSMInterface> psmMap = parser.getPSMMap();
		// create map by scan number
		final Map<String, Set<QuantifiedPSMInterface>> psmsByScan = new THashMap<String, Set<QuantifiedPSMInterface>>();
		for (final QuantifiedPSMInterface psm : psmMap.values()) {
			final String scan = psm.getScanNumber();
			if (!psmsByScan.containsKey(scan)) {
				psmsByScan.put(scan, new THashSet<QuantifiedPSMInterface>());
			}
			psmsByScan.get(scan).add(psm);
		}
		final DecimalFormat df = new DecimalFormat("#.##");
		int numChanged = 0;
		int numLocalSignificant = 0;
		int numGlobalSignificant = 0;
		for (final PSM luciphorPSM : luciphorPSMs.values()) {
			final String scan = luciphorPSM.getScanNumber();
			if (psmsByScan.containsKey(scan)) {
				final Set<QuantifiedPSMInterface> psms = psmsByScan.get(scan);
				if (psms.size() > 1) {
					System.err.println("more than one psms with same scan number?");
				}
				final QuantifiedPSMInterface psm2 = psms.iterator().next();
				if (!luciphorPSM.getSequence().equals(psm2.getSequence())) {
					System.err.println(luciphorPSM.getSequence() + " != " + psm2.getSequence());
				}
				if (!luciphorPSM.getFullSequence().equals(psm2.getFullSequence())) {

					final List<PTMInPeptide> ptMsInPeptideLuciphor = luciphorPSM.getPTMsInPeptide();
					final List<PTMInPeptide> ptMsInPeptide = psm2.getPTMsInPeptide();
					for (int i = 0; i < ptMsInPeptideLuciphor.size(); i++) {
						final PTMInPeptide ptmLuciphor = ptMsInPeptideLuciphor.get(i);
						final PTMInPeptide ptm = ptMsInPeptide.get(i);
//						if (ptmLuciphor.getAa() == 'N') {
						if (ptmLuciphor.getPosition() != ptm.getPosition()) {
							if (luciphorPSM.getScanNumber().equals("6432")) {
								System.out.println("asdf");
								final Peptide peptide = luciphorPSM.getPeptide();
								System.out.println(peptide.getFullSequence());
							}
							System.out.print(++numChanged + " - scan " + luciphorPSM.getScanNumber() + " changed from "
									+ psm2.getFullSequence() + " to " + luciphorPSM.getFullSequence());
							for (final Score score : luciphorPSM.getScores()) {
								final Double fdr = Double.valueOf(score.getValue());
								if (fdr < 0.05) {
									if (score.getScoreName().toLowerCase().contains("local")) {
										numLocalSignificant++;
									} else {
										numGlobalSignificant++;
									}
								}
								System.out.print("  " + score.getScoreName() + " = " + df.format(fdr));
							}
							System.out.println();
						}
//						}
					}
				}
			} else {
//				System.err.println(psm + " not found " + psm.getSequence() + "\t" + psm.getScanNumber());
			}
		}
		System.out.println(numGlobalSignificant + " PSMs significant with global FDR < 0.05");
		System.out.println(numLocalSignificant + " PSMs significant with local FDR < 0.05");

	}
}
