package edu.scripps.yates.glycomsquant;

import java.io.File;

import edu.scripps.yates.utilities.proteomicsmodel.enums.AmountType;

public interface InputParameters {
	public File getInputFile();

	public String getProteinOfInterestACC();

	public File getFastaFile();

	public double getFakePTM();

	public String getName();

	public double getIntensityThreshold();

	public AmountType getAmountType();

	public boolean isNormalizeReplicates();

	public boolean isCalculateProportionsByPeptidesFirst();
}
