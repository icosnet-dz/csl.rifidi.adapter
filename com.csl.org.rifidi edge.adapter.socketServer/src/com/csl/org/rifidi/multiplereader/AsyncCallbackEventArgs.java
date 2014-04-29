package com.csl.org.rifidi.multiplereader;

import java.util.*;
import CSLibrary.Structures.*;
import CSLibrary.Constants.*;

/**
 * Inventory or tag search callback event argument
 */
public class AsyncCallbackEventArgs extends EventObject {
    /**
	 * 
	 */
	private static final long serialVersionUID = -6800805465562587701L;
	/**
     * Callback Tag Information
     */
    public TagCallbackInfo info = new TagCallbackInfo();
    /**
     * Constructor
     * @param source class that handle this event
     * @param record Tag Information
     */
    public AsyncCallbackEventArgs(Object source, TagCallbackInfo info)
    {
        super(source);
        this.info = info;
        
    }
}
