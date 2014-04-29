package com.csl.org.rifidi.edge.adapter.cs203;

import java.util.Map;

import javax.management.MBeanInfo;
import org.rifidi.edge.exceptions.InvalidStateException;
import org.rifidi.edge.notification.NotifierService;
import org.rifidi.edge.sensors.AbstractCommandConfiguration;
import org.rifidi.edge.sensors.AbstractSensor;
import org.rifidi.edge.sensors.AbstractSensorFactory;

public class CS203ReaderFactory extends AbstractSensorFactory<CS203Reader>{

	/** The Unique FACTORY_ID for this Factory */
	public static final String FACTORY_ID = "CSL" ;
	
	/** Description of the sensorSession. */
	private static final String description = "The Rifidi CSL of CS203 adapter.";
	
	private static final String displayname = CS203eaderDefaultValues.DISPLAY_NAME;
	
	/** A JMS event notification sender */
	private volatile NotifierService notifierService;
	
	@Override
	public void createInstance(String serviceID)
			throws IllegalArgumentException, InvalidStateException {
		if (serviceID == null) {
			throw new IllegalArgumentException("ServiceID is null");
		}
		if (notifierService == null) {
			throw new InvalidStateException("All services are not set");
		}
		CS203Reader instance = new CS203Reader(commands);
		instance.setID(serviceID);
		instance.setNotifiyService(notifierService);
		instance.register(getContext(), FACTORY_ID);
	}

	@Override
	public MBeanInfo getServiceDescription(String factoryID) {
		return (MBeanInfo) CS203Reader.mbeaninfo.clone();

	}

	@Override
	public String getFactoryID() {
		return FACTORY_ID;
	}

	@Override
	public String getDisplayName() {
		// TODO Auto-generated method stub
		return displayname;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return description;
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.rifidi.edge.core.sensors.base.AbstractSensorFactory#
	 * bindCommandConfiguration
	 * (org.rifidi.edge.sensors.commands.AbstractCommandConfiguration,
	 * java.util.Map)
	 */
	@Override
	public void bindCommandConfiguration(
			AbstractCommandConfiguration<?> commandConfiguration,
			Map<?, ?> properties) {
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.rifidi.edge.core.sensors.base.AbstractSensorFactory#
	 * unbindCommandConfiguration
	 * (org.rifidi.edge.sensors.commands.AbstractCommandConfiguration,
	 * java.util.Map)
	 */
	@Override
	public void unbindCommandConfiguration(
			AbstractCommandConfiguration<?> commandConfiguration,
			Map<?, ?> properties) {
	}
	
	/**
	 * Called by spring
	 * 
	 * @param wrapper
	 */
	public void setNotifierService(NotifierService notifierService) {
		this.notifierService = notifierService;
	}

}
