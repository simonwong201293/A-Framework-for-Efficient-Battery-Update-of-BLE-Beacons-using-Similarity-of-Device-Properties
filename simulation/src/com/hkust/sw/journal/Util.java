package com.hkust.sw.journal;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;

public class Util {
	public static boolean panda = true;
	public static double acceptance = 1;
	public static long userAction = 60;
	public static long lastTimestamp = 0;
	public static HashMap<String, Boolean> done = new HashMap<>();
	public static HashMap<String, Long> firstTime = new HashMap<>();
	public static HashMap<String, Long> lastTime = new HashMap<>();
	public static String inputParent = "C:\\\\Users\\\\sw-admin\\\\Desktop\\\\ÐÂÔöÙYÁÏŠA\\\\";
	public static String inputPath = inputParent + "beacons_report_203233f4dacc_09072020.csv";
	public static String outputParent = inputParent+"pa_"+acceptance+"_"+userAction+"\\\\";
	public static String outputPath = inputPath.replace(".csv", "_est.csv").replace(inputParent, outputParent);
	public static String outputPath2 = inputPath.replace(".csv", "_raw.csv").replace(inputParent, outputParent);
	public static String outputPath3 = inputPath.replace(".csv", "_avg_err.csv").replace(inputParent, outputParent);
	public static String outputPath4 = inputPath.replace(".csv", "_acc.csv").replace(inputParent, outputParent);
	
	public static void exportAvgError(HashMap<Long, Double> errorList) {
		if(!new File(Util.outputPath3).getParentFile().exists()) {
			new File(Util.outputPath3).getParentFile().mkdirs();
		}
		if(new File(Util.outputPath3).exists()) {
			new File(Util.outputPath3).delete();
		}
		try {
			FileWriter fw = new FileWriter(Util.outputPath3);
			fw.write("Time, Avg. Error\n");
			for(Long time : errorList.keySet()) {	
				fw.write(time + "," + errorList.get(time) + "\n");
			}
			fw.close();
		}catch(Exception ignored) {}
	}
	
	public static void exportAcc(HashMap<Long, Integer[]> accList) {
		if(!new File(Util.outputPath4).getParentFile().exists()) {
			new File(Util.outputPath4).getParentFile().mkdirs();
		}
		if(new File(Util.outputPath4).exists()) {
			new File(Util.outputPath4).delete();
		}
		try {
			FileWriter fw = new FileWriter(Util.outputPath4);
			fw.write("Time, Accumulated Record, Original Record\n");
			for(Long time : accList.keySet()) {	
				fw.write(time + "," + accList.get(time)[0] + "," + accList.get(time)[1] + "\n");
			}
			fw.close();
		}catch(Exception ignored) {}
	}
	
}
