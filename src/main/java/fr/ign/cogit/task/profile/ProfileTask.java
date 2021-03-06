package fr.ign.cogit.task.profile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import fr.ign.cogit.exec.ProfileCalculation;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IDirectPosition;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.sig3d.analysis.streetprofile.BuildingProfileParameters;
import fr.ign.cogit.geoxygene.sig3d.analysis.streetprofile.Profile;
import fr.ign.cogit.geoxygene.sig3d.analysis.streetprofile.pattern.Pattern;
import fr.ign.cogit.geoxygene.sig3d.analysis.streetprofile.pattern.ProfilePatternDetector;
import fr.ign.cogit.geoxygene.sig3d.analysis.streetprofile.stats.ProfileBasicStats;
import fr.ign.cogit.geoxygene.sig3d.analysis.streetprofile.stats.ProfileMoran;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPosition;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;

public class ProfileTask {

	public static void main(String[] args) throws Exception {
		File folderOut = new File("/home/mbrasebin/tmp/test/");
		
		String[] dirNames = { "84284"};
		
		double stepXY = 1;
		double stepZ = 1;
		double maxDist = 200;

		double correlationThreshold = 0.8;

		int minimalPeriod = 20;

		int numberOfMinimalRepeat = 1;

		int maxPatternLength = 200;

		int maxRepeat = 10;

		String heightAttribute = "HAUTEUR";
		
		
		for(String dirName : dirNames) {
			
			
			File roadsFile = new File("/home/mbrasebin/.openmole/ZBOOK-SIGOPT-2016/webui/projects/ProfileDistribution/data/"
					+ dirName + "/road.shp");
			File buildingsFile = new File(
					"/home/mbrasebin/.openmole/ZBOOK-SIGOPT-2016/webui/projects/ProfileDistribution/data/" + dirName
							+ "/buildings.shp");

			

			run(new File(folderOut+ "/" +dirName), roadsFile, buildingsFile, stepXY, stepZ, maxDist, correlationThreshold, minimalPeriod,
					heightAttribute, dirName, numberOfMinimalRepeat, maxPatternLength, maxRepeat);
		}
		
		


	}

	public static File runDefault(File folderOut, File folderIn, double stepXY, double stepZ, double maxDist,
			double correlationThreshold, int minimalPeriod, String heightAttribute, String dirName,
			int numberOfMinimalRepeat, int maxPatternLength, int maxRepeat) throws Exception {
		return run(folderOut, new File(folderIn, "road.shp"), new File(folderIn, "buildings.shp"), stepXY, stepZ,
				maxDist, correlationThreshold, minimalPeriod, heightAttribute, dirName, numberOfMinimalRepeat,
				maxPatternLength, maxRepeat);

	}

	public static File run(File folderOut, File roadsFile, File buildingsFile, double stepXY, double stepZ,
			double maxDist, double correlationThreshold, int minimalPeriod, String heightAttribute, String dirName,
			int numberOfMinimalRepeat, int maxPatternLength, int maxRepeat) throws Exception {

		BuildingProfileParameters.ID = "GID";
		// Preparing outputfolder
		System.out.println("folder out = " + folderOut);
		if (!folderOut.exists()) {
			folderOut.mkdirs();
			if (folderOut.exists())
				System.out.println("I had to create it though");
			else {
				System.out.println("I could not create it...");
				throw new Exception("Could not create temp directory");
			}
		} else {
			System.out.println("We're all good!");
		}

		// Reading roads
		System.out.println("Reading roads ");
		IFeatureCollection<IFeature> roads = readShapefile(roadsFile.getParentFile(), roadsFile.getName());

		if (roads == null || roads.isEmpty()) {
			return folderOut;
		}

		System.out.println("Reading buildings");

		// Reading and extruding buildings
		IFeatureCollection<IFeature> buildings = ProfileCalculation.prepareBuildingCollection(
				getFileName(buildingsFile.getParentFile(), buildingsFile.getName()), heightAttribute);

		if (buildings == null || buildings.isEmpty()) {
			return folderOut;
		}

		DirectPosition.PRECISION = 10;
		// Preparing profile
		Profile profile = new Profile(roads,
				// Set of contigus roads from which the profil is calculated
				buildings,
				// 3D buildings used

				null);

		// Setting attributes
		profile.setXYStep(stepXY);
		profile.setZStep(stepZ);
		profile.setLongCut(maxDist);

		profile.setDisplayInit(true);

		System.out.println("Loading data");
		profile.loadData(false);
		System.out.println("Processing");
		profile.process();

		System.out.println("Writing output");
		// Writing point profile

		
		//profile.exportPoints(folderOut + "/outprofile.shp");

		writePointOut(profile.getPproj(), folderOut, dirName, "outpointsProfile.csv");

		// Writing points on geographic coordinate system
		IFeatureCollection<IFeature> ft1 = profile.getBuildingSide1();
		IFeatureCollection<IFeature> ft2 = profile.getBuildingSide2();

		IFeatureCollection<IFeature> featCollPointOut = new FT_FeatureCollection<>();
		if (ft1 != null && !ft1.isEmpty()) {
			featCollPointOut.addAll(ft1);
		}

		if (ft2 != null && !ft2.isEmpty()) {
			featCollPointOut.addAll(ft2);
		}

		////////////////////// Writing shapefile output

		System.out.println("Export points");
		// ShapefileWriter.write(featCollPointOut, folderOut + "/outpoints.shp");
		writePointOut(featCollPointOut, folderOut, dirName, "outpoints.csv");

		System.out.println("Export debug");
		IFeatureCollection<IFeature> featCOut = profile.getFeatOrthoColl();
		ShapefileWriter.write(featCOut, folderOut + "/debug.shp");

		///////////////// WRITING STATISTICS

		////////////// GLOBAL STATISTICS

		///////////////////////////////// UPPER PROFILE STATS

		System.out.println("Export global upper");
		writeGlobalStats(profile, Profile.SIDE.UPSIDE, folderOut, dirName, maxDist);

		///////////////////////////////// DOWN PROFILE STATS
		System.out.println("Export global down");
		writeGlobalStats(profile, Profile.SIDE.DOWNSIDE, folderOut, dirName, maxDist);

		////////////// LOCAL STATISTICS

		///////////////////////////////// UPPER PROFILE STATS

		System.out.println("Export local upper");
		writeLocalStats(profile, Profile.SIDE.UPSIDE, folderOut, dirName, minimalPeriod, correlationThreshold,
				numberOfMinimalRepeat, maxPatternLength, maxRepeat);

		///////////////////////////////// DOWN PROFILE STATS
		System.out.println("Export local downer");
		writeLocalStats(profile, Profile.SIDE.DOWNSIDE, folderOut, dirName, minimalPeriod, correlationThreshold,
				numberOfMinimalRepeat, maxPatternLength, maxRepeat);

		System.out.println("Taks end");
		return folderOut;
	}

	public static void writePointOut(IFeatureCollection<IFeature> fPoints, File folderOut, String dirName,
			String fileName) throws IOException {
		BufferedWriter writerPattern = new BufferedWriter(new FileWriter(new File(folderOut, fileName), true));

		for (IFeature feat : fPoints) {

			IDirectPosition dp = feat.getGeom().coord().get(0);

			writerPattern.append(dirName + ";");
			writerPattern.append(dp.getX() + ";");
			writerPattern.append(dp.getY() + ";");
			writerPattern.append(dp.getZ() + ";");
			writerPattern.append(feat.getAttribute(BuildingProfileParameters.ID) + ";");
			writerPattern.append(feat.getAttribute(BuildingProfileParameters.NAM_ATT_DISTANCE) + "\n");
		}

		writerPattern.close();

	}

	public static void writeLocalStats(Profile profile, Profile.SIDE s, File folderOut, String dirName,
			int minimalPeriod, double correlationThreshold, int numberOfMinimalRepeat, int maxPatternLength,
			int maxRepeat) throws IOException {

		ProfilePatternDetector pPD = new ProfilePatternDetector(minimalPeriod);
		// HEADER : "dirName;begin;length;repeat;correlation"

		BufferedWriter writerPattern = new BufferedWriter(new FileWriter(new File(folderOut, "patternOut.csv"), true));

		HashMap<Integer, List<Pattern>> patternListUp = pPD.patternDetector(profile, s, correlationThreshold, numberOfMinimalRepeat, maxPatternLength, maxRepeat);

		if (!patternListUp.isEmpty()) {

			for (Integer length : patternListUp.keySet()) {

				List<Pattern> lP = patternListUp.get(length);

				for (Pattern p : lP) {

					writerPattern.append(dirName + ";");
					writerPattern.append(s + ";");
					writerPattern.append(p.getIndexBegin() + ";");
					writerPattern.append(p.getLength() + ";");
					writerPattern.append(p.getRepeat() + ";");
					writerPattern.append(p.getCorrelationScore() + "\n");

				}

			}

		}

		writerPattern.close();

	}

	public static void writeGlobalStats(Profile profile, Profile.SIDE s, File folderOut, String dirName, double maxDist)
			throws IOException {

		List<Double> heights = profile.getHeightAlongRoad(s);

		if (heights == null || heights.isEmpty()) {
			return;
		}

		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(folderOut, "output.csv"), true));

		// HEADER = "dirname;side;minH;maxH;avgH;medH;moran;"

		// Identifing information
		writer.append(dirName + ";");
		writer.append(s + ";");

		// Moran up
		ProfileMoran pM = new ProfileMoran();
		pM.calculate(heights);
		double moranProfileValue = pM.getMoranProfileFinal();

		// Basic stats
		ProfileBasicStats pBS = new ProfileBasicStats();
		pBS.calculate(heights);

		// Height autocorrelation : up

		// ProfileAutoCorrelation pAC = new ProfileAutoCorrelation();
		// pAC.calculateACF(heights);
		// pAC.calculateMethodYin(heights);

		// Height Depth Autocorrelation
		// ProfileMultiDimensionnalCorrelation pMDC = new
		// ProfileMultiDimensionnalCorrelation();
		// pMDC.calculate(profile, s, maxDist, pBS.getMax());

		writer.append(pBS.getMin() + ";");
		writer.append(pBS.getMax() + ";");
		writer.append(pBS.getMoy() + ";");
		writer.append(pBS.getMed() + ";");
		writer.append(pBS.getStd() + ";");
		writer.append(moranProfileValue + "\n");

		writer.close();
	}

	private static File findFile(File folder, String filename) {
		for (File file : folder.listFiles()) {
			if (file.getName().matches("(?i)" + filename)) {
				return file;
			}
		}
		return null;
	}

	private static String getFileName(File folder, String filename) {
		File file = findFile(folder, filename);
		if (file == null) {
			return null;
		}
		return file.toString();
	}

	private static IFeatureCollection<IFeature> readShapefile(File folder, String filename) {
		String name = getFileName(folder, filename);
		if (null == name) {

			return new FT_FeatureCollection<>();
		}

		return ShapefileReader.read(name);
	}

}
