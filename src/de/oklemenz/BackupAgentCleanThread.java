package de.oklemenz;

import java.io.File;

import javax.swing.JOptionPane;

public class BackupAgentCleanThread extends BackupAgentBaseThread {
	 
	protected int cleanedFilesCount;
	
	public BackupAgentCleanThread(BackupAgentTab backupAgentTab) {
		super(backupAgentTab);
	}
	
	public void run() {
		if (backupAgentTab.filesToClean.size() > 0) {
			int count = backupAgentTab.filesToClean.size();
			System.out.println("Start cleaning " + count + " folder(s) from target.");
			filesCount = 0;
			errorsCount = 0;
			cleanedFilesCount = 0;
			backupAgentTab.progressBar.setMaximum(0);
			backupAgentTab.progressBar.setMaximum(count);
			backupAgentTab.progressBar.setValue(0);
			backupAgentTab.progressLabel.setText("0 %: 0 / " + backupAgentTab.progressBar.getMaximum() + " folders(s) processed.");
			clean();
			if (!stopped) {
				System.out.println("Cleaning folders finished. " + cleanedFilesCount + " folder(s) cleaned!" + (errorsCount > 0 ? " " + errorsCount + " file(s) not cleaned!" : ""));
				JOptionPane.showMessageDialog(backupAgentTab, cleanedFilesCount + " folder(s) cleaned!" + (errorsCount > 0 ? " " + errorsCount + " folders(s) not cleaned!" : ""), "Clean done. " + errorsCount + " folders(s) with errors.", JOptionPane.INFORMATION_MESSAGE);
			}
		}
		backupAgentTab.updateStatus(false);
	}
	
	protected void clean() {
		for (File file : backupAgentTab.filesToClean) {
			if (stopped) {
				return;
			}
			boolean cleanOk = true;
			File[] childFiles = file.listFiles();
			if (childFiles != null) {
				if (childFiles.length == 1 && childFiles[0].getName().equals(BackupAgentTab.MAC_STORE_FILE)) {
					if (childFiles[0].delete()) {
						System.out.println("Hidden file " + childFiles[0].getAbsolutePath() + " deleted.");
					} else {
						errorsCount++;
						System.out.println("Error: " + file.getAbsolutePath() + " not cleaned. " + BackupAgentTab.MAC_STORE_FILE + " could not be deleted!");
						cleanOk = false;
					}
				} else if (childFiles.length > 1) {
					errorsCount++;
					System.out.println("Error: " + file.getAbsolutePath() + " not cleaned. Folder still contains files!");
					cleanOk = false;
				}
			}
			if (cleanOk) {
				if (file.delete()) {
					System.out.println("Empty folder " + file.getAbsolutePath() + " cleaned.");
					cleanedFilesCount++;
				} else {
					errorsCount++;
					System.out.println("Error: " + file.getAbsolutePath() + " not cleaned.");
				}
			}
			filesCount++;
			backupAgentTab.progressBar.setValue(filesCount);
			int percent = filesProcessedPercentage();
			backupAgentTab.progressLabel.setText(percent + "% : " + filesCount + " / " + backupAgentTab.progressBar.getMaximum() + " folder(s) processed.");
		}
	}
}