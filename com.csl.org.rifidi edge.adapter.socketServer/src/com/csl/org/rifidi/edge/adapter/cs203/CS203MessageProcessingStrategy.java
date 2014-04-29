package com.csl.org.rifidi.edge.adapter.cs203;

import org.rifidi.edge.sensors.sessions.MessageProcessingStrategy;


public class CS203MessageProcessingStrategy implements  MessageProcessingStrategy{


	private CS203ReaderSession session = null;

	public CS203MessageProcessingStrategy(CS203ReaderSession session) {
		this.session = session;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.edge.sensors.sessions.MessageProcessingStrategy#
	 * processMessage(byte[])
	 */
	@Override
	public void processMessage(byte[] message) {
		System.out.println("i'm in reader");
		//this.session.sendTag(message);
	}
}
