package org.saiku.adhoc.model.dto;

import java.util.ArrayList;

public class FileTree {

	private ArrayList<Directory> directories;
	
	public FileTree() {
		this.directories = new  ArrayList<Directory>();
	}

	public void setDirectories(ArrayList<Directory> directories) {
		this.directories = directories;
	}

	public ArrayList<Directory> getDirectories() {
		return directories;
	}

}
