package de.oklemenz;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class BackupAgentReadCleanThread extends BackupAgentBaseThread {
	 
	private Set<String> filenamesToClean = new HashSet<String>();
	
	public BackupAgentReadCleanThread(BackupAgentTab backupAgentTab, File folder) {
		super(backupAgentTab);
		setStartFolder(folder);
	}
	
	public void run() {
		filenamesToClean.clear();
		backupAgentTab.filesToClean.clear();
		backupAgentTab.progressBar.setMaximum(0);
		backupAgentTab.progressBar.setMaximum(lastFileCount);
		backupAgentTab.progressBar.setValue(0);
		backupAgentTab.progressLabel.setText("0 %: 0 / ca. " + backupAgentTab.progressBar.getMaximum() + " folder(s) and files(s) processed.");
		read(startFolder);
		if (!stopped) {
			backupAgentTab.lastFileCount = filesCount;
		}
		filenamesToClean.clear();
		backupAgentTab.updateStatus(false);
	}
	
	private void read(File file) {
		if (stopped) {
			return;
		}
		if (file == null) {
			return;
		}
		String relativeFilePath = getRelativePath(file);
		if (sourceFoundFiles.contains(relativeFilePath)) {
			return;
		} else {
			sourceFoundFiles.add(relativeFilePath);
		}
		filesCount++;
		backupAgentTab.progressBar.setValue(filesCount);
		int percent = filesProcessedPercentage();
		backupAgentTab.progressLabel.setText(percent + "% : " + filesCount + " / ca. " + backupAgentTab.progressBar.getMaximum() + " folder(s) and file(s) processed.");
		if (filter(file)) {
			System.out.println("Filtered: " + file.getAbsolutePath());
			return;
		}
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (File childFile : files) {
					read(childFile);
					if (stopped) {
						return;
					}
				}
				if (files.length == 0 ||
				   (files.length == 1 && files[0].isFile() && files[0].getName().equals(".DS_Store"))) {
					System.out.println(file.getAbsolutePath());
					filenamesToClean.add(file.getAbsolutePath());
					backupAgentTab.filesToClean.add(file);
					return;
				}
				boolean allCleaned = true;
				for (File childFile : files) {
					if (childFile.isFile()) {
						if (childFile.getName().equals(".DS_Store")) {
							continue;
						}
						allCleaned = false;
						break;
					}
					if (!filenamesToClean.contains(childFile.getAbsolutePath())) {
						allCleaned = false;
						break;
					}
				}
				if (allCleaned) {
					System.out.println(file.getAbsolutePath());
					filenamesToClean.add(file.getAbsolutePath());
					backupAgentTab.filesToClean.add(file);				
				}
			}
		}		
	}
}