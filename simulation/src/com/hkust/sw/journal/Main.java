package com.hkust.sw.journal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hkust.sw.journal.mobile.Mobile;
import com.hkust.sw.journal.mobile.Mobile.MobileCallback;
import com.hkust.sw.journal.mobile.model.MobileDetectedModel;
import com.hkust.sw.journal.server.Server;
import com.hkust.sw.journal.server.Server.ServerCallback;
import com.hkust.sw.journal.server.model.DatabaseRecordModel;
import com.hkust.sw.journal.server.model.SuggestedRefModel;

public class Main implements MobileCallback, ServerCallback {

	public final Server server;
	public final List<Mobile> mobiles;
	public static HashMap<String, HashMap<Long, Double>> serverVal = new HashMap<>();
	public static HashMap<String, HashMap<Long, Double>> rawVal = new HashMap<>();
	public boolean isServerCompleted = false, isMobileCompleted = false;
	private Mobile bestMobile;

	public static synchronized void main(String[] args) {
		new Main();
	}

	public Main() {
		mobiles = new ArrayList<>();
//		mobiles.add(new Mobile(this, true));
//		mobiles.add(new Mobile(this, true));
//		mobiles.add(new Mobile(this, true));
//		mobiles.add(new Mobile(this, true));
//		mobiles.add(new Mobile(this, true));
		bestMobile = new Mobile(this, false);
		mobiles.add(bestMobile);
		bestMobile.exportRaw();
		server = new Server(this, bestMobile.getBeaconIds());
		server.run();
		calculateFinalAverageError();
		calculateAverageErrorAlongTime();
		calculateAcc();
	}

	private void calculateAverageErrorAlongTime() {
		// Get all the estimated time
		Set<Long> timeSet = new HashSet<>();
		HashMap<Long, Double> errorList = new HashMap<>();
		for (String beaconId : serverVal.keySet()) {
			HashMap<Long, Double> tmp = serverVal.get(beaconId);
			for (Long time : tmp.keySet()) {
				timeSet.add(time);
			}
		}
		for (Long time : timeSet) {
			double sum = 0.0;
			double count = 0.0;
			for (String beaconId : serverVal.keySet()) {
				if (serverVal.get(beaconId).containsKey(time) 
						&& rawVal.containsKey(beaconId) 
						&& rawVal.get(beaconId).containsKey(time)) {
					sum += Math.abs(serverVal.get(beaconId).get(time) - rawVal.get(beaconId).get(time))/rawVal.get(beaconId).get(time) * 100.0;
					count += 1.0;
				}
			}
			if(count > 0.0) {
				double error = sum/count;
				errorList.put(time, error);
			}
			Util.exportAvgError(errorList);
		}
		
	}

	private void calculateFinalAverageError() {
		double acc = 0.0;
		int total_id = 0;
		for (String id : serverVal.keySet()) {
			System.out.println("server id " + id);
			if (rawVal.containsKey(id)) {
				HashMap<Long, Double> tmp = rawVal.get(id);
				double error_percentage_sum = 0.0;
				int total_num = 0;
				for (Long time : serverVal.get(id).keySet()) {
					if (tmp.containsKey(time)) {
						total_num++;
						error_percentage_sum += (Math.abs(tmp.get(time) - serverVal.get(id).get(time)) / tmp.get(time))
								* 100;
					}
				}
				double avg_er = error_percentage_sum / (total_num * 1.0);
				if (total_num > 0) {
					total_id++;
					acc += avg_er;
				}
				System.out.println("Avg error for " + id + " = " + avg_er + ", sum err = " + error_percentage_sum
						+ ", num = " + total_num);
			} else {
				System.out.println("raw id not exist " + id);
			}
		}
		System.out.println("Final avg error = " + acc / (total_id * 1.0));
		System.out.println("Used Record Number = " + server.getTotalReportRecord());
		int totalRawValSize = 0;
		for (String id : rawVal.keySet()) {
			totalRawValSize += rawVal.get(id).size() / Util.userAction;
		}
		System.out.println("Original Record Number = " + totalRawValSize);
		System.out.println("Data Usage Reduction Rate = "
				+ ((totalRawValSize - server.getTotalReportRecord()) * 1.0 / (totalRawValSize * 1.0)));
	}

	private void calculateAcc() {
		HashMap<Long, Integer[]> result = new HashMap<>();
		HashMap<String, List<DatabaseRecordModel>> dbRecord = server.getDBRecords();
		for (String id : dbRecord.keySet()) {
			for(DatabaseRecordModel model : dbRecord.get(id)) {
				if(!result.containsKey(model.time)) {
					result.put(model.time, new Integer[2]);
					result.get(model.time)[0] = 0;
					result.get(model.time)[1] = 0;
				}
				result.get(model.time)[0] = result.get(model.time)[0] + 1;
			}
		}
		for (String id : rawVal.keySet()) {
			for(Long time : rawVal.get(id).keySet()) {
				if(!result.containsKey(time)) {
					result.put(time, new Integer[2]);
					result.get(time)[0] = 0;
					result.get(time)[1] = 0;
				}
				result.get(time)[1] = result.get(time)[1] + 1;
			}
		}
		Util.exportAcc(result);
	}
	
	@Override
	public void notifyRequiredInfo(List<SuggestedRefModel> models, long time) {
		// System.out.println("Server Requested " + models.size() + " @ time = " +
		// time);
		for(Mobile model: mobiles)
			model.requestBeaconInfo(models, time);
	}

	@Override
	public void reportRecord(MobileDetectedModel reportedRecord) {
		// System.out.println("Mobile Reported " + reportedRecord.beaconId);
		server.receiveRecords(reportedRecord);
	}

	@Override
	public void onServerCompleted() {
		// System.out.println("onServerCompleted");
		server.run();
	}

	@Override
	public void onMobileCompleted() {
		// System.out.println("onMobileCompleted");
		if (Util.done.isEmpty()) {
			server.run();
		}
	}

}
