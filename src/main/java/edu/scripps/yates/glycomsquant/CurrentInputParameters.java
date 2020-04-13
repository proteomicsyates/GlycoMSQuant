package edu.scripps.yates.glycomsquant;

public class CurrentInputParameters {
	private static CurrentInputParameters instance;

	public static CurrentInputParameters getInstance() {
		if (instance == null) {
			instance = new CurrentInputParameters();
		}
		return instance;
	}

	private InputParameters inputParameters;

	public void setInputParameters(InputParameters inputParameters) {
		this.inputParameters = inputParameters;
	}

	public InputParameters getInputParameters() {
		return inputParameters;
	}
}
