package de.oklemenz;

import java.io.File;

public class BackupAgentCompareThread extends BackupAgentBaseThread {
	
	int lastModifiedDelta = 0;
	boolean compareFileSizes = true;
	boolean onlyCopy = false;
	
	public BackupAgentCompareThread(BackupAgentTab backupAgentTab, boolean onlyCopy) {
		super(backupAgentTab);
		this.onlyCopy = onlyCopy; 
		lastModifiedDelta = Integer.parseInt(backupAgentTab.lastModifiedDiff.getText());
		compareFileSizes = backupAgentTab.compareFileSizes.isSelected();
	}
	
	public void run() {
		backupAgentTab.filesToCopy.clear();
		if (!onlyCopy) {
			backupAgentTab.filesToDelete.clear();
		}
		backupAgentTab.progressBar.setMaximum(0);
		backupAgentTab.progressBar.setMaximum(backupAgentTab.sourceFiles.size() + backupAgentTab.targetFiles.size());
		backupAgentTab.progressBar.setValue(0);
		backupAgentTab.progressLabel.setText("0 %: 0 / " + backupAgentTab.progressBar.getMaximum() + " files(s) processed.");
		compare();
		backupAgentTab.updateStatus(false);
	}
	
	protected void compare() {
		backupAgentTab.filesToCopy.clear();
		backupAgentTab.filesToDelete.clear();
		for (String relativeTargetFile : backupAgentTab.targetFiles) {
			if (stopped) {
				return;
			}
			filesCount++;
			backupAgentTab.progressBar.setValue(filesCount);
			int percent = filesProcessedPercentage();
			backupAgentTab.progressLabel.setText(percent + "% : " + filesCount + " / " + backupAgentTab.progressBar.getMaximum() + " file(s) processed.");
			String absoluteSourceFilePath = sourceFolderPath + relativeTargetFile;
			String absoluteTargetFilePath = targetFolderPath + relativeTargetFile;
			if (!backupAgentTab.sourceFilesSize.containsKey(relativeTargetFile)) {
				if (!onlyCopy) {
					System.out.println("To be deleted: " + absoluteTargetFilePath);
					backupAgentTab.filesToDelete.add(new File(absoluteTargetFilePath));
				}
			} else {
				long sourceFileSize = backupAgentTab.sourceFilesSize.get(relativeTargetFile);
				long targetFileSize = backupAgentTab.targetFilesSize.get(relativeTargetFile);
				long sourceFileModified = backupAgentTab.sourceFilesModified.get(relativeTargetFile);
				long targetFileModified = backupAgentTab.targetFilesModified.get(relativeTargetFile);
				if ((compareFileSizes && sourceFileSize != targetFileSize) || 
					 Math.abs(sourceFileModified - targetFileModified) > lastModifiedDelta) {
					System.out.println("To be copied: " + absoluteSourceFilePath);
					backupAgentTab.filesToCopy.add(new File(absoluteSourceFilePath));
				}
			}
		}
		for (String relativeSourceFile : backupAgentTab.sourceFiles) {
			if (stopped) {
				return;
			}
			filesCount++;
			backupAgentTab.progressBar.setValue(filesCount);
			int percent = filesProcessedPercentage();
			backupAgentTab.progressLabel.setText(percent + "% : " + filesCount + " / " + backupAgentTab.progressBar.getMaximum() + " file(s) processed.");
			String absoluteSourceFile = sourceFolderPath + relativeSourceFile;
			if (!backupAgentTab.targetFilesSize.containsKey(relativeSourceFile)) {
				System.out.println("To be copied: " + absoluteSourceFile);
				backupAgentTab.filesToCopy.add(new File(absoluteSourceFile));
			}
		}
	}
}