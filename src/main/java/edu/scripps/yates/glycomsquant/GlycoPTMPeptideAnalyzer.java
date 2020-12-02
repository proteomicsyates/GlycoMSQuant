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
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TIntHashSet;

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
	public static final String HIVPOSITIONS_PEPTIDES_TO_REMOVE = "HIVPositions peptides to remove";

	private final List<QuantifiedPeptideInterface> peptideNodes;
	private final String proteinOfInterestACC;
//	private final static 
	private final String proteinSequence;
	private final AmountType amountType;
	private final String motifRegexp;
	private final boolean dontAllowConsecutiveMotifs;
	private final String referenceProteinSequence;

	public GlycoPTMPeptideAnalyzer(List<QuantifiedPeptideInterface> peptideNodes, String proteinOfInterestACC,
			AmountType amountType, String motifRegexp, boolean dontAllowConsecutiveMotifs,
			String referenceProteinSequence) {
		this.peptideNodes = peptideNodes;
		this.proteinOfInterestACC = proteinOfInterestACC;
		this.proteinSequence = ProteinSequences.getInstance().getProteinSequence(proteinOfInterestACC);
		this.amountType = amountType;
		this.motifRegexp = motifRegexp;
		this.dontAllowConsecutiveMotifs = dontAllowConsecutiveMotifs;
		this.referenceProteinSequence = referenceProteinSequence;

	}

	public GlycoPTMPeptideAnalyzer(List<QuantifiedPeptideInterface> peptideNodes, String proteinOfInterestACC,
			File fastaFile, AmountType amountType, String motifRegexp, boolean dontAllowConsecutiveMotifs,
			String referenceProteinSequence) {
		this.peptideNodes = peptideNodes;
		this.proteinOfInterestACC = proteinOfInterestACC;
		this.proteinSequence = ProteinSequences.getInstance(fastaFile, motifRegexp)
				.getProteinSequence(proteinOfInterestACC);
		this.amountType = amountType;
		this.motifRegexp = motifRegexp;
		this.dontAllowConsecutiveMotifs = dontAllowConsecutiveMotifs;
		this.referenceProteinSequence = referenceProteinSequence;
	}

	public List<GlycoSite> getGlycoSites() {
		// first create the glycosites, irrespectively of whether they are covered or
		// not by peptides
		final TIntList motifPositions = GlycoPTMAnalyzerUtil.getMotifPositions(this.proteinSequence, motifRegexp);
		// create HIVPosition objects
		final TIntObjectMap<GlycoSite> map = new TIntObjectHashMap<GlycoSite>();
		for (final int position : motifPositions.toArray()) {
			map.put(position, new GlycoSite(position, proteinOfInterestACC, referenceProteinSequence));
		}
		// now loop over the peptides to add the intensities to the covered sites
		for (final QuantifiedPeptideInterface peptide : peptideNodes) {
			final TIntArrayList positionsOfPeptideInProtein = StringUtils.allPositionsOf(proteinSequence,
					peptide.getSequence());
			if (positionsOfPeptideInProtein.size() > 1 || positionsOfPeptideInProtein.isEmpty()) {
				// discard peptides that can be mapped to multiple positions in the protein
				continue;
			}
			final List<PTMInProtein> positionsInProtein = getPTMPositionsInProtein(peptide);

			for (final PTMInProtein ptmPositionInProtein : positionsInProtein) {
				if (!ptmPositionInProtein.getProteinACC().equals(proteinOfInterestACC)) {
					continue;
				}
				final int position = ptmPositionInProtein.getPosition();

				final PTMCode ptmCodeObj = PTMCode.getByValue(ptmPositionInProtein.getDeltaMass());
				if (!map.containsKey(position)) {
					// this shoudn't happen because we already created the sites before this loop
					map.put(position, new GlycoSite(position, proteinOfInterestACC, this.referenceProteinSequence));
				}
				final List<Double> intensities = peptide.getAmounts().stream()
						.filter(a -> a.getAmountType() == amountType).map(a -> a.getValue())
						.collect(Collectors.toList());

				// each ratio corresponds to the intensity in one replicate
				for (final Double intensity : intensities) {
					// if the ratio is 0.0, it is because it was not present
					if (Double.compare(0.0, intensity) != 0) {
						map.get(position).addValue(ptmCodeObj, intensity, peptide);
					}
				}

			}
		}

		List<GlycoSite> ret = new ArrayList<GlycoSite>();
		ret.addAll(map.valueCollection());
		// sort by position
		Collections.sort(ret, new Comparator<GlycoSite>() {

			@Override
			public int compare(GlycoSite o1, GlycoSite o2) {
				return Integer.compare(o1.getPosition(), o2.getPosition());
			}
		});
		if (this.dontAllowConsecutiveMotifs) {
			ret = dontAllowConsecutiveMotifs(ret);
		}
		firePropertyChange(HIVPOSITIONS_CALCULATED, null, ret);
		return ret;
	}

	private List<GlycoSite> dontAllowConsecutiveMotifs(List<GlycoSite> sites) {
		final List<GlycoSite> ret = new ArrayList<GlycoSite>();
		final TIntSet motifsWithNoPeptides = new TIntHashSet();
		for (int i = 0; i < sites.size(); i++) {
			if (sites.get(i).getCoveredPeptides().isEmpty()) {
				motifsWithNoPeptides.add(sites.get(i).getPosition());
			}
		}
		log.info("Checking for consecutive motifs");
		for (int i = 0; i < sites.size() - 1; i++) {
			final int position = sites.get(i).getPosition();
			if (position == sites.get(i + 1).getPosition() - 1) {
				// they are consecutive
				// just keep the second site according to Saby's suggestion 16 Nov 2020

				firePropertyChange("progress", null, "Consecutive sites " + position + " and " + (position + 1)
						+ " are found. Just keeping site at " + (position + 1));
				// we continue to the next without adding it
				continue;
			}
			if (!sites.get(i).getCoveredPeptides().isEmpty()
					|| motifsWithNoPeptides.contains(sites.get(i).getPosition())) {
				ret.add(sites.get(i));
			} else {
				firePropertyChange("progress", null, "Site " + sites.get(i).getPosition()
						+ " has been removed after removing physically not possible peptides");
			}

		}
		// add the last one
		if (!sites.get(sites.size() - 1).getCoveredPeptides().isEmpty()
				|| motifsWithNoPeptides.contains(sites.get(sites.size() - 1).getPosition())) {
			ret.add(sites.get(sites.size() - 1));
		}

		return ret;
	}

	private void markSitesAsAmbiguous(GlycoSite glycoSite, GlycoSite glycoSite2) {
		glycoSite.addAmbiguousSitePosition(glycoSite2.getPosition());
		glycoSite2.addAmbiguousSitePosition(glycoSite.getPosition());
		final Set<QuantifiedPeptideInterface> peptidesRemoved = new THashSet<QuantifiedPeptideInterface>();
		peptidesRemoved
				.addAll(glycoSite.removePeptidesWithPTMInPositions(glycoSite.getPosition(), glycoSite2.getPosition()));
		peptidesRemoved
				.addAll(glycoSite2.removePeptidesWithPTMInPositions(glycoSite.getPosition(), glycoSite2.getPosition()));

		if (!peptidesRemoved.isEmpty()) {
			final PeptidesRemovedBecauseOfConsecutiveSitesWithPTM ret = new PeptidesRemovedBecauseOfConsecutiveSitesWithPTM(
					glycoSite.getPosition(), glycoSite2.getPosition(), peptidesRemoved);
			firePropertyChange(HIVPOSITIONS_PEPTIDES_TO_REMOVE, null, ret);

		}
		return;
	}

	private List<PTMInProtein> getPTMPositionsInProtein(QuantifiedPeptideInterface peptide) {
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

		final TIntList motifsPositions = GlycoPTMAnalyzerUtil.getMotifPositions(peptide, this.proteinSequence,
				motifRegexp);

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
			return getGlycoSites();
		} catch (final Exception e) {
			e.printStackTrace();
			firePropertyChange(HIVPOSITIONS_ERROR, null, e.getMessage());
		}
		return null;
	}

	public class PeptidesRemovedBecauseOfConsecutiveSitesWithPTM {
		private final int position1;
		private final int position2;
		private final Set<QuantifiedPeptideInterface> peptides;

		public PeptidesRemovedBecauseOfConsecutiveSitesWithPTM(int position1, int position2,
				Set<QuantifiedPeptideInterface> peptides) {
			this.position1 = position1;
			this.position2 = position2;
			this.peptides = peptides;
		}

		public int getPosition1() {
			return position1;
		}

		public int getPosition2() {
			return position2;
		}

		public Set<QuantifiedPeptideInterface> getPeptides() {
			return peptides;
		}

		public int size() {
			return peptides.size();
		}
	}
}
