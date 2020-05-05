package edu.scripps.yates.glycomsquant;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.census.read.model.interfaces.QuantifiedProteinInterface;
import edu.scripps.yates.glycomsquant.util.GlycoPTMAnalyzerUtil;
import edu.scripps.yates.utilities.proteomicsmodel.enums.AmountType;
import edu.scripps.yates.utilities.sequence.PTMInPeptide;
import edu.scripps.yates.utilities.sequence.PTMInProtein;
import edu.scripps.yates.utilities.strings.StringUtils;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Analyzes the {@link QuantifiedPeptideInterface} generated by PCQ
 * 
 * @author salvador
 *
 */
public class GlycoPTMPeptideAnalyzer extends SwingWorker<List<GlycoSite>, Object> {
	private final static Logger log = Logger.getLogger(GlycoPTMPeptideAnalyzer.class);
	public static final String HIVPOSITIONS_CALCULATED = "HIVPositions_calculated";
	public static final String HIVPOSITIONS_ERROR = "HIVPositions_error";

	private final List<QuantifiedPeptideInterface> peptideNodes;
	private final String proteinOfInterestACC;
//	private final static 
	private final String proteinSequence;
	private final AmountType amountType;
	private final String motifRegexp;

	public GlycoPTMPeptideAnalyzer(List<QuantifiedPeptideInterface> peptideNodes, String proteinOfInterestACC,
			File fastaFile, AmountType amountType, String motifRegexp) {
		this.peptideNodes = peptideNodes;
		this.proteinOfInterestACC = proteinOfInterestACC;
		this.proteinSequence = ProteinSequences.getInstance().getProteinSequence(proteinOfInterestACC);
		this.amountType = amountType;
		this.motifRegexp = motifRegexp;
	}

	public List<GlycoSite> getHIVPositions() {
		// filter by protein
		final List<QuantifiedPeptideInterface> peptidesFilteredByProtein = peptideNodes.stream()
				.filter(p -> containsProtein(p.getQuantifiedProteins(), proteinOfInterestACC))
				.collect(Collectors.toList());
		// create HIVPosition objects
		final TIntObjectMap<GlycoSite> map = new TIntObjectHashMap<GlycoSite>();
		for (final QuantifiedPeptideInterface peptide : peptidesFilteredByProtein) {

			final List<PTMInProtein> positionsInProtein = getPositionsInProtein(peptide);

			for (final PTMInProtein positionInProtein : positionsInProtein) {
				if (!positionInProtein.getProteinACC().equals(proteinOfInterestACC)) {
					continue;
				}
				final int position = positionInProtein.getPosition();
				final String ptmCode = String.valueOf(positionInProtein.getDeltaMass());
				final PTMCode ptmCodeObj = PTMCode.getByValue(ptmCode);
				if (!map.containsKey(position)) {
					map.put(position, new GlycoSite(position, proteinOfInterestACC));
				}
				final List<Double> intensities = peptide.getAmounts().stream()
						.filter(a -> a.getAmountType() == amountType).map(a -> a.getValue())
						.collect(Collectors.toList());
				if (intensities.size() > 3) {
					System.out.println("asdf");
				}
				// each ratio corresponds to the intensity in one replicate
				for (final Double intensity : intensities) {
					// if the ratio is 0.0, it is because it was not present
					if (Double.compare(0.0, intensity) != 0) {
						map.get(position).addValue(ptmCodeObj, intensity, peptide);
					}
				}

			}
		}

		final List<GlycoSite> ret = new ArrayList<GlycoSite>();
		ret.addAll(map.valueCollection());
		// sort by position
		Collections.sort(ret, new Comparator<GlycoSite>() {

			@Override
			public int compare(GlycoSite o1, GlycoSite o2) {
				return Integer.compare(o1.getPosition(), o2.getPosition());
			}
		});
		firePropertyChange(HIVPOSITIONS_CALCULATED, null, ret);
		return ret;
	}

	private List<PTMInProtein> getPositionsInProtein(QuantifiedPeptideInterface peptide) {
		final String sequence = peptide.getSequence();
		final TIntArrayList positionsOfPeptideInProtein = StringUtils.allPositionsOf(proteinSequence, sequence);
		if (positionsOfPeptideInProtein.size() > 1) {
			firePropertyChange("progress", null,
					"Peptide '" + sequence + "' can be found " + positionsOfPeptideInProtein.size()
							+ " times in protein '" + proteinOfInterestACC + "'. ONLY first ocurrence at position '"
							+ positionsOfPeptideInProtein.get(0) + "' will be reported.");
		}
		if (positionsOfPeptideInProtein.isEmpty()) {
			throw new IllegalArgumentException(
					"Peptide '" + sequence + "' can NOT be found in protein '" + proteinOfInterestACC + "'");
		}

		final TIntList motifsPositions = GlycoPTMAnalyzerUtil.hasMotif(peptide, this.proteinSequence, motifRegexp);

		final List<PTMInProtein> ret = new ArrayList<PTMInProtein>();
		for (final int positionOfPeptideInProtein : positionsOfPeptideInProtein.toArray()) {

			for (final int positionInPeptide : motifsPositions.toArray()) {
				final int positionInProtein = positionInPeptide + positionOfPeptideInProtein - 1;
				final PTMInPeptide ptm = getPTMAtPosition(peptide, positionInPeptide);
				Double deltaMass = 0.0;
				if (ptm != null) {
					deltaMass = ptm.getDeltaMass();
				} else {
					// if ptm not found, it is because is a non-PTM
				}

				final PTMInProtein ptmInProtein = new PTMInProtein(positionInProtein,
						sequence.charAt(positionInPeptide - 1), proteinOfInterestACC, deltaMass);
				ret.add(ptmInProtein);

			}

		}
		return ret;
	}

	/**
	 * Looks for a PTM in the peptide that is in a certain position
	 * 
	 * @param peptide
	 * @param positionInPeptide
	 * @return
	 */
	private PTMInPeptide getPTMAtPosition(QuantifiedPeptideInterface peptide, int positionInPeptide) {
		for (final PTMInPeptide ptmInPeptide : peptide.getPTMsInPeptide()) {
			if (ptmInPeptide.getPosition() == positionInPeptide) {
				return ptmInPeptide;
			}
		}
		return null;
	}

	private boolean containsProtein(Set<QuantifiedProteinInterface> proteins, String proteinAcc) {
		for (final QuantifiedProteinInterface protein : proteins) {
			if (protein.getAccession().equals(proteinAcc)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected List<GlycoSite> doInBackground() throws Exception {
		try {
			return getHIVPositions();
		} catch (final Exception e) {
			e.printStackTrace();
			firePropertyChange(HIVPOSITIONS_ERROR, null, e.getMessage());
		}
		return null;
	}
}
