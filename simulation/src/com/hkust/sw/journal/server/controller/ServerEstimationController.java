package com.hkust.sw.journal.server.controller;

import com.hkust.sw.journal.Util;
import com.hkust.sw.journal.server.model.DatabaseRecordModel;

public class ServerEstimationController {
	public long getNewReportTime(long currentTime, DatabaseRecordModel data1, DatabaseRecordModel data2) {
		double slope = (data1.voltage - data2.voltage)/(data1.time - data2.time);
		if(slope == 0) return Util.userAction;
		long w = (long) (-1 * Util.acceptance * data2.time / slope);
		if(w <= 0) return currentTime + Util.userAction;
		if(currentTime - data2.time > 2 * w) return 0;
		int i = 1;
		while(data2.time + w * (2 - Math.pow(0.5, ++i)) < currentTime ) {
		}
		return (long) (data2.time + w  * (2 - Math.pow(0.5, i)));
	}
}
