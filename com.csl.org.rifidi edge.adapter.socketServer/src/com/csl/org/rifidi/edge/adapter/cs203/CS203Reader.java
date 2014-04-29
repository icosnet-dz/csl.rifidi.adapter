package com.csl.org.rifidi.edge.adapter.cs203;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rifidi.edge.api.SessionDTO;
import org.rifidi.edge.configuration.AnnotationMBeanInfoStrategy;
import org.rifidi.edge.configuration.JMXMBean;
import org.rifidi.edge.configuration.Property;
import org.rifidi.edge.configuration.PropertyType;
import org.rifidi.edge.exceptions.CannotCreateSessionException;
import org.rifidi.edge.sensors.AbstractCommandConfiguration;
import org.rifidi.edge.sensors.AbstractSensor;
import org.rifidi.edge.sensors.CannotDestroySensorException;
import org.rifidi.edge.sensors.SensorSession;

@JMXMBean
public class CS203Reader extends AbstractSensor<CS203ReaderSession> {

	private static final Log logger = LogFactory.getLog(CS203Reader.class);

	/** The only session an alien reader allows. */
	private AtomicReference<CS203ReaderSession> session = new AtomicReference<CS203ReaderSession>();

	/** Flag to check if this reader is destroyed. */
	private AtomicBoolean destroyed = new AtomicBoolean(false);

	/** A hashmap containing all the properties for this reader */
	private final ConcurrentHashMap<String, String> readerProperties;

	/** IP address of the sensorSession. */
	private volatile String ipAddress = CS203eaderDefaultValues.IPADDRESS;

	/** Port to connect to. */
	private volatile Integer port = Integer
			.parseInt(CS203eaderDefaultValues.PORT);

	/** Time between two connection attempts. */
	private volatile Integer reconnectionInterval = Integer
			.parseInt(CS203eaderDefaultValues.RECONNECTION_INTERVAL);

	/** Number of connection attempts before a connection goes into fail state. */
	private volatile Integer maxNumConnectionAttempts = Integer
			.parseInt(CS203eaderDefaultValues.MAX_CONNECTION_ATTEMPTS);

	/** The port of the server socket */
	private volatile Integer notifyPort = 0;
	private volatile Integer ioStreamPort = 0;
	/** The ID of the session */
	private AtomicInteger sessionID = new AtomicInteger(0);

	private String displayName = "CS203";

	private final Set<AbstractCommandConfiguration<?>> commands;

	/** Mbeaninfo for this class. */
	public static final MBeanInfo mbeaninfo;
	static {
		AnnotationMBeanInfoStrategy strategy = new AnnotationMBeanInfoStrategy();
		mbeaninfo = strategy.getMBeanInfo(CS203Reader.class);
	}

	/**
	 * Constructor.
	 */
	public CS203Reader(Set<AbstractCommandConfiguration<?>> commands) {
		this.commands = commands;
		readerProperties = new ConcurrentHashMap<String, String>();
		logger.debug("New instance of CS203 created.");
	}

	@Override
	public String createSensorSession() throws CannotCreateSessionException {
		if (!destroyed.get() && session.get() == null) {
			Integer sessionID = this.sessionID.incrementAndGet();
			if (session.compareAndSet(null, new CS203ReaderSession(this,
					Integer.toString(sessionID), this.port,
					CS203eaderDefaultValues.MAX_NBR_SENSOR, commands,
					this.ipAddress, notifierService))) {
				// TODO: remove this once we get AspectJ in here!
				notifierService.addSessionEvent(this.getID(),
						Integer.toString(sessionID));
				return sessionID.toString();
			}
		}
		throw new CannotCreateSessionException();
	}

	@Override
	public String createSensorSession(SessionDTO sessionDTO)
			throws CannotCreateSessionException {
		if (!destroyed.get() && session.get() == null) {
			Integer sessionID = Integer.parseInt(sessionDTO.getID());
			if (session.compareAndSet(null, new CS203ReaderSession(this,
					Integer.toString(sessionID), this.port,
					CS203eaderDefaultValues.MAX_NBR_SENSOR, commands,
					this.ipAddress, notifierService))) {
				// TODO: remove this once we get AspectJ in here!
				notifierService.addSessionEvent(this.getID(),
						Integer.toString(sessionID));
				return sessionID.toString();
			}
		}
		throw new CannotCreateSessionException();
	}

	@Override
	public Map<String, SensorSession> getSensorSessions() {
		// TODO Auto-generated method stub
		Map<String, SensorSession> ret = new HashMap<String, SensorSession>();
		CS203ReaderSession cslSession = session.get();
		if (cslSession != null) {
			ret.put(cslSession.getID(), cslSession);
		}
		return ret;
	}

	@Override
	public void destroySensorSession(String id)
			throws CannotDestroySensorException {
		CS203ReaderSession cslSession = session.get();
		if (cslSession != null && cslSession.getID().equals(id)) {
			session.set(null);
			cslSession.killAllCommands();
			cslSession.disconnect();
			// TODO: remove this once we get AspectJ in here!
			notifierService.removeSessionEvent(this.getID(), id);
		} else {
			String error = "Tried to delete a non existend session: " + id;
			logger.warn(error);
			throw new CannotDestroySensorException(error);
		}
	}

	@Override
	protected void destroy() {
		// TODO Auto-generated method stub
		if (destroyed.compareAndSet(false, true)) {
			super.destroy();
			CS203ReaderSession cslSession = session.get();
			if (cslSession != null) {
				try {
					destroySensorSession(cslSession.getID());
				} catch (CannotDestroySensorException e) {
					logger.warn(e.getMessage());
				}
			}
		}
	}

	@Override
	public void applyPropertyChanges() {
		// TODO Auto-generated method stub

	}

	@Override
	public void unbindCommandConfiguration(
			AbstractCommandConfiguration<?> commandConfiguration,
			Map<?, ?> properties) {
		if (!destroyed.get()) {
			CS203ReaderSession cs203Session = session.get();
			if (cs203Session != null) {
				cs203Session.suspendCommand(commandConfiguration.getID());
			}
		}
	}

	@Override
	public MBeanInfo getMBeanInfo() {
		// TODO Auto-generated method stub
		return (MBeanInfo) mbeaninfo.clone();
	}

	@Override
	@Property(displayName = "Display Name", description = "Logical Name of Reader", writable = true, type = PropertyType.PT_STRING, category = "connection", defaultValue = CS203eaderDefaultValues.NAME, orderValue = 0)
	public String getDisplayName() {
		// TODO Auto-generated method stub
		return displayName;
	}

	/**
	 * @return the ipAddress
	 */
	@Property(displayName = "IP Address", description = "IP Address of  the Reader", writable = true, type = PropertyType.PT_STRING, category = "connection", defaultValue = CS203eaderDefaultValues.IPADDRESS, orderValue = 1)
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * @param ipAddress
	 *            the ipAddress to set
	 */
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	/**
	 * @return the port
	 */
	@Property(displayName = "Port", description = "Port of the" + " Reader", writable = true, type = PropertyType.PT_INTEGER, category = "connection", orderValue = 2, defaultValue = CS203eaderDefaultValues.PORT, minValue = "0", maxValue = "65535")
	public Integer getPort() {
		return port;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public void setPort(Integer port) {
		this.port = port;
	}

	/**
	 * @return the reconnectionInterval
	 */
	public Integer getReconnectionInterval() {
		return reconnectionInterval;
	}

	/**
	 * @param reconnectionInterval
	 *            the reconnectionInterval to set
	 */
	public void setReconnectionInterval(Integer reconnectionInterval) {
		this.reconnectionInterval = reconnectionInterval;
	}

	/**
	 * @return the maxNumConnectionAttempts
	 */
	public Integer getMaxNumConnectionAttempts() {
		return maxNumConnectionAttempts;
	}

	/**
	 * @param maxNumConnectionAttempts
	 *            the maxNumConnectionAttempts to set
	 */
	public void setMaxNumConnectionAttempts(Integer maxNumConnectionAttempts) {
		this.maxNumConnectionAttempts = maxNumConnectionAttempts;
	}

	/**
	 * @param displayName
	 *            the displayName to set
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

}
