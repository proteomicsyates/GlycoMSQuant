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
}
