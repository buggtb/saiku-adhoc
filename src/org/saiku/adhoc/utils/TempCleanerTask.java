package org.saiku.adhoc.utils;

import java.util.TimerTask;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.ISolutionFile;
import org.pentaho.platform.api.repository.ISolutionRepository;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;

public class TempCleanerTask extends TimerTask {

	private static final String path = "system/saiku-adhoc/temp";

	public void run() {

		IPentahoSession userSession = PentahoSessionHolder.getSession();

		final ISolutionRepository solutionRepository = PentahoSystem.get(ISolutionRepository.class, userSession);

		final String fileExtensions = "cda";

		String name = solutionRepository.getRepositoryName();

		ISolutionFile[] files =  solutionRepository.getFileByPath(this.path).listFiles(
				/*
			new IFileFilter(){
			public boolean accept(ISolutionFile file) {
				boolean hasAccess = solutionRepository.hasAccess(file,ISolutionRepository.ACTION_DELETE);
				boolean visible = !file.isDirectory();
				return visible  && hasAccess ? !file.isDirectory() && fileExtensions.length() > 0 ? fileExtensions.indexOf(file.getExtension()) != -1 : true : false;

			}}		*/		
		);

		for (int i = 0; i < files.length; i++) {
			ISolutionFile iSolutionFile = files[i];
			solutionRepository.removeSolutionFile(iSolutionFile.getFullPath());

		}

	}
	
}