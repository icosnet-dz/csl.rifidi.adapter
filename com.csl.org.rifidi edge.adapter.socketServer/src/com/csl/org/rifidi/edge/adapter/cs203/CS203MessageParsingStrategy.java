package com.csl.org.rifidi.edge.adapter.cs203;

import java.util.LinkedList;
import java.util.List;

import org.rifidi.edge.sensors.sessions.MessageParsingStrategy;

public class CS203MessageParsingStrategy implements MessageParsingStrategy{


	List<Byte> messageList = new LinkedList<Byte>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.rifidi.edge.sensors.sessions.MessageParsingStrategy
	 * #isMessage(byte)
	 */
	@Override
	public byte[] isMessage(byte message) {
		if (message == '\n') {
			byte[] retVal = new byte[messageList.size()];
			int index = 0;
			for (Byte b : messageList) {
				retVal[index] = b;
				index++;
			}
			return retVal;
		}
		messageList.add(message);
		return null;
	}
}
