package com.csl.org.rifidi.edge.adapter.cs203;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rifidi.edge.notification.EPCGeneration2Event;
import org.rifidi.edge.notification.ReadCycle;
import org.rifidi.edge.notification.TagReadEvent;

import com.csl.org.rifidi.multiplereader.AsyncCallbackEventArgs;
import com.csl.org.rifidi.multiplereader.TagCallbackInfo;

public class CS203ReaderSessionTagHandler {
	/**
	 * All values we will be searching for upon message parsing: Tag ID,
	 * antenna, and timestamp. Any other key will be put into the extra
	 * information hashmap.
	 */
	public static final String ID_KEY = "ID";
	public static final String ANTENNA_KEY = "Antenna";
	public static final String TIMESTAMP_KEY = "Timestamp";

	private static final String PAIR_DELIM = "\\|";
	private static final String KEY_VAL_DELIM = ":";

	private String readerID = null;

	public CS203ReaderSessionTagHandler(String readerID) {
		this.readerID = readerID;
	}

	/** Logger for this class. */
	private static final Log logger = LogFactory
			.getLog(CS203ReaderSessionTagHandler.class);

	/**
	 * @author Sofiane.mekhaba
	 * @param taginfo
	 * @return
	 */
	public TagReadEvent parseTag(AsyncCallbackEventArgs taginfo) {
		try {
			TagCallbackInfo tagData = taginfo.info;
			String data;
			data = "";
			data += "ID:" + tagData.epc.ToString();
			// data += "|Antenna:" + 1;
			data += "|Timestamp:" + System.currentTimeMillis();
			data += "|PC:" + tagData.pc.ToString();
			data += "|RSSI:" + tagData.rssi + "\n";
			
			return this.parseTag(data);

		} catch (Exception e) {
			// TODO: handle exceptione
			System.out.println(e.getMessage());
		}
		return null;
	}

	/**
	 * @author Sofiane.mekhaba
	 * @param message
	 * @return
	 */
	public TagReadEvent parseTag(String message) {
		Map<String, String> extrainfo = new HashMap<String, String>();
		Integer antenna = null;
		Long timestamp = null;
		EPCGeneration2Event gen2event = new EPCGeneration2Event();
		String strmessage = new String(message);
		try {
			for (String key_val_pair : strmessage.split(PAIR_DELIM)) {
				String[] key_val = key_val_pair.split(KEY_VAL_DELIM);
				String key = key_val[0];
				String val = key_val[1];
				if (key.equalsIgnoreCase(ID_KEY)) {
					int numbits = val.length() * 4;
					BigInteger epc = null;
					try {
						epc = new BigInteger(Hex.decodeHex(val.toCharArray()));
					} catch (DecoderException e) {
						throw new RuntimeException("Cannot decode tag: " + val);
					}
					gen2event.setEPCMemory(epc, numbits);
				} else if (key.equalsIgnoreCase(ANTENNA_KEY)) {
					antenna = Integer.parseInt(val);
				} else if (key.equalsIgnoreCase(TIMESTAMP_KEY)) {
					timestamp = Long.parseLong(val);
				} else {
					extrainfo.put(key, val);
				}
			} /**/
			if (timestamp == null) {
				timestamp = System.currentTimeMillis();
			}
			if (antenna == null) {
				antenna = -1;
			}
			TagReadEvent retVal = new TagReadEvent(readerID, gen2event,
					antenna, timestamp);

			for (String extrakey : extrainfo.keySet()) {
				retVal.addExtraInformation(extrakey, extrainfo.get(extrakey));
			}
			return retVal;

		} catch (Exception e) {
			/*
			 * logger.error("There was an exception when processing an " +
			 * "incoming message for reader " + readerID + "\n " +
			 * e.getMessage());
			 */
			System.out.println(strmessage);
			// e.printStackTrace();
		}
		return null;
	}
}
