package com.csl.org.rifidi.edge.adapter.cs203;

import org.rifidi.edge.sensors.sessions.MessageParsingStrategy;
import org.rifidi.edge.sensors.sessions.MessageParsingStrategyFactory;

public class CS203MessageParsingStrategyFactory implements MessageParsingStrategyFactory {

	
	
	@Override
	public MessageParsingStrategy createMessageParser() {
		// TODO Auto-generated method stub
		return new CS203MessageParsingStrategy() ;
	}

}
