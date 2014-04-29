package com.csl.org.rifidi.multiplereader;

public class StartInventoryThread implements Runnable {
	/** */
	private String ip;

	/** */
	private String mac;

	/** */
	private Inventory inventory;

	/**
	 * @author Sofiane.mekhaba
	 * @param ip
	 * @param mac
	 * @param inventory
	 */
	StartInventoryThread(String ip, String mac, Inventory inventory) {
		this.ip = ip;
		this.mac = mac;
		this.inventory = inventory;
	}
	
	/**
	 * @author Sofiane.mekhaba
	 * @param ip
	 * @param inventory
	 */
	public StartInventoryThread(String ip, Inventory inventory) {
		this.ip = ip;
		this.inventory = inventory;
	}

	/**
     * @author Sofiane.mekhaba
     */
	public void run() {
		inventory.StartInventory(ip);
	}
}