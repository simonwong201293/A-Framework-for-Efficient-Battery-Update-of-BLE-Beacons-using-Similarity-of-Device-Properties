package com.hkust.sw.journal.server.controller;

import com.hkust.sw.journal.Util;
import com.hkust.sw.journal.server.model.BatteryModel;

public class BatteryModelController {
	public long suggestReportInterval(long userAction, String batteryModel, double voltage) {
		BatteryModel model = BatteryModel._CR2477Model;
		switch (batteryModel) {
		case "CR2032":
			model = BatteryModel._CR2032Model;
			break;
		case "CR2430":
			model = BatteryModel._CR2430Model;
			break;
		default:
		case "CR2477":
			model = BatteryModel._CR2477Model;
		}
		if (voltage > model.v2) {
			return Math.min(userAction, (long) (Util.acceptance * model.t1 * model.v1 / (model.v1 - model.v2)));
//			return userAction;
		} else if (voltage > model.v3) {
			if ((model.v2 - model.v3) / model.t2 <= 0.1) {
				return userAction;
			} else {
				return (long) (Util.acceptance * model.t2 *	model.v2 / (model.v2 - model.v3));
//				return userAction;
			}
		} else {
			return userAction;
		}
	}
}
