package de.oklemenz;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class BackupAgentBaseThread extends Thread {

	protected BackupAgentTab backupAgentTab;
	protected boolean stopped;
	
	protected String startFolderPath;
	protected String sourceFolderPath;
	protected String targetFolderPath; 
	
	protected int startPathLength;
	protected int sourcePathLength;
	protected int targetPathLength;
	
	protected int lastFileCount;
	protected int filesCount;
	protected int errorsCount;
	
	protected File startFolder;
	protected void setStartFolder(File startFolder) {
		this.startFolder = startFolder;
		startFolderPath = startFolder.getAbsolutePath();
		startPathLength = startFolderPath.length(); 
	}
	
	protected Set<String> filterFiles = new HashSet<String>();
	protected Set<String> sourceFoundFiles = new HashSet<String>();
	protected Set<String> targetFoundFiles = new HashSet<String>();
	
	public BackupAgentBaseThread(BackupAgentTab backupAgentTab) {
		this.setDaemon(true);
		this.backupAgentTab = backupAgentTab;
		sourceFolderPath = backupAgentTab.sourceFolder.getText();
		targetFolderPath = backupAgentTab.targetFolder.getText();
		sourcePathLength = sourceFolderPath.length();
		targetPathLength = targetFolderPath.length();
		for (String part : backupAgentTab.filterFiles.getText().split(BackupAgentTab.NEW_LINE)) {
			filterFiles.add(sourceFolderPath + BackupAgentTab.FILE_SEP + part);
		}
		lastFileCount = backupAgentTab.lastFileCount;
		sourceFoundFiles.clear();
		targetFoundFiles.clear();
	}
	
	public boolean filter(File file) {
		return filter(file.getAbsolutePath());
	}
	
	public boolean filter(String filename) {
		if (filterFiles.contains(filename)) {
			return true;
		}
		return false;
	}

	public String getRelativePath(File file) {
		return getRelativePath(file.getAbsolutePath());
	}
	
	public String getRelativePath(String filepath) {
		if (filepath.startsWith(startFolderPath)) {
			return filepath.substring(startPathLength);
		}
		throw new RuntimeException("File path '" + filepath + "' does not start with start folder path!");
	}
	
	public String getRelativeSourcePath(File file) {
		return getRelativeSourcePath(file.getAbsolutePath());
	}
	
	public String getRelativeSourcePath(String filepath) {
		if (filepath.startsWith(sourceFolderPath)) {
			return filepath.substring(sourcePathLength);
		}
		throw new RuntimeException("Source file path '" + filepath + "' does not start with source folder path!");
	}

	public String getRelativeTargetPath(File file) {
		return getRelativeTargetPath(file.getAbsolutePath());
	}
	
	public String getRelativeTargetPath(String filepath) {
		if (filepath.startsWith(targetFolderPath)) {
			return filepath.substring(targetPathLength);
		}
		throw new RuntimeException("Target file path '" + filepath + "' does not start with target folder path!");
	}

	public String getFileNameWithoutExtension(File file) {
		int index = file.getName().lastIndexOf('.');
        if (index > 0 && index <= file.getName().length() - 2) {
            return file.getName().substring(0, index).trim();
        }    
        return "";
    }
	
	public void stopProcess() {
		stopped = true;
	}	
	
	public int filesProcessedPercentage() {
		if (lastFileCount == 0) {
			return 0;
		}
		int percent = (int) (100.0 * filesCount / backupAgentTab.progressBar.getMaximum());
		if (percent > 100) {
			percent = 100;
		}
		return percent;
	}
}
