package edu.scripps.yates.glycomsquant.util;

import java.util.List;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.gui.files.ResultsProperties;

public class ResultsLoadedFromDisk {
	private final List<GlycoSite> sites;
	private final List<QuantifiedPeptideInterface> peptides;
	private final boolean sumIntensitiesAcrossReplicates;
	private final ResultsProperties resultProperties;

	public ResultsLoadedFromDisk(ResultsProperties resultProperties, List<GlycoSite> sites,
			List<QuantifiedPeptideInterface> peptides, boolean sumIntensitiesAcrossReplicates) {
		this.resultProperties = resultProperties;
		this.sites = sites;
		this.peptides = peptides;
		this.sumIntensitiesAcrossReplicates = sumIntensitiesAcrossReplicates;
	}

	public List<GlycoSite> getSites() {
		return sites;
	}

	public List<QuantifiedPeptideInterface> getPeptides() {
		return peptides;
	}

	public boolean isSumIntensitiesAcrossReplicates() {
		return sumIntensitiesAcrossReplicates;
	}

	public ResultsProperties getResultProperties() {
		return resultProperties;
	}

}
