package org.saiku.adhoc.utils;

import java.util.Timer;

import org.pentaho.platform.api.engine.IPluginLifecycleListener;
import org.pentaho.platform.api.engine.PluginLifecycleException;

public class AdhocLifecycleListener implements IPluginLifecycleListener {

	@Override
	public void init() throws PluginLifecycleException {
	
		 Timer timer = new Timer();
		 timer.schedule(new TempCleanerTask(), 2000, 20000);
		
	}

	@Override
	public void loaded() throws PluginLifecycleException {



	}

	@Override
	public void unLoaded() throws PluginLifecycleException {
		// TODO Auto-generated method stub

	}

}
