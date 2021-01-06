package com.hkust.sw.journal.mobile.controller;

import com.hkust.sw.journal.mobile.model.MobileDetectedModel;
import com.hkust.sw.journal.mobile.model.MobileEstimatedModel;
import com.hkust.sw.journal.server.model.SuggestedRefModel;

public class MobileEstimationController {
	public MobileEstimatedModel getErrorRate(SuggestedRefModel model, MobileDetectedModel model2, long time) {
		MobileEstimatedModel result = new MobileEstimatedModel();
		result.beaconId = model.beaconId;
		result.time = time;
		result.ref = model;
		// (v11-v12)/(t11-t12) * (t21-t22)/(v21-v22) = lamda
		// v11 - v12 = lamda * (v21-v22)/(t21-t22)*(t11-time)
		// v12 = v11 - lamda * (v21-v22)/(t21-t22)*(t11-time)
//		System.out.println("lambda = " + model.lambda);
		double estimatedV = model.voltage - 
				model.lambda * 
				(model.refRecord1.voltage - model.refRecord2.voltage)/
				(model.refRecord1.time-model.refRecord2.time) *
				(model.time-time);
//		System.out.println("getErrorRate estimatedV = " + estimatedV+", actualV =  " + model2.voltage +" @ " + time);
		result.estimatedVoltage = estimatedV;
		result.errorRate = Math.abs(model2.voltage - estimatedV)/model2.voltage * 100;
		return result;
	}
}
