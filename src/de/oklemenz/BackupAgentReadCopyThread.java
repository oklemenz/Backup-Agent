package de.oklemenz;

import java.io.File;

public class BackupAgentReadCopyThread extends BackupAgentReadSyncThread {
	
	public BackupAgentReadCopyThread(BackupAgentTab backupAgentTab, File folder, boolean source) {
		super(backupAgentTab, folder, source);
	}
}
