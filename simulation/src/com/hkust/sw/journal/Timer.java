package com.hkust.sw.journal;

public class Timer extends Thread {
	private int time = 0;
	private boolean setting = false;
	public void run() {
		while(true) {
			try {
				sleep(10);
			}catch(InterruptedException e) {
			}
			setTime(time + 1);
		}
	}
	
	public synchronized void execute(){
        while(setting){
            try {
                wait();
            } catch (InterruptedException e) {}
        }
    }
	
	public synchronized void setTime(int time) {
		setting = true;
		this.time = time;
		setting = false;
		notifyAll();
	}
}
