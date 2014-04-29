package com.csl.org.rifidi.edge.adapter.cs203;

import org.rifidi.edge.sensors.sessions.MessageProcessingStrategy;
import org.rifidi.edge.sensors.sessions.MessageProcessingStrategyFactory;

public class CS203MessageProcessingStrategyFactory implements
		MessageProcessingStrategyFactory {

	private CS203ReaderSession session = null;

	public CS203MessageProcessingStrategyFactory(CS203ReaderSession session) {
		this.session = session;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.edge.sensors.sessions.MessageProcessingStrategyFactory
	 * #createMessageProcessor()
	 */
	@Override
	public MessageProcessingStrategy createMessageProcessor() {
		return new CS203MessageProcessingStrategy(session);
	}

}
