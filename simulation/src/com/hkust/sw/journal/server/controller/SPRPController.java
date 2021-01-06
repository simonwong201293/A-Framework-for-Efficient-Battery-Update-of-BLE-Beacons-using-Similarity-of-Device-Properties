package com.hkust.sw.journal.server.controller;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.hkust.sw.journal.server.model.DatabaseRecordModel;

public class SPRPController {
	
	public Object[] getSuitableReferences(long time, String beaconId,  ConcurrentHashMap<String, List<DatabaseRecordModel>> receivedRecords) {
		Object[] result = new Object[3];
		result[0] = null;
		result[1] = null;
		result[2] = null;
		DatabaseRecordModel beaconRecord = receivedRecords.get(beaconId).get(receivedRecords.get(beaconId).size() - 1);
		DatabaseRecordModel beaconRecord2 = receivedRecords.get(beaconId).get(receivedRecords.get(beaconId).size() - 2);
		int cg = beaconRecord.configurationGroup;
		int lg = beaconRecord.locationGroup;
		int maxpoint = 0;
		long lastTime = 0;
		double slope = (beaconRecord2.voltage - beaconRecord.voltage)/(beaconRecord2.time - beaconRecord.time);
		for(String id : receivedRecords.keySet()) {
			if(id.equals(beaconId)) continue;
			List<DatabaseRecordModel> tmpList = receivedRecords.get(id);
			for(int i = tmpList.size() - 1; i >= 0; i--) {
				DatabaseRecordModel tmpRecord = tmpList.get(i);
				int index = tmpList.indexOf(tmpRecord);
				if(index < 2 || tmpRecord.time > time) continue;
				else if(tmpRecord.configurationGroup == cg && tmpRecord.locationGroup == lg) {
					if(maxpoint == 2 && tmpRecord.time > lastTime) {
						DatabaseRecordModel tmpRecord2 = tmpList.get(i - 1);
						double slope2 = (tmpRecord2.voltage - tmpRecord.voltage)/(tmpRecord2.time - tmpRecord.time);
						if(slope2 == 0 && slope != 0) continue;
						result[0] = tmpRecord2;
						result[1] = tmpRecord;
						result[2] = (slope2==0&&slope==0)?1:slope/slope2;
						lastTime = tmpRecord.time;
					}else if(maxpoint < 2) {
						DatabaseRecordModel tmpRecord2 = tmpList.get(i - 1);
						double slope2 = (tmpRecord2.voltage - tmpRecord.voltage)/(tmpRecord2.time - tmpRecord.time);
						if(slope2 == 0 && slope != 0) continue;
						result[0] = tmpRecord2;
						result[1] = tmpRecord;
						result[2] = (slope2==0&&slope==0)?1:slope/slope2;
						lastTime = tmpRecord.time;
					}
					maxpoint = 2;
				}else if((tmpRecord.configurationGroup == cg || tmpRecord.locationGroup == lg)) {
					if(maxpoint == 1 && tmpRecord.time > lastTime) {
						DatabaseRecordModel tmpRecord2 = tmpList.get(i - 1);
						double slope2 = (tmpRecord2.voltage - tmpRecord.voltage)/(tmpRecord2.time - tmpRecord.time);
						if(slope2 == 0 && slope != 0) continue;
						result[0] = tmpRecord2;
						result[1] = tmpRecord;
						result[2] = (slope2==0&&slope==0)?1:slope/slope2;
						lastTime = tmpRecord.time;
					}else if(maxpoint < 1) {
						DatabaseRecordModel tmpRecord2 = tmpList.get(i - 1);
						double slope2 = (tmpRecord2.voltage - tmpRecord.voltage)/(tmpRecord2.time - tmpRecord.time);
						if(slope2 == 0 && slope != 0) continue;
						result[0] = tmpRecord2;
						result[1] = tmpRecord;
						result[2] = (slope2==0&&slope==0)?1:slope/slope2;
						lastTime = tmpRecord.time;
					}
					maxpoint = 1;
				}
			}
		}
		return result;
	}
}
