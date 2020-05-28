package edu.scripps.yates.glycomsquant.gui.reference;

import edu.scripps.yates.glycomsquant.ProteinSequences;
import edu.scripps.yates.utilities.alignment.nwalign.NWAlign;
import edu.scripps.yates.utilities.alignment.nwalign.NWResult;
import gnu.trove.map.hash.TIntObjectHashMap;

public class MappingToReferenceHXB2 extends TIntObjectHashMap<String> {
	public final static String HXB2 = "MRVKEKYQHLWRWGWRWGTMLLGMLMICSATEKLWVTVYYGVPVWKEATTTLFCASDAKAYDTEVHNVWATHACVPTDPNPQEVVLVNVTENFNMWKNDMVEQMHEDIISLWDQSLKPCVKLTPLCVSLKCTDLKNDTNTNSSSGRMIMEKGEIKNCSFNISTSIRGKVQKEYAFFYKLDIIPIDNDTTSYKLTSCNTSVITQACPKVSFEPIPIHYCAPAGFAILKCNNKTFNGTGPCTNVSTVQCTHGIRPVVSTQLLLNGSLAEEEVVIRSVNFTDNAKTIIVQLNTSVEINCTRPNNNTRKRIRIQRGPGRAFVTIGKIGNMRQAHCNISRAKWNNTLKQIASKLREQFGNNKTIIFKQSSGGDPEIVTHSFNCGGEFFYCNSTQLFNSTWFNSTWSTEGSNNTEGSDTITLPCRIKQIINMWQKVGKAMYAPPISGQIRCSSNITGLLLTRDGGNSNNESEIFRPGGGDMRDNWRSELYKYKVVKIEPLGVAPTKAKRRVVQREKRAVGIGALFLGFLGAAGSTMGAASMTLTVQARQLLSGIVQQQNNLLRAIEAQQHLLQLTVWGIKQLQARILAVERYLKDQQLLGIWGCSGKLICTTAVPWNASWSNKSLEQIWNHTTWMEWDREINNYTSLIHSLIEESQNQQEKNEQELLELDKWASLWNWFNITNWLWYIKLFIMIVGGLVGLRIVFAVLSIVNRVRQGYSPLSFQTHLPTPRGPDRPEGIEEEGGERDRDRSIRLVNGSLALIWDDLRSLCLFSYHRLRDLLLIVTRIVELLGRRGWEALKYWWNLLQYWSQELKNSAVSLLNATAIAVAEGTDRVIEVVQGACRAIRHIPRRIRQGLERILL";

	private final String proteinAcc;

	public MappingToReferenceHXB2(String proteinAcc, String referenceProteinSequence) {
		this.proteinAcc = proteinAcc;
		final String proteinSequence = ProteinSequences.getInstance().getProteinSequence(proteinAcc);
		if (proteinSequence == null) {
			throw new IllegalArgumentException("Protein sequence not found for protein " + proteinAcc);
		}
		if (referenceProteinSequence == null) {
			throw new IllegalArgumentException("Reference protein sequence is null");
		}
		final NWResult alignment = NWAlign.needlemanWunsch(proteinSequence, referenceProteinSequence);
		final String protein = alignment.getAlignedSequence1();
		final String reference = alignment.getAlignedSequence2();
		int positionInProtein = 0;
		int positionInReference = 0;
		Character insertionLetter = null;
		for (int i = 0; i < protein.length(); i++) {
			final char aaInProtein = protein.charAt(i);
			final char aaInReference = reference.charAt(i);
			// update positions
			if (aaInProtein != '-') {
				positionInProtein++;
			}
			if (aaInReference != '-') {
				positionInReference++;
			}
			//
			if (aaInProtein != '-') {
				if (aaInReference == '-') {
					// this is an insertion
					if (insertionLetter == null) {
						insertionLetter = 'a';
					} else {
						insertionLetter++;
					}
					this.put(positionInProtein, String.valueOf(positionInReference) + insertionLetter);
				} else {
					insertionLetter = null;
					this.put(positionInProtein, String.valueOf(positionInReference));
				}
			}
		}
	}

	public String getProteinAcc() {
		return proteinAcc;
	}
}
