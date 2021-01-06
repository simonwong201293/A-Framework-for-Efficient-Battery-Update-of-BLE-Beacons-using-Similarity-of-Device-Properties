package com.hkust.sw.journal.server;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.hkust.sw.journal.Main;
import com.hkust.sw.journal.Util;
import com.hkust.sw.journal.server.controller.BatteryModelController;
import com.hkust.sw.journal.server.controller.SPRPController;
import com.hkust.sw.journal.server.controller.ServerEstimationController;
import com.hkust.sw.journal.server.model.BatteryModel;
import com.hkust.sw.journal.server.model.DatabaseRecordModel;
import com.hkust.sw.journal.server.model.ServerEstimatedRecord;
import com.hkust.sw.journal.server.model.SuggestedRefModel;

public class Server extends Thread{
	public interface ServerCallback {
		public void notifyRequiredInfo(List<SuggestedRefModel> models, long time);
		public void onServerCompleted();
	}

	private long time = 0L;
	public static final ConcurrentHashMap<String, Boolean> requested = new ConcurrentHashMap<>();
	public static final ConcurrentHashMap<String, Long> expectedReportTime = new ConcurrentHashMap<>();
	public static final ConcurrentHashMap<String, List<DatabaseRecordModel>> receivedRecords = new ConcurrentHashMap<>();
	public static final List<ServerEstimatedRecord> presentRecords = new ArrayList<>();
	private final BatteryModelController batteryModelController;
	private SPRPController sprpController;
	private ServerEstimationController estController;
	private final ServerCallback mCallback;

	public Server(ServerCallback callback, Set<String> beaconIds) {
		BatteryModel.initModels();
		this.mCallback = callback;
		for (String id : beaconIds) {
			requested.put(id, false);
		}
		batteryModelController = new BatteryModelController();
		sprpController = new SPRPController();
		estController = new ServerEstimationController();
	}
	
	public int getTotalReportRecord() {
		int total = 0;
		for(String key: receivedRecords.keySet()) {
			total += receivedRecords.get(key).size();
		}
		return total;
	}
	
	public HashMap<String, List<DatabaseRecordModel>> getDBRecords(){
		return new HashMap<>(receivedRecords);
	}
	
	public void run() {
		if(time > 71571) {
			exportEstimation();
			return;
		}
		time += Util.userAction;
		System.out.println("time = " + time);
		List<SuggestedRefModel> result = new ArrayList<>();
		for(String id: requested.keySet()) {
			if(time < Util.firstTime.get(id) || time > Util.lastTime.get(id)) {
				continue;
			}
			logTotalRecords();
			if(getTotalRecord(id) < 2) {
//				System.out.println("not enough records for " + id + " @ " + time + " where !receivedRecords.contains(id) = " + (!receivedRecords.contains(id)));
				continue;
			}
			SuggestedRefModel model = new SuggestedRefModel();
			model.beaconId = id;
			Object[] suggestion = sprpController.getSuitableReferences(time, id, receivedRecords);
			if (suggestion[0] != null) {
				model.refRecord1 = (DatabaseRecordModel) suggestion[0];
				model.refRecord2 = (DatabaseRecordModel) suggestion[1];
				model.lambda = (Double) suggestion[2];
				DatabaseRecordModel tmpModel = receivedRecords.get(id)
						.get(receivedRecords.get(id).size() - 1);
				model.time = tmpModel.time;
				model.voltage = tmpModel.voltage;
				if(model.refRecord1 != null) {
					double estimatedV = model.voltage - 
							model.lambda * 
							(model.refRecord1.voltage - model.refRecord2.voltage)/
							(model.refRecord1.time-model.refRecord2.time) *
							(model.time-time);
					ServerEstimatedRecord estRecord = new ServerEstimatedRecord();
					estRecord.beaconId = model.beaconId;
					estRecord.estimatedVoltage = estimatedV;
					estRecord.time = time;
					if(Double.isNaN(estimatedV) || estimatedV < 0) {
						SuggestedRefModel m = new SuggestedRefModel();
						m.beaconId = model.beaconId;
						requested.put(model.beaconId, true);
						Util.done.put(model.beaconId, false);
						result.add(m);
					}else
						presentRecords.add(estRecord);
				}
			}else {
//				System.out.println("cannot find frds");
			}
		}
		for (String id : requested.keySet()) {
			if(time < Util.firstTime.get(id) || time > Util.lastTime.get(id)) {
				expectedReportTime.remove(id);
				continue;
			}
			SuggestedRefModel model = checkBeacon(id);
			if(model != null) {
				result.add(model);
			}
			try {
				Thread.sleep(2);
			} catch (InterruptedException ignored) {
			}
		}
		if(time % 100 == 0) {
			logTotalRecords();
			logReportTime();
		}
		if (result.size() <= 0) {
			run();
		}else {
			mCallback.notifyRequiredInfo(result, time);
		}
	};
	
	private void exportEstimation() {
		if(!new File(Util.outputPath).getParentFile().exists()) {
			new File(Util.outputPath).getParentFile().mkdirs();
		}
		if(new File(Util.outputPath).exists()) {
			new File(Util.outputPath).delete();
		}
		try {
			FileWriter fw = new FileWriter(Util.outputPath);
			if(!Util.panda) {
				Set<Long> timeSet = new HashSet<>();
				HashMap<String, HashMap<Long, Double>> values = new HashMap<>();
				for (ServerEstimatedRecord record : presentRecords) {
					timeSet.add(record.time);
					if(!values.containsKey(record.beaconId)) values.put(record.beaconId, new HashMap<>());
					values.get(record.beaconId).put(record.time, record.estimatedVoltage);
	//				fw.write(record.beaconId + "," + record.estimatedVoltage + "," + record.time + "\n");
				}
				Main.serverVal = values;
				Long[] times = timeSet.toArray(new Long[timeSet.size()]);
				Arrays.sort(times);
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
				for (ServerEstimatedRecord record : presentRecords) {
					timeSet.add(record.time);
					if(!values.containsKey(record.beaconId)) values.put(record.beaconId, new HashMap<>());
					values.get(record.beaconId).put(record.time, record.estimatedVoltage);
	//				fw.write(record.beaconId + "," + record.estimatedVoltage + "," + record.time + "\n");
				}
				Main.serverVal = values;
				fw.write("BeaconId, Estimated Voltage, Time\n");
				for(ServerEstimatedRecord model : presentRecords) {
					fw.write(model.beaconId + "," + model.estimatedVoltage + "," + model.time + "\n");
				}
			}
			fw.close();
		}catch(Exception ignored) {}
	}
	
	private void logReportTime() {
		long time = Long.MAX_VALUE;
//		for(String id: expectedReportTime.keySet()) {
//			if(expectedReportTime.get(id) < time) {
//				time = expectedReportTime.get(id);
//				System.out.println("expectedReportTime = " + time + " for " + id);
//			}
//		}
	}

	private SuggestedRefModel checkBeacon(String beaconId) {
//		System.out.println("expectedReportTime size = " + expectedReportTime.size() + " for " + beaconId + "@ " + time);
		if(expectedReportTime.containsKey(beaconId) && expectedReportTime.get(beaconId) == 0) {
			SuggestedRefModel model = new SuggestedRefModel();
			model.beaconId = beaconId;
			requested.put(beaconId, true);
			Util.done.put(beaconId, false);
			return model;
		}else if (expectedReportTime.containsKey(beaconId) && receivedRecords.get(beaconId).size() >= 2) {
			if (expectedReportTime.get(beaconId) < time) {
				SuggestedRefModel model = new SuggestedRefModel();
				model.beaconId = beaconId;
				Object[] suggestion = sprpController.getSuitableReferences(time, beaconId, receivedRecords);
				if (suggestion[0] != null) {
//					System.out.println("Has suitable");
					model.refRecord1 = (DatabaseRecordModel) suggestion[0];
					model.refRecord2 = (DatabaseRecordModel) suggestion[1];
					model.lambda = (Double) suggestion[2];
					DatabaseRecordModel tmpModel = receivedRecords.get(beaconId)
							.get(receivedRecords.get(beaconId).size() - 1);
					model.time = tmpModel.time;
					model.voltage = tmpModel.voltage;
				}else {
//					System.out.println("Has no suitable");
				}
				requested.put(beaconId, true);
				Util.done.put(beaconId, false);
				return model;
			} else
				return null;
		} else {
			SuggestedRefModel model = new SuggestedRefModel();
			model.beaconId = beaconId;
			requested.put(beaconId, true);
			Util.done.put(beaconId, false);
			return model;
		}
	}

	public void receiveRecords(DatabaseRecordModel model) {
		if (!receivedRecords.containsKey(model.beaconId)) {
			receivedRecords.put(model.beaconId, new ArrayList<>());
		}
		ServerEstimatedRecord estRecord = new ServerEstimatedRecord();
		estRecord.beaconId = model.beaconId;
		estRecord.estimatedVoltage = model.voltage;
		estRecord.time = model.time;
		presentRecords.add(estRecord);
		if(!checkRecordExists(model))
			receivedRecords.get(model.beaconId).add(model);
		updateReportTime(model.beaconId, model.batteryType, model.voltage);
		requested.put(model.beaconId, false);
	}
	
	private boolean checkRecordExists(DatabaseRecordModel model) {
		for(DatabaseRecordModel m : receivedRecords.get(model.beaconId)) {
			if(m.beaconId.equals(model.beaconId) && m.voltage == model.voltage && m.time == model.time) {
				return true;
			}
		}
		return false;
	}
	
	private void logTotalRecords() {
		int total = 0;
//		for(String id : receivedRecords.keySet()) {
//			total += receivedRecords.get(id).size();
//			System.out.println("Received Records size = " + receivedRecords.get(id).size() + " for " + id + " at time " + time);
//		}
	}
	private int getTotalRecord(String id) {
		for(String rid : receivedRecords.keySet()) {
			if(rid.equals(id)) {
				return  receivedRecords.get(rid).size();
			}
		}
		return 0;
	}

	private void updateReportTime(String beaconId, String batteryModel, double voltage){
		if (receivedRecords.containsKey(beaconId) && receivedRecords.get(beaconId).size() >= 2) {
			int size = receivedRecords.get(beaconId).size();
			expectedReportTime.put(beaconId, estController.getNewReportTime(time, receivedRecords.get(beaconId).get(size - 1),
					receivedRecords.get(beaconId).get(size - 1)));
//			System.out.println("new report time 1 = " + estController.getNewReportTime(time, receivedRecords.get(beaconId).get(size - 1),
//					receivedRecords.get(beaconId).get(size - 1)));
		} else {
			expectedReportTime.put(beaconId,
					time + batteryModelController.suggestReportInterval(Util.userAction, batteryModel, voltage));
//			System.out.println("new report time 2 = " + (time + batteryModelController.suggestReportInterval(Util.userAction, batteryModel, voltage)));
		}
	}
}
