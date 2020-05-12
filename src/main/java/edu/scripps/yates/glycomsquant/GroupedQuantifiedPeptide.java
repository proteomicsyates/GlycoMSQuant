package edu.scripps.yates.glycomsquant;

import java.util.Collection;
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
	private int chargeState;
	private String sequence;
	private TMap<PTMCode, TDoubleList> intensitiesByPTMCode;
	private final String proteinAcc;

	/**
	 * 
	 * @param peptides
	 * @param proteinAcc with respect to what protein we are working on
	 */
	public GroupedQuantifiedPeptide(Collection<QuantifiedPeptideInterface> peptides, String proteinAcc) {
		this.proteinAcc = proteinAcc;
		addAll(peptides);

	}

	public GroupedQuantifiedPeptide(QuantifiedPeptideInterface peptide, String proteinAcc) {
		this.proteinAcc = proteinAcc;
		add(peptide);
		chargeState = peptide.getPSMs().get(0).getChargeState();
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
		return GlycoPTMAnalyzerUtil.getIndividualProportionsByPTMCode(this, sumIntensitiesAcrossReplicates)
				.get(ptmCode);
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
}
