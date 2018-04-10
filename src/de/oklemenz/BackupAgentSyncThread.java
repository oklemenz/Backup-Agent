package de.oklemenz;

import java.io.File;

import javax.swing.JOptionPane;

public class BackupAgentSyncThread extends BackupAgentCopyThread {
	 
	public BackupAgentSyncThread(BackupAgentTab backupAgentTab) {
		super(backupAgentTab);
	}
	
	public void run() {
		if (backupAgentTab.filesToCopy.size() > 0 || backupAgentTab.filesToDelete.size() > 0) {
			int count = backupAgentTab.filesToCopy.size() + backupAgentTab.filesToDelete.size();
			System.out.println("Start syncing " +  count + " file(s) from source to target.");
			filesCount = 0;
			errorsCount = 0;
			copiedFilesCount = 0;
			deletedFilesCount = 0;
			backupAgentTab.progressBar.setMaximum(0);
			backupAgentTab.progressBar.setMaximum(count);
			backupAgentTab.progressBar.setValue(0);
			backupAgentTab.progressLabel.setText("0 %: 0 / " + backupAgentTab.progressBar.getMaximum() + " file(s) processed.");
			sync();
			if (!stopped) {
				System.out.println("Syncing files finished. " + copiedFilesCount + " file(s) copied! " + deletedFilesCount + " file(s) deleted!" + (errorsCount > 0 ? " " + errorsCount + " file(s) not synced!" : ""));
				JOptionPane.showMessageDialog(backupAgentTab, copiedFilesCount + " file(s) copied! " + deletedFilesCount + " file(s) deleted!" + (errorsCount > 0 ? " " + errorsCount + " file(s) not synced!" : ""), "Syncing done. " + errorsCount + " file(s) with errors.", JOptionPane.INFORMATION_MESSAGE);
			}
		}
		backupAgentTab.updateStatus(false);
	}
	
	protected void sync() {
		copy();
		for (File targetFile : backupAgentTab.filesToDelete) {
			if (stopped) {
				return;
			}
			File parentFile = targetFile.getParentFile();
			if (targetFile.delete()) {
				System.out.println(targetFile.getAbsolutePath() + " deleted.");
				deletedFilesCount++;
				checkFolderDeletionRecursive(parentFile);
			} else {
				errorsCount++;
				System.out.println("Error: " + targetFile.getAbsolutePath() + " not deleted.");
			}
			filesCount++;
			backupAgentTab.progressBar.setValue(filesCount);
			int percent = filesProcessedPercentage();
			backupAgentTab.progressLabel.setText(percent + "% : " + filesCount + " / " + backupAgentTab.progressBar.getMaximum() + " file(s) processed.");
		}
	}
	
	protected void checkFolderDeletionRecursive(File file) {
		if (stopped) {
			return;
		}
		if (file != null) {
			if (file.isDirectory()) {
				File[] files = file.listFiles();
				if (files != null && 
				   (files.length == 0 ||
				   (files.length == 1 && files[0].isFile() && files[0].getName().equals(".DS_Store"))))
				{
					if (file.delete()) {
						System.out.println("Empty directory " + file.getAbsolutePath() + " deleted.");
					} else {
						System.out.println("Error: " + file.getAbsolutePath() + " not deleted.");
						return;
					}					
				} else {
					return;
				}
			}
			File parent = file.getParentFile();
			checkFolderDeletionRecursive(parent);
			if (stopped) {
				return;
			}
		}
	}
}