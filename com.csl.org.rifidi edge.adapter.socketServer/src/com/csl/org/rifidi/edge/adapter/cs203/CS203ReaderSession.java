package com.csl.org.rifidi.edge.adapter.cs203;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rifidi.edge.api.SessionStatus;
import org.rifidi.edge.configuration.AttributesChangedListener;
import org.rifidi.edge.configuration.Configuration;
import org.rifidi.edge.notification.NotifierService;
import org.rifidi.edge.notification.ReadCycle;
import org.rifidi.edge.notification.TagReadEvent;
import org.rifidi.edge.sensors.AbstractCommandConfiguration;
import org.rifidi.edge.sensors.AbstractSensor;
import org.rifidi.edge.sensors.sessions.AbstractServerSocketSensorSession;
import org.rifidi.edge.sensors.sessions.MessageParsingStrategy;
import org.rifidi.edge.sensors.sessions.MessageParsingStrategyFactory;
import org.rifidi.edge.sensors.sessions.MessageProcessingStrategy;
import org.rifidi.edge.sensors.sessions.MessageProcessingStrategyFactory;

import com.csl.org.rfid.tagserver.CslRfidTagServer;
import com.csl.org.rfid.tagserver.TagInfo;
import com.csl.org.rifidi.multiplereader.AsyncCallbackEventArgs;
import com.csl.org.rifidi.multiplereader.AsyncCallbackEventListener;
import com.csl.org.rifidi.multiplereader.Inventory;
import com.csl.org.rifidi.multiplereader.StartInventoryThread;
import com.csl.org.rifidi.multiplereader.TagCallbackInfo;

public class CS203ReaderSession extends AbstractServerSocketSensorSession
		implements AsyncCallbackEventListener {

	/** Logger for this class. */
	private static final Log logger = LogFactory
			.getLog(CS203ReaderSession.class);

	/**  */
	private String ipAdresse;

	/**  */
	private int idPort;

	/**  */
	private volatile NotifierService notifierService;

	/**
	 * 
	 */
	private CslRfidTagServer CSLConnector;

	private CS203Connector Connector;

	private AsyncCallbackEventListener cls;

	/** The ID of the reader this session belongs to */
	private final String readerID;

	/** Ok, because only accessed from synchronized block */
	int messageID = 1;
	int maxConAttempts = -1;
	int reconnectionInterval = -1;

	private CS203MessageParsingStrategyFactory parsingFactory;

	/**
	 * 
	 */
	private CS203ReaderSessionTagHandler tagHandler = null;

	/**
	 * 
	 * @param sensor
	 * @param ID
	 * @param serverSocketPort
	 * @param maxNumSensors
	 * @param commandConfigurations
	 * @param host
	 * @param ns
	 */
	public CS203ReaderSession(AbstractSensor<?> sensor, String ID,
			int serverSocketPort, int maxNumSensors,
			Set<AbstractCommandConfiguration<?>> commandConfigurations,
			String host, NotifierService ns) {
		super(sensor, ID, serverSocketPort, maxNumSensors,
				commandConfigurations);
		this.ipAdresse = host;
		this.idPort = serverSocketPort;
		this.readerID = sensor.getID();
		this.notifierService = ns;
		this.tagHandler = new CS203ReaderSessionTagHandler(ID);
		this.parsingFactory = new CS203MessageParsingStrategyFactory();
		this.Connector = new CS203Connector(this.getSensor().getName(),
				this.ipAdresse);
		this.cls = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.edge.sensors.SensorSession#connect()
	 */
	@Override
	public void connect() throws IOException {
		this.Connector.addAsyncCallbackEventListener(this.cls);
		new Thread(new Runnable() {
			@Override
			public void run() {
				Connector.Connect();
				Connector.StartInventory();
			}
		}).start();
		super._connect();
		super.submitQueuedCommands();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.edge.sensors.SensorSession#connect()
	 */
	@Override
	public void disconnect() {
		super.disconnect();
		this.Connector.removeAsyncCallbackEventListener(cls);
		Connector.stopInventory = true;
		Connector.Disconnect();
	}

	@Override
	protected synchronized void setStatus(SessionStatus status) {
		super.setStatus(status);
		notifierService.sessionStatusChanged(super.getSensor().getID(),
				getID(), status);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.edge.sensors.sessions.AbstractServerSocketSensorSession
	 * #getMessageParsingStrategyFactory()
	 */
	@Override
	protected MessageParsingStrategyFactory getMessageParsingStrategyFactory() {
		return this.parsingFactory;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rifidi.edge.sensors.sessions.AbstractServerSocketSensorSession
	 * #getMessageProcessingStrategyFactory()
	 */
	@Override
	protected MessageProcessingStrategyFactory getMessageProcessingStrategyFactory() {
		return new CS203MessageProcessingStrategyFactory(this);
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		// super.toString() + "-- " + this.ipAdresse + " : " + this.idPort;
		return " IPSession " + this.ipAdresse + ":" + this.idPort + " ( "
				+ getStatus() + " )";
	}

	public synchronized void sendData(TagCallbackInfo tagData) {
		try {

			String data;
			data = "";
			data += "ID:" + tagData.epc.ToString();
			// data += "|Antenna:" + 1;
			data += "|Timestamp:" + System.currentTimeMillis();
			data += "|PC:" + tagData.pc.ToString();
			data += "|RSSI:" + tagData.rssi + "\n";

			TagReadEvent event = this.tagHandler.parseTag(data);
			Set<TagReadEvent> tres = new HashSet<TagReadEvent>();
			tres.add(event);
			ReadCycle cycle = new ReadCycle(tres, readerID,
					System.currentTimeMillis());
			this.getSensor().send(cycle);

		} catch (Exception e) {
			// TODO: handle exceptione
			System.out.println(e.getMessage());
		}/**/
	}

	/**
	 * Parses and sends the tag to the desired destination.
	 * 
	 * @param tag
	 */
	public void sendTag(byte[] message) {

		TagReadEvent event = this.tagHandler.parseTag(new String(message));
		Set<TagReadEvent> tres = new HashSet<TagReadEvent>();
		tres.add(event);
		ReadCycle cycle = new ReadCycle(tres, readerID,
				System.currentTimeMillis());
		this.getSensor().send(cycle);
	}

	/**
	 * Parses and sends the tag to the desired destination.
	 * 
	 * @param tag
	 */
	public void sendTag(AsyncCallbackEventArgs message) {
		TagReadEvent event = this.tagHandler.parseTag(message);
		Set<TagReadEvent> tres = new HashSet<TagReadEvent>();
		tres.add(event);
		ReadCycle cycle = new ReadCycle(tres, readerID,
				System.currentTimeMillis());
		this.getSensor().send(cycle);
	}

	@Override
	public void AsyncCallbackEvent(AsyncCallbackEventArgs ev) {
		this.sendTag(ev);
	}

}
