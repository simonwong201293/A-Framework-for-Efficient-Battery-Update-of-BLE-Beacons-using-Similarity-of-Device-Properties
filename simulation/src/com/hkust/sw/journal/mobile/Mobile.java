package com.hkust.sw.journal.mobile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.hkust.sw.journal.Main;
import com.hkust.sw.journal.Util;
import com.hkust.sw.journal.mobile.controller.MobileEstimationController;
import com.hkust.sw.journal.mobile.model.MobileDetectedModel;
import com.hkust.sw.journal.mobile.model.MobileEstimatedModel;
import com.hkust.sw.journal.server.model.SuggestedRefModel;

public class Mobile extends Thread{
	
	public interface MobileCallback{
		public void reportRecord(MobileDetectedModel reportedRecord);
		public void onMobileCompleted();
	}
	
	public Set<String> workQueue = new HashSet<>();
	public Set<String> idList = new HashSet<>();
	public HashMap<String, List<MobileDetectedModel>> receivedSignals = new HashMap<>();
	public final MobileEstimationController estimationController;
	private final MobileCallback mCallback;
	
	public Mobile(MobileCallback callback, boolean random) {
		mCallback = callback;
		initRecords(Util.inputPath, "CR2477", 1, random);
		estimationController = new MobileEstimationController();
	}
	
	public Set<String> getBeaconIds(){
		return idList;
	}
	
	private void initRecords(String path, String batteryModel, int lg, boolean random){
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(path));
			String line = reader.readLine();
			while (line != null) {
				line = reader.readLine();
				if(line != null && line.length() > 0 && !line.contains("id") && !line.contains("0_0") /*&& !line.contains("0170") && !line.contains("2200")*/) {
					if(random && new Random().nextInt(100) > 50) continue;
					String[] s = line.split(",");
					MobileDetectedModel model = new MobileDetectedModel();
					model.beaconId = s[2].trim().toLowerCase();
					model.batteryType = batteryModel;
					model.locationGroup = lg;
					model.configurationGroup = model.beaconId.contains("0170") ? 2 : model.beaconId.contains("2200") ? 3 : 1;
					model.time = Long.parseLong(s[14]);
					model.voltage = Double.parseDouble(s[11]) * 3.6 / 100.0;
					idList.add(s[2]);
					if(!receivedSignals.containsKey(model.beaconId))
						receivedSignals.put(model.beaconId, new ArrayList<>());
					receivedSignals.get(model.beaconId).add(model);
					Collections.sort(receivedSignals.get(model.beaconId), new Comparator<MobileDetectedModel>(){
						@Override
						public int compare(MobileDetectedModel arg0, MobileDetectedModel arg1) {
							// TODO Auto-generated method stub
							return Long.compare(arg0.time, arg1.time);
						}
					});
					if(!Util.firstTime.containsKey(model.beaconId)) {
						Util.firstTime.put(model.beaconId, model.time);
					}
				}
			}
			reader.close();
			for(String id : receivedSignals.keySet()) {
				Util.lastTime.put(id, receivedSignals.get(id).get(receivedSignals.get(id).size()-1).time);
			}
		} catch (IOException ignored) {
		}
	}
	
	public void exportRaw() {
		if(!new File(Util.outputPath2).getParentFile().exists()) {
			new File(Util.outputPath2).getParentFile().mkdirs();
		}
		if(new File(Util.outputPath2).exists()) {
			new File(Util.outputPath2).delete();
		}
		try {
			FileWriter fw = new FileWriter(Util.outputPath2);
			if(!Util.panda) {
				Set<Long> timeSet = new HashSet<>();
				HashMap<String, HashMap<Long, Double>> values = new HashMap<>();
				for (String id : receivedSignals.keySet()) {
					if(!values.containsKey(id)) values.put(id, new HashMap<>());
					for(MobileDetectedModel model : receivedSignals.get(id)) {
						timeSet.add(model.time);
						values.get(id).put(model.time, model.voltage);
					}
				}
				Main.rawVal = values;
				Long[] times = timeSet.toArray(new Long[timeSet.size()]);
				Arrays.sort(times);
				Util.lastTimestamp = times[times.length - 1];
				String[] timeStrs = new String[times.length];
				for(int i = 0 ; i < times.length; i++)
					timeStrs[i] = String.valueOf(times[i]);
				fw.write("BeaconId," + String.join(",", timeStrs) + "\n");
				for(String id : values.keySet()) {
					StringBuilder sb = new StringBuilder();
					sb.append(id+",");
					for(Long time : times) {
						if(values.get(id).containsKey(time)) {
							sb.append(values.get(id).get(time) + ",");
						}else {
							sb.append(",");
						}
					}
					fw.write(sb.toString() + "\n");
				}
			}else {
				Set<Long> timeSet = new HashSet<>();
				HashMap<String, HashMap<Long, Double>> values = new HashMap<>();
				for (String id : receivedSignals.keySet()) {
					if(!values.containsKey(id)) values.put(id, new HashMap<>());
					for(MobileDetectedModel model : receivedSignals.get(id)) {
						timeSet.add(model.time);
						values.get(id).put(model.time, model.voltage);
					}
				}
				Main.rawVal = values;
				fw.write("BeaconId, Raw Voltage, Time\n");
				List<MobileDetectedModel> tmpList = new ArrayList<>();
				for (String id : receivedSignals.keySet()) {
					tmpList.addAll(receivedSignals.get(id));
				}
				Collections.sort(tmpList, new Comparator<MobileDetectedModel>() {
					@Override
					public int compare(MobileDetectedModel o1, MobileDetectedModel o2) {
						return Long.compare(o1.time, o2.time);
					}
				});
				for(MobileDetectedModel model : tmpList) {
					fw.write(model.beaconId + "," + model.voltage + "," + model.time + "\n");
				}
			}
			fw.close();
		}catch(Exception ignored) {}
	}
	
	private MobileDetectedModel findClosestModel(String beaconId, long time) {
		if(receivedSignals.containsKey(beaconId)) {
			for(MobileDetectedModel model : receivedSignals.get(beaconId)) {
				if(model.time >= time) return model;
			}
			return null;
		}else return null;
	}
	
	public void requestBeaconInfo(List<SuggestedRefModel> datas, long time) {
		if(datas != null)
			for(SuggestedRefModel data : datas) {
				if(data == null) {
//					System.out.println("data = null"); 
					continue;
					}
				MobileDetectedModel closestModel = findClosestModel(data.beaconId, time);
				if(closestModel != null) {
					if(data.refRecord1 == null) {
//						System.out.println("data.refRecord1 for " + data.beaconId + " @ " + time);
						mCallback.reportRecord(closestModel);
					}else {
						MobileEstimatedModel model = estimationController.getErrorRate(data, closestModel, time);
//						System.out.println("MobileEstimatedModel getErrorRate = " + model.errorRate + " for " + data.beaconId + " @ " + time);
						if(model.errorRate > Util.acceptance) {
//							System.out.println("err rate = " + estimationController.getErrorRate(data, closestModel, time).errorRate + " for " + data.beaconId + " @ " + time);
							mCallback.reportRecord(closestModel);
						}
					}

				}else {
//					System.out.println("closestModel = null for " + data.beaconId + " @ " + time);
				}
				Util.done.remove(data.beaconId);
			}
		else {
//			System.out.println("datas empty");
		}
		mCallback.onMobileCompleted();
	}
}
