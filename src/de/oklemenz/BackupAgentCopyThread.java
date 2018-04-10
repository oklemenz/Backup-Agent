package de.oklemenz;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JOptionPane;

public class BackupAgentCopyThread extends BackupAgentBaseThread {
	 	
	protected int copiedFilesCount;
	protected int deletedFilesCount;
	
	public BackupAgentCopyThread(BackupAgentTab backupAgentTab) {
		super(backupAgentTab);
		sourceFolderPath = backupAgentTab.sourceFolder.getText();
		targetFolderPath = backupAgentTab.targetFolder.getText();
	}
	
	public void run() {
		if (backupAgentTab.filesToCopy.size() > 0) {
			int count = backupAgentTab.filesToCopy.size();
			System.out.println("Start copying " + count + " file(s) from source to target.");
			filesCount = 0;
			errorsCount = 0;
			copiedFilesCount = 0;
			deletedFilesCount = 0;
			backupAgentTab.progressBar.setMaximum(0);
			backupAgentTab.progressBar.setMaximum(count);
			backupAgentTab.progressBar.setValue(0);
			backupAgentTab.progressLabel.setText("0 %: 0 / " + backupAgentTab.progressBar.getMaximum() + " file(s) processed.");
			copy();
			if (!stopped) {
				System.out.println("Copying files finished. " + copiedFilesCount + " file(s) copied!" + (errorsCount > 0 ? " " + errorsCount + " file(s) not copied!" : ""));
				JOptionPane.showMessageDialog(backupAgentTab, copiedFilesCount + " file(s) copied!" + (errorsCount > 0 ? " " + errorsCount + " file(s) not copied!" : ""), "Copying done. " + errorsCount + " file(s) with errors.", JOptionPane.INFORMATION_MESSAGE);
			}
		}
		backupAgentTab.updateStatus(false);
	}
	
	protected void copy() {
		int sourcePathLength = sourceFolderPath.length();
		for (File sourceFile : backupAgentTab.filesToCopy) {
			if (stopped) {
				return;
			}
			String sourcePathName = sourceFile.getAbsolutePath();
			if (sourcePathName.startsWith(sourceFolderPath)){
				String relativePathName = sourcePathName.substring(sourcePathLength);
				String absoluteTargetPathName = targetFolderPath + relativePathName;
				File targetFile = new File(absoluteTargetPathName);
				targetFile.getParentFile().mkdirs();
				System.out.println("Copying " + sourcePathName + "...");
				if (!_copy(sourceFile, targetFile)) {
					return;
				}
			} else {
				throw new RuntimeException("Source file path " + sourcePathName + " does not start with source folder path!");
			}
			filesCount++;
			backupAgentTab.progressBar.setValue(filesCount);
			int percent = filesProcessedPercentage();
			backupAgentTab.progressLabel.setText(percent + "% : " + filesCount + " / " + backupAgentTab.progressBar.getMaximum() + " file(s) processed.");
		}
	}
	
	private boolean _copy(File sourceFile, File targetFile) {
		try {
			FileReader in = new FileReader(sourceFile);
			FileWriter out = new FileWriter(targetFile);
			int c;
			while ((c = in.read()) != -1) {
				if (stopped) {
					return false;
				}
				out.write(c);
			}
			in.close();
			out.close();
			targetFile.setLastModified(sourceFile.lastModified());
			copiedFilesCount++;
			System.out.println(sourceFile.getAbsolutePath() + " copied.");
		} catch (IOException e) {
			errorsCount++;
			System.out.println("Error: " + sourceFile.getAbsolutePath()  + " not copied.");
		}
		return true;
	}
}