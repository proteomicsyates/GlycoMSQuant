package edu.scripps.yates.glycomsquant;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPSMInterface;
import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.util.GlycoPTMAnalyzerUtil;
import gnu.trove.list.TDoubleList;
import gnu.trove.map.TMap;
import gnu.trove.set.hash.THashSet;

/**
 * This class represents a group of peptides that share the same
 * cleanSequence+charge, but they could have different PTMs
 * 
 * @author salvador
 *
 */
public class GroupedQuantifiedPeptide extends THashSet<QuantifiedPeptideInterface> {
	private final static Logger log = Logger.getLogger(GroupedQuantifiedPeptide.class);
	private String key;
	private String keyWithNoCharge;
	private final int chargeState;
	private String sequence;
	private TMap<PTMCode, TDoubleList> intensitiesByPTMCode;
	private final String proteinAcc;
	private Integer positionInPeptide;
	private final int startingPositionInProtein;

	/**
	 * 
	 * @param peptide
	 * @param proteinAcc
	 * @param positionInPeptide position in the peptide for which this
	 *                          {@link GroupedQuantifiedPeptide} was created
	 */
	public GroupedQuantifiedPeptide(QuantifiedPeptideInterface peptide, String proteinAcc, Integer positionInPeptide) {
		this.proteinAcc = proteinAcc;
		this.startingPositionInProtein = GlycoPTMAnalyzerUtil.getPositionsInProtein(peptide, proteinAcc);
		this.positionInPeptide = positionInPeptide;
		chargeState = peptide.getPSMs().get(0).getChargeState();
		add(peptide);

	}

	/**
	 * Same constructor as the other but not specifying the position in peptide for
	 * which we want the data. This is used in the advanced inspection, because
	 * sometimes we want data from other positions.
	 * 
	 * @param peptide
	 * @param proteinAcc
	 * 
	 */
	public GroupedQuantifiedPeptide(QuantifiedPeptideInterface peptide, String proteinAcc) {
		this(peptide, proteinAcc, null);
	}

	/**
	 * The protein for which this {@link GroupedQuantifiedPeptide} was created
	 * 
	 * @return
	 */
	public String getProteinAcc() {
		return this.proteinAcc;
	}

	@Override
	public boolean add(QuantifiedPeptideInterface peptide) {
		if (key != null) {
			final String key2 = GlycoPTMAnalyzerUtil.getPeptideKey(peptide, true);
			if (!key2.equals(key)) {
				throw new IllegalArgumentException(
						"peptide " + peptide.getFullSequence() + " cannot be grouped in group with key=" + this.key);
			}
		} else {
			key = GlycoPTMAnalyzerUtil.getPeptideKey(peptide, true);
			keyWithNoCharge = GlycoPTMAnalyzerUtil.getPeptideKey(peptide, true);
		}
		return super.add(peptide);
	}

	@Override
	public boolean addAll(Collection<? extends QuantifiedPeptideInterface> collection) {
		boolean added = false;
		for (final QuantifiedPeptideInterface peptide : collection) {
			added = added || add(peptide);
		}
		return added;
	}

	public String getKey(boolean removeCharge) {
		if (!removeCharge) {
			return key;
		} else {
			return keyWithNoCharge;
		}
	}

	public int getChargeState() {
		return chargeState;
	}

	public Set<QuantifiedPSMInterface> getQuantifiedPSMs() {
		final Set<QuantifiedPSMInterface> ret = new THashSet<QuantifiedPSMInterface>();
		stream().forEach(peptide -> ret.addAll(peptide.getQuantifiedPSMs()));
		return ret;
	}

	/**
	 * Returns true if this {@link GroupedQuantifiedPeptide} contains any of the
	 * peptides in the argument
	 * 
	 * @param peptides
	 * @return
	 */
	public boolean containsAny(Set<QuantifiedPeptideInterface> peptides) {
		for (final QuantifiedPeptideInterface pep : peptides) {
			if (contains(pep)) {
				return true;
			}
		}
		return false;
	}

	public TDoubleList getProportionsByPTMCode(PTMCode ptmCode, boolean sumIntensitiesAcrossReplicates) {

		final Map<PTMCode, TDoubleList> individualProportionsByPTMCode = GlycoPTMAnalyzerUtil
				.getIndividualProportionsByPTMCode(this, sumIntensitiesAcrossReplicates);
		if (individualProportionsByPTMCode.containsKey(ptmCode)) {
			return individualProportionsByPTMCode.get(ptmCode);
		}
		throw new IllegalArgumentException(
				"No proportion data for position " + getPositionInPeptide() + " in peptide " + getKey(false));
	}

	public String getSequence() {
		if (sequence == null) {
			sequence = iterator().next().getSequence();
		}
		return sequence;
	}

	public TDoubleList getIntensitiesByPTMCode(PTMCode ptmCode) {
		if (intensitiesByPTMCode == null) {
			intensitiesByPTMCode = GlycoPTMAnalyzerUtil.getIntensitiesByPTMCode(this);

		}
		return intensitiesByPTMCode.get(ptmCode);
	}

	/**
	 * position in the peptide for which this {@link GroupedQuantifiedPeptide} was
	 * created
	 * 
	 * @return
	 */
	public Integer getPositionInPeptide() {
		return positionInPeptide;
	}

	public void setPositionInPeptide(Integer positionInPeptide) {
		this.positionInPeptide = positionInPeptide;
	}

	/**
	 * Sets the position in peptide that determines the current position to look,
	 * but using the position in the protein, so this function wil translate that
	 * position to a position in the peptide.
	 * 
	 * @param positionInProtein if null, the position in peptide will be set to null
	 */
	public void setPositionInPeptideWithPositionInProtein(Integer positionInProtein) {
		if (positionInProtein == null) {
			setPositionInPeptide(null);
		} else {
			final int positionInPeptide2 = positionInProtein - startingPositionInProtein + 1;
			if (positionInPeptide2 < 1 || positionInPeptide2 > getSequence().length()) {
				setPositionInPeptide(null);
			} else {
				setPositionInPeptide(positionInPeptide2);
			}
		}
	}
}
