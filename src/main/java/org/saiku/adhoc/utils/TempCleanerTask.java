package org.saiku.adhoc.utils;

import java.util.TimerTask;

import org.saiku.adhoc.service.repository.IRepositoryHelper;

public class TempCleanerTask extends TimerTask {
	
	private IRepositoryHelper repository;

	public void setRepositoryHelper(IRepositoryHelper repository) {
		this.repository = repository;
	}

	public void run() {
		System.out.println("schack");
		// iterate over all email addresses and archive them
	}
}