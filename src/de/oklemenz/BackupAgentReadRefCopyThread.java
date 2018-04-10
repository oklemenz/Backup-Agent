package de.oklemenz;

import java.io.File;
import java.util.Date;

public class BackupAgentReadRefCopyThread extends BackupAgentBaseThread {
	 
	private Date refDate;
	private long refTimestamp;
	
	public BackupAgentReadRefCopyThread(BackupAgentTab backupAgentTab, File folder, Date refDate) {
		super(backupAgentTab);
		setStartFolder(folder);
		this.refDate = refDate;
		this.refTimestamp = this.refDate.getTime();
	}
	
	public void run() {
		backupAgentTab.filesToCopy.clear();
		backupAgentTab.progressBar.setMaximum(0);
		backupAgentTab.progressBar.setMaximum(lastFileCount);
		backupAgentTab.progressBar.setValue(0);
		backupAgentTab.progressLabel.setText("0 %: 0 / ca. " + backupAgentTab.progressBar.getMaximum() + " folder(s) and files(s) processed.");
		read(startFolder);
		if (!stopped) {
			lastFileCount = filesCount;
		}
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
		if (file.getName().startsWith(".") && !backupAgentTab.copyHidden.isSelected()) {
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
		if (!file.isDirectory()) {
			if (file.lastModified() >= refTimestamp) {
				System.out.println("To be copied: " + file.getAbsolutePath());
				backupAgentTab.filesToCopy.add(file);
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