package de.oklemenz;

import java.io.File;

public class BackupAgentReadSyncThread extends BackupAgentBaseThread {
	 
	private boolean source;
	private boolean completed;
	
	private BackupAgentReadSyncThread otherRead;
	public void setOtherRead(BackupAgentReadSyncThread otherRead) {
		if (this.otherRead == null) {
			this.otherRead = otherRead;
		}	
	}
	
	public BackupAgentReadSyncThread(BackupAgentTab backupAgentTab, File folder, boolean source) {
		super(backupAgentTab);
		setStartFolder(folder);
		this.source = source;
	}
	
	public void run() {
		completed = false;
		if (source) {
			backupAgentTab.sourceFiles.clear();
			backupAgentTab.sourceFilesSize.clear();
			backupAgentTab.sourceFilesModified.clear();
		} else {
			backupAgentTab.targetFiles.clear();
			backupAgentTab.targetFilesSize.clear();
			backupAgentTab.targetFilesModified.clear();
		}
		backupAgentTab.progressBar.setMaximum(0);
		backupAgentTab.progressBar.setMaximum(lastFileCount);
		backupAgentTab.progressBar.setValue(0);
		backupAgentTab.progressLabel.setText("0 %: 0 / ca. " + backupAgentTab.progressBar.getMaximum() + " folder(s) and files(s) processed.");
		read(startFolder);
		if (!stopped) {
			lastFileCount = filesCount;
		}
		completed = true;
		backupAgentTab.updateStatus(false);
	}
	
	private void read(File file) {
		if (stopped) {
			return;
		}
		if (file == null) {
			return;
		}
		if (file.getName().startsWith(".DS_Store") || file.getName().startsWith(".Spotlight") || file.getName().startsWith(".Trashes")) {
			return;
		}
		if (source && (file.getName().startsWith(".") && !backupAgentTab.copyHidden.isSelected())) { 
			return;
		}
		String relativeFilePath = getRelativePath(file);
		if (source) {
			if (sourceFoundFiles.contains(relativeFilePath)) {
				return;
			} else {
				sourceFoundFiles.add(relativeFilePath);
			}
		} else {
			if (targetFoundFiles.contains(relativeFilePath)) {
				return;
			} else {
				targetFoundFiles.add(relativeFilePath);
			}
		}		
		filesCount++;
		if (filesCount < otherRead.filesCount || (filesCount > otherRead.filesCount &&  otherRead.completed)) {
			backupAgentTab.progressBar.setValue(filesCount);
			int percent = filesProcessedPercentage();
			backupAgentTab.progressLabel.setText(percent + "% : " + filesCount + " / ca. " + backupAgentTab.progressBar.getMaximum() + " folder(s) and file(s) processed.");
		}
		if (source) {
			if (filter(file)) {
				System.out.println("Filtered: " + file.getAbsolutePath());
				return;
			}
		}
		if (!file.isDirectory()) {
			if (source) {
				backupAgentTab.sourceFiles.add(relativeFilePath);
				backupAgentTab.sourceFilesSize.put(relativeFilePath, file.length());
				backupAgentTab.sourceFilesModified.put(relativeFilePath, file.lastModified());
			} else {
				backupAgentTab.targetFiles.add(relativeFilePath);
				backupAgentTab.targetFilesSize.put(relativeFilePath, file.length());
				backupAgentTab.targetFilesModified.put(relativeFilePath, file.lastModified());
			}
		} else {		
			File[] childFiles = file.listFiles();
			if (childFiles != null) {
				for (File childFile : childFiles) {
					read(childFile);
					if (stopped) {
						return;
					}
				}
			}
		}
	}
}