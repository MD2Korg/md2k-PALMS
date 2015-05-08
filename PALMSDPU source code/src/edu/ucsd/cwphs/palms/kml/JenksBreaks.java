package edu.ucsd.cwphs.palms.kml;

import java.util.ArrayList;
import java.util.Comparator;

public class JenksBreaks {

	// returns array of indexes of breaks occurring within the sorted ArrayList
	
	static public int[] getJenksBreaks(ArrayList<Double> list, int numclass) {
		int k = 0;
		int numdata = list.size();
		int[] kclass = new int[numclass];
		double[][] mat1 = new double[numdata + 1][numclass + 1];
		double[][] mat2 = new double[numdata + 1][numclass + 1];
//		double[] st = new double[numdata];   // not used and appears not to be needed

		try {
		for (int i = 1; i <= numclass; i++) {
			mat1[1][i] = 1;
			mat2[1][i] = 0;
			for (int j = 2; j <= numdata; j++)
				mat2[j][i] = Double.MAX_VALUE;
		}
		double v = 0;
		for (int l = 2; l <= numdata; l++) {
			double s1 = 0;
			double s2 = 0;
			double w = 0;
			for (int m = 1; m <= l; m++) {
				int i3 = l - m + 1;

				double val = ((Double)list.get(i3-1)).doubleValue();
				s2 += val * val;
				s1 += val;
				w++;
				v = s2 - (s1 * s1) / w;
				int i4 = i3 - 1;
				if (i4 != 0) {
					for (int j = 2; j <= numclass; j++) {
						if (mat2[l][j] >= (v + mat2[i4][j- 1])) {
							mat1[l][j] = i3;
							mat2[l][j] = v + mat2[i4][j - 1];

						};
					};
				};
			};
			mat1[l][1] = 1;
			mat2[l][1] = v;
		};
		k = numdata;
		kclass[numclass - 1] = list.size() - 1;
		for (int j = numclass; j >= 2; j--) {
//			System.out.println("rank = " + mat1[k][j]);
			int id =  (int) (mat1[k][j]) - 2;
//			System.out.println("val = " + list.get(id));
			//System.out.println(mat2[k][j]);
			kclass[j - 2] = id;
			k = (int) mat1[k][j] - 1;
		};
		} // end try
		catch (Exception ex){
//			EventLogger.logException("JenksBreaks - ", ex);
//			EventLogger.logEvent("JenksBreaks - numclass = "+numclass + " k = " + k);
			return null;
		}
		return kclass;
	}

	class doubleComp implements Comparator<Double> {
		public int compare(Double a, Double b) {
			if ((a).doubleValue() < (b).doubleValue())
				return -1;
			if ((a).doubleValue() > (b).doubleValue())
				return 1;
			return 0;
		}
	}
}
