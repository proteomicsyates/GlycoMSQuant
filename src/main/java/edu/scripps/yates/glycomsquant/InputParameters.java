package edu.scripps.yates.glycomsquant;

import java.io.File;

import edu.scripps.yates.utilities.proteomicsmodel.enums.AmountType;

public interface InputParameters {
	public File getInputFile();

	public String getProteinOfInterestACC();

	public File getFastaFile();

	public File getLuciphorFile();

	public String getName();

	public Double getIntensityThreshold();

	public AmountType getAmountType();

	public Boolean isNormalizeReplicates();

	public Boolean isDiscardNonUniquePeptides();

	public Boolean isSumIntensitiesAcrossReplicates();

	public String getMotifRegexp();

	public Boolean isDiscardWrongPositionedPTMs();

	public Boolean isDontAllowConsecutiveMotifs();

	public String getReferenceProteinSequence();

	public Boolean isFixWrongPositionedPTMs();

	public Boolean isDiscardPeptidesWithNoMotifs();

	/**
	 * Whether is using the charge to group peptides with the same sequence (and
	 * charge if true) so that then the proportions are calculated for each grouped
	 * peptide and then averaged.
	 * 
	 * @return
	 */
	public Boolean isUseCharge();

	public Boolean isDiscardPeptidesRepeatedInProtein();
}
