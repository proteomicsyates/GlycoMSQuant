package edu.scripps.yates.glycomsquant.util;

import java.util.List;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.gui.files.ResultsProperties;

public class ResultsLoadedFromDisk {
	private final List<GlycoSite> sites;
	private final List<QuantifiedPeptideInterface> peptides;
	private final boolean calculatePeptideProportionsFirst;
	private final ResultsProperties resultProperties;

	public ResultsLoadedFromDisk(ResultsProperties resultProperties, List<GlycoSite> sites,
			List<QuantifiedPeptideInterface> peptides, boolean calculatePeptideProportionsFirst) {
		this.resultProperties = resultProperties;
		this.sites = sites;
		this.peptides = peptides;
		this.calculatePeptideProportionsFirst = calculatePeptideProportionsFirst;
	}

	public List<GlycoSite> getSites() {
		return sites;
	}

	public List<QuantifiedPeptideInterface> getPeptides() {
		return peptides;
	}

	public boolean isCalculatePeptideProportionsFirst() {
		return calculatePeptideProportionsFirst;
	}

	public ResultsProperties getResultProperties() {
		return resultProperties;
	}

}
