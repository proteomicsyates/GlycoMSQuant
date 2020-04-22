package edu.scripps.yates.glycomsquant;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.SwingWorker;

import edu.scripps.yates.annotations.uniprot.UniprotFastaRetriever;
import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.census.read.model.interfaces.QuantifiedProteinInterface;
import edu.scripps.yates.utilities.annotations.uniprot.xml.Entry;
import edu.scripps.yates.utilities.proteomicsmodel.PTM;
import edu.scripps.yates.utilities.proteomicsmodel.PTMSite;
import edu.scripps.yates.utilities.proteomicsmodel.enums.AmountType;
import edu.scripps.yates.utilities.sequence.PTMInProtein;
import edu.scripps.yates.utilities.strings.StringUtils;
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

	public static final String HIVPOSITIONS_CALCULATED = "HIVPositions_calculated";
	public static final String HIVPOSITIONS_ERROR = "HIVPositions_error";

	private final List<QuantifiedPeptideInterface> peptideNodes;
	private final String proteinOfInterestACC;
//	private final static 
	private String proteinSequence;
	private final File fastaFile;
	private final AmountType amountType;

	public GlycoPTMPeptideAnalyzer(List<QuantifiedPeptideInterface> peptideNodes, String proteinOfInterestACC,
			File fastaFile, AmountType amountType) {
		this.peptideNodes = peptideNodes;
		this.proteinOfInterestACC = proteinOfInterestACC;
		this.fastaFile = fastaFile;
		this.amountType = amountType;
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
		final String proteinSequence = getProteinSequence();
		final TIntArrayList positions = StringUtils.allPositionsOf(proteinSequence, sequence);
		if (positions.size() > 1) {
			firePropertyChange("progress", null,
					"Peptide '" + sequence + "' can be found " + positions.size() + " times in protein '"
							+ proteinOfInterestACC + "'. ONLY first ocurrence at position '" + positions.get(0)
							+ "' will be reported.");
		}
		if (positions.isEmpty()) {
			throw new IllegalArgumentException(
					"Peptide '" + sequence + "' can NOT be found in protein '" + proteinOfInterestACC + "'");
		}
		final List<PTMInProtein> ret = new ArrayList<PTMInProtein>();
		final int positionOfPeptideInProtein = positions.get(0);
		for (final PTM ptm : peptide.getPTMs()) {
			if (PTMCode.getByValue(String.valueOf(ptm.getMassShift())) == null) {
				continue;
			}
			for (final PTMSite ptmSite : ptm.getPTMSites()) {
				final int positionInPeptide = ptmSite.getPosition();
				final int positionInProtein = positionInPeptide + positionOfPeptideInProtein - 1;
				final char charAt = proteinSequence.charAt(positionInProtein - 1);
				// check whether the position + 2 is a t or S
				if (positionInPeptide + 2 <= peptide.getSequence().length()) {
					if (peptide.getSequence().charAt(positionInPeptide + 2 - 1) != 'S'
							&& peptide.getSequence().charAt(positionInPeptide + 2 - 1) != 'T') {
						// this PTM is not valid. The peptide should have another valid one
						continue;
					}
					if (charAt != ptmSite.getAA().charAt(0)) {
						throw new IllegalArgumentException("Some error calculating positions here!");
					}
					final PTMInProtein ptmInProtein = new PTMInProtein(positionInProtein, ptmSite.getAA().charAt(0),
							proteinOfInterestACC, ptm.getMassShift());
					ret.add(ptmInProtein);

				}
			}
		}
		return ret;
	}

	private String getProteinSequence() {
		if (proteinSequence == null) {
			if (proteinOfInterestACC.equals(GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST)) {
				proteinSequence = GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST_SEQUENCE;
			} else {
				try {
					final Entry fastaEntry = UniprotFastaRetriever.getFastaEntry(proteinOfInterestACC);
					if (fastaEntry != null) {
						proteinSequence = fastaEntry.getSequence().getValue();
					} else {
						throw new IllegalArgumentException("Sequence from " + proteinOfInterestACC
								+ " not found in UniprotKB. Implement getting it from FASTA if available!");
					}
				} catch (final URISyntaxException e) {
					e.printStackTrace();
				} catch (final IOException e) {
					e.printStackTrace();
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
				proteinSequence = getProteinSequenceFromFastaFile();
				if (proteinSequence == null) {
					throw new IllegalArgumentException("Sequence from " + proteinOfInterestACC
							+ " not found in UniprotKB. Implement getting it from FASTA if available!");
				}
			}
		}
		return proteinSequence;
	}

	private String getProteinSequenceFromFastaFile() {
		// TODO Auto-generated method stub
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
