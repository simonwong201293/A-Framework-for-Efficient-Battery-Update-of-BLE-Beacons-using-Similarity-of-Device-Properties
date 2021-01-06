package com.hkust.sw.journal.server.model;

public class BatteryModel {
	public static BatteryModel _CR2477Model;
	public static BatteryModel _CR2430Model;
	public static BatteryModel _CR2032Model;
	
	public double v1, v2, v3;
	public long t1, t2;
	
	public static void initModels() {
		_CR2477Model = new BatteryModel();
		_CR2477Model.v1 = 3.6;
		_CR2477Model.v2 = 2.9;
		_CR2477Model.v3 = 2.6;
		_CR2477Model.t1 = 50 * 60;
		_CR2477Model.t2 = 4150 * 60;
		_CR2430Model = new BatteryModel();
		_CR2430Model.v1 = 3.0;
		_CR2430Model.v2 = 3.0;
		_CR2430Model.v3 = 2.8;
		_CR2430Model.t1 = 0;
		_CR2430Model.t2 = 1250 * 60;
		_CR2032Model = new BatteryModel();
		_CR2032Model.v1 = 2.9;
		_CR2032Model.v2 = 2.9;
		_CR2032Model.v3 = 2.8;
		_CR2032Model.t1 = 0;
		_CR2032Model.t2 = 600 * 60;
	}
}
