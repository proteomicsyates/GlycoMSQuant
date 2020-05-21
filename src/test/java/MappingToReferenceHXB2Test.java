import org.junit.Test;

import edu.scripps.yates.glycomsquant.AppDefaults;
import edu.scripps.yates.glycomsquant.GlycoPTMAnalyzer;
import edu.scripps.yates.glycomsquant.ProteinSequences;
import junit.framework.Assert;

public class MappingToReferenceHXB2Test {

	@Test
	public void test1() {
		final String proteinAcc = "BG505_SOSIP_gp140";
		final int[] positions = { 60, 105, 109, 120, 124, 154, 157, 169, 206, 234, 248, 267, 273, 303, 310, 326, 334,
				357, 363, 369, 376, 381, 418, 432, 583, 590, 597, 609 };
		final String[] maps = { "88", "133", "137", "156", "160", "185e", "185h", "197", "234", "262", "276", "295",
				"301", "332", "339", "355", "363", "386", "392", "398", "406", "411", "448", "462", "611", "618", "625",
				"637" };

		ProteinSequences.getInstance(AppDefaults.getDefaultProteinOfInterestInternalFastaFile(),
				GlycoPTMAnalyzer.NEW_DEFAULT_MOTIF_REGEXP);
		for (int i = 0; i < positions.length; i++) {
			final int position = positions[i];
			final String map = ProteinSequences.getInstance().mapPositionToReferenceProtein(proteinAcc, position);
			Assert.assertEquals(maps[i], map);
		}

	}
}
