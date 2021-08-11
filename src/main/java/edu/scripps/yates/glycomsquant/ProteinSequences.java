package edu.scripps.yates.glycomsquant;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.compomics.dbtoolkit.io.implementations.FASTADBLoader;
import com.compomics.util.protein.Protein;

import edu.scripps.yates.annotations.uniprot.UniprotFastaRetriever;
import edu.scripps.yates.glycomsquant.gui.reference.MappingToReferenceHXB2;
import edu.scripps.yates.glycomsquant.util.ReferenceProteinIsEmptyException;
import edu.scripps.yates.utilities.annotations.uniprot.xml.Entry;
import edu.scripps.yates.utilities.fasta.FastaParser;
import edu.scripps.yates.utilities.proteomicsmodel.Accession;
import edu.scripps.yates.utilities.proteomicsmodel.enums.AccessionType;
import gnu.trove.map.hash.THashMap;

/**
 * Class that will provide the protein sequences
 * 
 * @author salvador
 *
 */
public class ProteinSequences {
	private final static Logger log = Logger.getLogger(ProteinSequences.class);
	private static ProteinSequences instance;
	private final List<File> fastaFiles = new ArrayList<File>(); // it can contain null if the sequence is provided!
	private final List<String> motifRegexps = new ArrayList<String>(); // one per fasta file
	private final Map<String, String> proteinSequencesByAcc = new THashMap<String, String>();
	private boolean loaded;
	private final Map<String, MappingToReferenceHXB2> mappingsByProtein = new THashMap<String, MappingToReferenceHXB2>();
	private final Map<String, String> motifsByProteinAccs = new THashMap<String, String>();
	public final static String REFERENCE = "HXB2";

	private ProteinSequences(File fastaFile, String motifRegexp) {
		this.fastaFiles.add(fastaFile);
		this.motifRegexps.add(motifRegexp);
		loaded = false;
	}

	public ProteinSequences(String proteinAcc, String proteinSequence, String motifRegexp2) {

		this.proteinSequencesByAcc.put(proteinAcc, proteinSequence);
		this.motifRegexps.add(motifRegexp2);
		this.fastaFiles.add(null);
		this.loaded = true;
		motifsByProteinAccs.put(proteinAcc, motifRegexp2);
	}

	/**
	 * This method assumes that you called first to one of the other getInstance()
	 * methods, so that the singleton instance is not null. Otherwise it throws an
	 * exception
	 * 
	 * @return
	 */
	public static ProteinSequences getInstance() {
		if (instance == null) {
			throw new IllegalArgumentException(
					"Call to getInstance(fasta,motifRegexp) or getInstance(acc,proteinsequence,motifRegexp) before calling here");
		}
		return instance;
	}

	/**
	 * When having a fasta file
	 * 
	 * @param fastaFile
	 * @param motifRegexp
	 * @return
	 */
	public static ProteinSequences getInstance(File fastaFile, String motifRegexp) {
		if (instance == null) {
			instance = new ProteinSequences(fastaFile, motifRegexp);
		} else if (!instance.fastaFiles.contains(fastaFile)) {
			instance.fastaFiles.add(fastaFile);
			instance.motifRegexps.add(motifRegexp);
			instance.loaded = false;
		}
		return instance;
	}

	/**
	 * When only one protein
	 * 
	 * @param proteinAcc
	 * @param proteinSequence
	 * @param motifRegexp
	 * @return
	 */
	public static ProteinSequences getInstance(String proteinAcc, String proteinSequence, String motifRegexp) {
		if (instance == null) {
			instance = new ProteinSequences(proteinAcc, proteinSequence, motifRegexp);
		} else if (!instance.containsSequence(proteinAcc)) {
			instance.fastaFiles.add(null);
			instance.motifRegexps.add(motifRegexp);
			instance.proteinSequencesByAcc.put(proteinAcc, proteinSequence);
			instance.motifsByProteinAccs.put(proteinAcc, motifRegexp);
		}
		return instance;
	}

	public boolean containsSequence(String proteinAcc) {
		if (!loaded) {
			loadFasta();
		}
		return proteinSequencesByAcc.containsKey(proteinAcc);
	}

	private void loadFasta() {
		if (!fastaFiles.isEmpty()) {
			try {
				for (int i = 0; i < fastaFiles.size(); i++) {
					File fastaFile = fastaFiles.get(i);
					String motif = motifRegexps.get(i);
					if (fastaFile != null && fastaFile.exists()) {
						final FASTADBLoader loader = new FASTADBLoader();
						try {
							if (loader.canReadFile(fastaFile)) {
								try {
									loader.load(fastaFile.getAbsolutePath());
									Protein protein = loader.nextProtein();
									while (protein != null) {
										final Accession acc = FastaParser.getACC(protein.getHeader().getRawHeader());
										if (acc != null) {
											if (acc.getAccessionType() == AccessionType.UNKNOWN) {
												final String acc2 = FastaParser.getSPorTRAccession(acc.getAccession());
												if (acc2 != null) {
													proteinSequencesByAcc.put(acc2,
															protein.getSequence().getSequence());
													motifsByProteinAccs.put(acc2, motif);
												} else {
													proteinSequencesByAcc.put(acc.getAccession(),
															protein.getSequence().getSequence());
													motifsByProteinAccs.put(acc.getAccession(), motif);
												}
											} else {
												proteinSequencesByAcc.put(acc.getAccession(),
														protein.getSequence().getSequence());
												motifsByProteinAccs.put(acc.getAccession(), motif);
											}
										}
										protein = loader.nextProtein();
									}
									log.info(proteinSequencesByAcc.size() + " protein sequences read from fasta '"
											+ fastaFile.getAbsolutePath() + "'");
									if (!proteinSequencesByAcc
											.containsKey(GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST)) {
										proteinSequencesByAcc.put(GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST,
												GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST_SEQUENCE);
										motifsByProteinAccs.put(GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST,
												GlycoPTMAnalyzer.NEW_DEFAULT_MOTIF_REGEXP);
									}
								} catch (final IOException e) {
									e.printStackTrace();
								}
							} else {
								throw new IllegalArgumentException("The fasta file '" + fastaFile.getAbsolutePath()
										+ "' is not readable. Make sure all fasta headers start by '>' symbol");
							}
						} finally {
							loader.close();
							loaded = true;
						}
					}
				}
			} finally {
				loaded = true;
			}
		}
	}

	private String getProteinSequenceFromUniprot(String proteinAcc) {
		try {
			final String uniProtACC = FastaParser.getUniProtACC(proteinAcc);
			if (uniProtACC != null) {
				log.info("Getting protein sequence from UniprotKB for " + proteinAcc);
				final Entry fastaEntry = UniprotFastaRetriever.getFastaEntry(proteinAcc);
				if (fastaEntry != null) {
					final String proteinSequence = fastaEntry.getSequence().getValue();
					return proteinSequence;
				} else {
					throw new IllegalArgumentException("Sequence from " + proteinAcc + " not found in UniprotKB.");
				}
			}
		} catch (final URISyntaxException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getProteinSequence(String proteinAcc) {
		if (proteinAcc == null || "".equals(proteinAcc)) {
			return null;
		}
		if (!loaded) {
			loadFasta();
		}
		if (proteinSequencesByAcc.containsKey(proteinAcc)) {
			return proteinSequencesByAcc.get(proteinAcc);
		} else {
			return getProteinSequenceFromUniprot(proteinAcc);
		}
	}

	public List<String> getMotifRegexps() {
		return this.motifRegexps;
	}

	public String mapPositionToReferenceProtein(String proteinAcc, int position, String referenceProteinSequence) {
		if (!mappingsByProtein.containsKey(proteinAcc + referenceProteinSequence)) {

			try {
				mappingsByProtein.put(proteinAcc + referenceProteinSequence,
						new MappingToReferenceHXB2(proteinAcc, referenceProteinSequence));
			} catch (final ReferenceProteinIsEmptyException e) {
			}
		}
		final MappingToReferenceHXB2 alignment = mappingsByProtein.get(proteinAcc + referenceProteinSequence);
		if (alignment != null) {
			final String positionInReference = alignment.get(position);
			return positionInReference;
		}
		return null;
	}

	public String getMotifRegexp(String proteinAcc) {
		String motif = motifsByProteinAccs.get(proteinAcc);
		return motif;
	}
}
