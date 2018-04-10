package de.oklemenz;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class BackupAgentTab extends JPanel {

	private static final long serialVersionUID = 6837339402043585743L;

	static SimpleDateFormat dateFormat 	   = new SimpleDateFormat("dd.MM.yyyy");
	static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyy HH:mm:ss");
	
	public static final String NEW_LINE = System.getProperty("line.separator");
	public static final String FILE_SEP = System.getProperty("file.separator");
	
	static String MAC_STORE_FILE = ".DS_Store";
	
	private static int ROWS = 5;
	
	private static int MODE_UNKOWN   = 0;
	private static int MODE_COPY     = 1;
	private static int MODE_REF_COPY = 2;
	private static int MODE_SYNC     = 3;
	private static int MODE_CLEAN    = 4;
	
	private static int STATUS_IGNORE = -1;
	private static int STATUS_INIT   = 0;
	
	private static int STATUS_COPY_READ    = 1;
	private static int STATUS_COPY_COPY    = 2;
	private static int STATUS_COPY_COMPARE = 3;
	private static int STATUS_COPY_DONE    = 4;

	private static int STATUS_REF_COPY_READ = 1;
	private static int STATUS_REF_COPY_COPY = 2;
	private static int STATUS_REF_COPY_DONE = 3;
	
	private static int STATUS_SYNC_READ    = 1;
	private static int STATUS_SYNC_COMPARE = 2;
	private static int STATUS_SYNC_SYNC    = 3;
	private static int STATUS_SYNC_DONE    = 4;

	private static int STATUS_CLEAN_READ  = 1;
	private static int STATUS_CLEAN_CLEAN = 2;
	private static int STATUS_CLEAN_DONE  = 3;
	
	private static String PROP_KEY_TITLE           = "Title";
	private static String PROP_KEY_INDEX           = "Index";
	private static String PROP_KEY_DESCRIPTION     = "Description";
	private static String PROP_KEY_COPY_ONLY       = "CopyOnly";
	private static String PROP_KEY_COPY_HIDDEN     = "CopyHidden";
	private static String PROP_KEY_SOURCE_FOLDER   = "SourceFolder";
	private static String PROP_KEY_TARGET_FOLDER   = "TargetFolder";
	private static String PROP_KEY_FILTER_FILES    = "FilterFiles";
	private static String PROP_KEY_LAST_FILE_COUNT = "LastFileCount";
	private static String PROP_KEY_LAST_MOD_DIFF   = "LastModifiedDiff";
	private static String PROP_KEY_COMP_FILE_SIZE  = "CompareFileSizes";
	private static String PROP_KEY_REF_COPY_DATE   = "RefCopyDate";
	
	public static String BACKUP_DATA_SUFFIX = "data";
	
	private BackupAgent backupAgent;
	
	String title;
	int index; 
	JTextField description;
	JTextField sourceFolder;
	JTextField targetFolder;
	JTextArea filterFiles;
	int lastFileCount;
	JTextField lastModifiedDiff;
	JCheckBox compareFileSizes;
	JTextField refCopyDate;
	
	JCheckBox copyOnly;
	JCheckBox copyHidden;
	JButton todayButton;
	JButton browseSourceButton;
	JButton browseTargetButton;
	JButton resetOptionsButton;
	JButton browseFilterButton;
	JButton clearFilterButton;
	
	JButton copyButton;
	JButton refCopyButton;
	JButton syncButton;
	JButton cleanButton;
	JButton stopButton;
	
	JProgressBar progressBar;
	JLabel progressLabel;
	
	String propertyFileName;
	
	int mode = MODE_UNKOWN;
	int status = STATUS_INIT;
	
	List<File> filesToCopy = new ArrayList<File>();
	List<File> filesToDelete = new ArrayList<File>();
	List<File> filesToClean = new ArrayList<File>();
	
	List<String> sourceFiles = new ArrayList<String>();
	Map<String, Long> sourceFilesSize = new HashMap<String, Long>();
	Map<String, Long> sourceFilesModified = new HashMap<String, Long>();
	List<String> targetFiles = new ArrayList<String>();
	Map<String, Long> targetFilesSize = new HashMap<String, Long>();
	Map<String, Long> targetFilesModified = new HashMap<String, Long>();
	
	int blockCount = 0;
	int copyReadCount = 0;
	int syncReadCount = 0;
	boolean stopped;
	Set<BackupAgentBaseThread> processes = new HashSet<BackupAgentBaseThread>();
			
	public BackupAgentTab(BackupAgent backupAgent, String title) {
		this.backupAgent = backupAgent;
		this.title = title;
		
		propertyFileName = backupAgent.getDataPath() + FILE_SEP + title + "." + BACKUP_DATA_SUFFIX;
	
		setLayout(new BorderLayout());
		
		// Labels
		JPanel labels = new JPanel();
		labels.setLayout(new GridLayout(ROWS, 1));
		labels.add(new JLabel("Title:"));
		labels.add(new JLabel("Source:"));
		labels.add(new JLabel("Target:"));
		labels.add(new JLabel("Options: "));
		labels.add(new JLabel("Filter:"));
		add(labels, BorderLayout.WEST);

		// Input Fields
		JPanel inputFields = new JPanel();
		inputFields.setLayout(new GridLayout(ROWS, 1));
		description = new JTextField();
		inputFields.add(description);
		sourceFolder = new JTextField();
		sourceFolder.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent component) {
				while (sourceFolder.getText().endsWith(FILE_SEP)) {
					sourceFolder.setText(sourceFolder.getText().substring(0, sourceFolder.getText().length()-1));
				}
				File file = new File(sourceFolder.getText());
				if (file.exists() && file.isDirectory()) {
					return true;
				} else {
					sourceFolder.setText("");
					return false;
				}
			}
		});
		inputFields.add(sourceFolder);
		targetFolder = new JTextField();
		targetFolder.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent component) {
				while (targetFolder.getText().endsWith(FILE_SEP)) {
					targetFolder.setText(targetFolder.getText().substring(0, targetFolder.getText().length()-1));
				}				
				File file = new File(targetFolder.getText());
				if (file.exists() && file.isDirectory()) {
					return true;
				} else {
					targetFolder.setText("");
					return false;
				}
			}
		});
		inputFields.add(targetFolder);
		JPanel options = new JPanel(new GridLayout(1, 6));
		options.add(new JLabel("Last Modify Diff. (ms):"));
		lastModifiedDiff = new JTextField();
		lastModifiedDiff.setText("0");
		lastModifiedDiff.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent component) {
				try {
					Integer.parseInt(lastModifiedDiff.getText());					
				} catch (NumberFormatException e) {
					lastModifiedDiff.setText("0");
					return false;
				}
				return true;
			}
		});
		options.add(lastModifiedDiff);
		compareFileSizes = new JCheckBox("Compare File Sizes");
		compareFileSizes.setSelected(true);
		options.add(compareFileSizes);
		options.add(new JLabel("Ref. Copy Date:"));
		refCopyDate = new JTextField();
		refCopyDate.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent component) {
				// ...
				return true;
			}
		});
		options.add(refCopyDate);
		todayButton = new JButton("Today");
		todayButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int result = JOptionPane.showConfirmDialog(BackupAgentTab.this, "Do you really want to set reference date to today?", "Set ref. date to today.", JOptionPane.YES_NO_OPTION);
				if (result == JOptionPane.YES_OPTION) {
					refCopyDate.setText(dateFormat.format(new Date()));
					return;
				}				
			}
		});
		options.add(todayButton);
		inputFields.add(options);
		filterFiles = new JTextArea();
		inputFields.add(new JScrollPane(filterFiles));
		add(inputFields, BorderLayout.CENTER);
		
		// Buttons
		JPanel buttons = new JPanel();
		buttons.setLayout(new GridLayout(ROWS, 1));
		JPanel checkboxes = new JPanel();
		checkboxes.setLayout(new GridLayout(2, 1));
		copyOnly = new JCheckBox("Copy only");
		checkboxes.add(copyOnly);
		copyHidden = new JCheckBox("Copy hidden");
		checkboxes.add(copyHidden);
		buttons.add(checkboxes);
		browseSourceButton = new JButton("Browse...");
		browseSourceButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fc = new JFileChooser(sourceFolder.getText());
				fc.setDialogTitle("Select source folder");
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = fc.showOpenDialog(BackupAgentTab.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
		            File file = fc.getSelectedFile();
		            sourceFolder.setText(file.getAbsolutePath());
				}
			}
		});
		buttons.add(browseSourceButton);
		browseTargetButton = new JButton("Browse...");
		browseTargetButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fc = new JFileChooser(targetFolder.getText());
				fc.setDialogTitle("Select target folder");
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = fc.showOpenDialog(BackupAgentTab.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
		            File file = fc.getSelectedFile();
		            targetFolder.setText(file.getAbsolutePath());
				}
			}
		});
		buttons.add(browseTargetButton);
		resetOptionsButton = new JButton("Reset");
		resetOptionsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int result = JOptionPane.showConfirmDialog(BackupAgentTab.this, "Do you really want to reset the options?", "Reset options.", JOptionPane.YES_NO_OPTION);
				if (result == JOptionPane.YES_OPTION) {
					lastModifiedDiff.setText("0");
					compareFileSizes.setSelected(true);
					refCopyDate.setText("");
				}
			}
		});
		buttons.add(resetOptionsButton);
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(2, 1));
		browseFilterButton = new JButton("Browse...");
		browseFilterButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fc = new JFileChooser(sourceFolder.getText());
				fc.setDialogTitle("Select target files");
				fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				int returnVal = fc.showOpenDialog(BackupAgentTab.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
		            File file = fc.getSelectedFile();
		            filterFiles.setText(filterFiles.getText().trim());
		            if (!filterFiles.getText().equals("")) {
		            	filterFiles.setText(filterFiles.getText() + NEW_LINE);
		            }
		            String filename = file.getAbsolutePath();
		            String sourceFolderPath = BackupAgentTab.this.sourceFolder.getText() + FILE_SEP;
		            int sourcePathLength = sourceFolderPath.length();
					if (filename.startsWith(sourceFolderPath)) {
						String relativePathName = filename.substring(sourcePathLength);
						filterFiles.setText(filterFiles.getText() + relativePathName);
					}			            
				}
			}
		});
		panel.add(browseFilterButton);
		clearFilterButton = new JButton("Clear...");
		clearFilterButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int result = JOptionPane.showConfirmDialog(BackupAgentTab.this, "Do you really want to clear filter?", "Clear Filter?", JOptionPane.YES_NO_OPTION);
				if (result == JOptionPane.YES_OPTION) {
					filterFiles.setText("");
				}
			}
		});
		panel.add(clearFilterButton);
		buttons.add(panel);
		add(buttons, BorderLayout.EAST);
		
		// Control
		JPanel controls = new JPanel();
		copyButton = new JButton("Copy");
		copyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				copy();
			}
		});
		controls.add(copyButton);
		refCopyButton = new JButton("Ref. Copy");
		refCopyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				refCopy();
			}
		});
		controls.add(refCopyButton);
		syncButton = new JButton("Sync");
		syncButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				sync();
			}
		});
		controls.add(syncButton);
		cleanButton = new JButton("Cleanup");
		cleanButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				clean();
			}
		});
		controls.add(cleanButton);
		progressBar = new JProgressBar();
		controls.add(progressBar);
		progressLabel = new JLabel();
		controls.add(progressLabel);
		stopButton = new JButton("Stop");
		stopButton.setEnabled(false);
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				stop();
			}
		});
		controls.add(stopButton);
		
		add(controls, BorderLayout.SOUTH);
		
		loadProperties();		
	}
	
	private void loadProperties() {
	    Properties prop = new Properties();

	    try {
	        prop.load(new FileInputStream(propertyFileName));
	    } catch (IOException e) {
	    }
	    
	    if (prop.containsKey(PROP_KEY_TITLE)) {
    		title = prop.getProperty(PROP_KEY_TITLE); 
    	}
	    
	    if (prop.containsKey(PROP_KEY_INDEX)) {
    		index = Integer.parseInt(prop.getProperty(PROP_KEY_INDEX)); 
    	}

	    if (prop.containsKey(PROP_KEY_DESCRIPTION)) {
    		description.setText(prop.getProperty(PROP_KEY_DESCRIPTION)); 
    	}
	    
	    if (prop.containsKey(PROP_KEY_COPY_ONLY)) {
    		copyOnly.setSelected(Boolean.parseBoolean(prop.getProperty(PROP_KEY_COPY_ONLY))); 
    	}
	    
	    if (prop.containsKey(PROP_KEY_COPY_HIDDEN)) {
    		copyHidden.setSelected(Boolean.parseBoolean(prop.getProperty(PROP_KEY_COPY_HIDDEN))); 
    	}
	        	
    	if (prop.containsKey(PROP_KEY_SOURCE_FOLDER)) {
    		sourceFolder.setText(prop.getProperty(PROP_KEY_SOURCE_FOLDER)); 
    	}
    	
    	if (prop.containsKey(PROP_KEY_TARGET_FOLDER)) {
    		targetFolder.setText(prop.getProperty(PROP_KEY_TARGET_FOLDER)); 
    	}
    	
    	if (prop.containsKey(PROP_KEY_FILTER_FILES)) {
    		String filter = "";
    		for (String part : prop.getProperty(PROP_KEY_FILTER_FILES).split(";")) {
    			if (!filter.isEmpty()) {
    				filter += NEW_LINE;
    			}
    			filter += part;
    		}
    		filterFiles.setText(filter);
    	}
    	
    	if (prop.containsKey(PROP_KEY_LAST_FILE_COUNT)) {
    		lastFileCount = Integer.parseInt(prop.getProperty(PROP_KEY_LAST_FILE_COUNT)); 
    	}
    	if (prop.containsKey(PROP_KEY_LAST_MOD_DIFF)) {
    		lastModifiedDiff.setText(prop.getProperty(PROP_KEY_LAST_MOD_DIFF)); 
    	}
    	if (prop.containsKey(PROP_KEY_COMP_FILE_SIZE)) {
    		compareFileSizes.setSelected(Boolean.parseBoolean(prop.getProperty(PROP_KEY_COMP_FILE_SIZE))); 
    	}
    	if (prop.containsKey(PROP_KEY_REF_COPY_DATE)) {
    		refCopyDate.setText(prop.getProperty(PROP_KEY_REF_COPY_DATE));
    	}
	}
	
	private void storeProperties() {
		Properties prop = new Properties();
		prop.setProperty(PROP_KEY_TITLE, title);
		prop.setProperty(PROP_KEY_INDEX, ""+getTabIndex());
		prop.setProperty(PROP_KEY_DESCRIPTION, description.getText());
		prop.setProperty(PROP_KEY_COPY_ONLY, ""+copyOnly.isSelected());
		prop.setProperty(PROP_KEY_COPY_HIDDEN, ""+copyHidden.isSelected());
		prop.setProperty(PROP_KEY_SOURCE_FOLDER, sourceFolder.getText());
		prop.setProperty(PROP_KEY_TARGET_FOLDER, targetFolder.getText());
		String filter = "";
		for (String part : filterFiles.getText().split(NEW_LINE)) {
			if (!filter.isEmpty()) {
				filter += ";";
			}
			filter += part;
		}
		prop.setProperty(PROP_KEY_FILTER_FILES, filter);
		prop.setProperty(PROP_KEY_LAST_FILE_COUNT, ""+lastFileCount);
		prop.setProperty(PROP_KEY_LAST_MOD_DIFF, ""+lastModifiedDiff.getText());
		prop.setProperty(PROP_KEY_COMP_FILE_SIZE, ""+compareFileSizes.isSelected());
		prop.setProperty(PROP_KEY_REF_COPY_DATE, ""+refCopyDate.getText());
		
	    try {
	        prop.store(new FileOutputStream(propertyFileName), null);
	    } catch (IOException e) {
	    	throw new RuntimeException("Error storing properties!");
	    } 
	}
	
	public String getTitle() {
		return title;
	}
	
	public int getTabIndex() {
		return backupAgent.tabbedPane.indexOfComponent(this);
	}
	
	public int getIndex() {
		return index;
	}
	
	public String getPopertyFileName() {
		return propertyFileName; 
	}
			
	private void copy() {
		File source = new File(sourceFolder.getText());
		File target = new File(targetFolder.getText());
		if (!source.exists() || !source.isDirectory()) {
			JOptionPane.showMessageDialog(this, "Source folder does not exist or is not a folder!", "Incorrect source folder!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (!target.exists() || !target.isDirectory()) {
			JOptionPane.showMessageDialog(this, "Target folder does not exist or is not a folder!", "Incorrect target folder!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		mode = MODE_COPY;
		copyReadCount = 0;
		System.out.println("Start finding files in source and target.");
		updateStatus(true, STATUS_COPY_READ);
	}
	
	private void refCopy() {
		File source = new File(sourceFolder.getText());
		File target = new File(targetFolder.getText());
		if (!source.exists() || !source.isDirectory()) {
			JOptionPane.showMessageDialog(this, "Source folder does not exist or is not a folder!", "Incorrect source folder!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (!target.exists() || !target.isDirectory()) {
			JOptionPane.showMessageDialog(this, "Target folder does not exist or is not a folder!", "Incorrect target folder!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			dateFormat.parse(refCopyDate.getText());
		} catch (ParseException e) {
			JOptionPane.showMessageDialog(this, "Reference date is not valid!", "Invalid ref. date!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		mode = MODE_REF_COPY;
		copyReadCount = 0;
		System.out.println("Start finding files in source and target.");
		updateStatus(true, STATUS_REF_COPY_READ);
	}
	
	private void sync() {
		if (copyOnly.isSelected()) {
			JOptionPane.showMessageDialog(this, "Backup agent allows copy only!", "Copy only allowed!", JOptionPane.ERROR_MESSAGE);
			return;
		} else {
			if (sourceFolder.getText().isEmpty() || targetFolder.getText().isEmpty()) {
				JOptionPane.showMessageDialog(this, "Please provide source and target folder!", "Information missing!", JOptionPane.ERROR_MESSAGE);
				return;
			}
			File source = new File(sourceFolder.getText());
			File target = new File(targetFolder.getText());
			if (!source.exists() || !source.isDirectory()) {
				JOptionPane.showMessageDialog(this, "Source folder does not exist or is not a folder!", "Incorrect source folder!", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (!target.exists() || !target.isDirectory()) {
				JOptionPane.showMessageDialog(this, "Target folder does not exist or is not a folder!", "Incorrect target folder!", JOptionPane.ERROR_MESSAGE);
				return;
			}
			mode = MODE_SYNC;
			syncReadCount = 0;
			System.out.println("Start finding files in source and target.");
			updateStatus(true, STATUS_SYNC_READ);
		}
	}
	
	private void clean() {
		if (targetFolder.getText().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please provide target folder!", "Information missing!", JOptionPane.ERROR_MESSAGE);
			return;
		}		
		File target = new File(targetFolder.getText());
		if (!target.exists() || !target.isDirectory()) {
			JOptionPane.showMessageDialog(this, "Target folder does not exist or is not a folder!", "Incorrect target folder!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		mode = MODE_CLEAN;
		System.out.println("Start finding folders in target.");
		updateStatus(true, STATUS_CLEAN_READ);
	}
		
	private void stop() {
		int result = JOptionPane.showConfirmDialog(this, "Do you really want to stop the process?", "Stop the process?", JOptionPane.YES_NO_OPTION);
		if (result == JOptionPane.YES_OPTION) {
			stopped = true;
			for (BackupAgentBaseThread process : processes) {
				process.stopProcess();
			}
			System.out.println("Process stopped!");
			JOptionPane.showMessageDialog(this, "Process stopped!", "Stopped!", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	void updateStatus(boolean block) {
		updateStatus(block, STATUS_IGNORE);
	}
	
	void updateStatus(boolean block, int newStatus) {
		blockCount += block ? 1 : -1;
		if (blockCount < 0) {
			blockCount = 0;
		}
		if (newStatus != STATUS_IGNORE) {
			status = newStatus;
		}
		if (blockCount > 0 || 
		   (mode == MODE_COPY  && status == STATUS_COPY_DONE) ||
		   (mode == MODE_REF_COPY  && status == STATUS_REF_COPY_DONE) ||
		   (mode == MODE_SYNC  && status == STATUS_SYNC_DONE) ||
		   (mode == MODE_CLEAN && status == STATUS_CLEAN_DONE) || stopped) {
			description.setEnabled(blockCount == 0);
			sourceFolder.setEnabled(blockCount == 0);
			targetFolder.setEnabled(blockCount == 0);
			lastModifiedDiff.setEnabled(blockCount == 0);
			refCopyDate.setEnabled(blockCount == 0);
			todayButton.setEnabled(blockCount == 0);
			compareFileSizes.setEnabled(blockCount == 0);
			filterFiles.setEnabled(blockCount == 0);
			browseSourceButton.setEnabled(blockCount == 0);
			browseTargetButton.setEnabled(blockCount == 0);
			resetOptionsButton.setEnabled(blockCount == 0);
			browseFilterButton.setEnabled(blockCount == 0);
			clearFilterButton.setEnabled(blockCount == 0);
			copyButton.setEnabled(blockCount == 0);
			refCopyButton.setEnabled(blockCount == 0);
			syncButton.setEnabled(blockCount == 0);
			cleanButton.setEnabled(blockCount == 0);
			stopButton.setEnabled(blockCount != 0);
		}
		backupAgent.updateStatus(block);
		// Status handling
		if (block) {
			if (mode == MODE_COPY) {
				if (status == STATUS_COPY_READ) {
					processes.clear();
					BackupAgentReadCopyThread process1 = new BackupAgentReadCopyThread(this, new File(sourceFolder.getText()), true);
					processes.add(process1);
					BackupAgentReadCopyThread process2 = new BackupAgentReadCopyThread(this, new File(targetFolder.getText()), false);
					processes.add(process2);
					process1.setOtherRead(process2);
					process2.setOtherRead(process1);
					process1.start();
					process2.start();
					System.out.println(dateTimeFormat.format(new Date()) + ": Read started...");
				} else if (status == STATUS_COPY_COMPARE) {
					processes.clear();
					BackupAgentCompareThread process = new BackupAgentCompareThread(this, true);
					processes.add(process);
					process.start();
					System.out.println(dateTimeFormat.format(new Date()) + ": Compare started...");
				} else if (status == STATUS_COPY_COPY) {
					processes.clear();
					BackupAgentCopyThread process = new BackupAgentCopyThread(this);
					processes.add(process);
					process.start();
					System.out.println(dateTimeFormat.format(new Date()) + ": Copy started...");
				}
			} else if (mode == MODE_REF_COPY) {
				if (status == STATUS_REF_COPY_READ) {
					processes.clear();
					Date refDate;
					try {
						refDate = dateFormat.parse(refCopyDate.getText());
					} catch (ParseException e) {
						throw new RuntimeException(e);
					}
					BackupAgentReadRefCopyThread process = new BackupAgentReadRefCopyThread(this, new File(sourceFolder.getText()), refDate);
					processes.add(process);
					process.start();
					System.out.println(dateTimeFormat.format(new Date()) + ": Read started...");
				} else if (status == STATUS_COPY_COPY) {
					processes.clear();
					BackupAgentCopyThread process = new BackupAgentCopyThread(this);
					processes.add(process);
					process.start();
					System.out.println(dateTimeFormat.format(new Date()) + ": Copy started...");
				}
			} else if (mode == MODE_SYNC) {
				if (status == STATUS_SYNC_READ) {
					processes.clear();
					BackupAgentReadSyncThread process1 = new BackupAgentReadSyncThread(this, new File(sourceFolder.getText()), true);
					processes.add(process1);
					BackupAgentReadSyncThread process2 = new BackupAgentReadSyncThread(this, new File(targetFolder.getText()), false);
					processes.add(process2);
					process1.setOtherRead(process2);
					process2.setOtherRead(process1);
					process1.start();
					process2.start();
					System.out.println(dateTimeFormat.format(new Date()) + ": Read started...");
				} else if (status == STATUS_SYNC_COMPARE) {
					processes.clear();
					BackupAgentCompareThread process = new BackupAgentCompareThread(this, false);
					processes.add(process);
					process.start();
					System.out.println(dateTimeFormat.format(new Date()) + ": Compare started...");
				} else if (status == STATUS_SYNC_SYNC) {
					processes.clear();
					BackupAgentSyncThread process = new BackupAgentSyncThread(this);
					processes.add(process);
					process.start();
					System.out.println(dateTimeFormat.format(new Date()) + ": Sync started...");
				}
			} else if (mode == MODE_CLEAN) {
				if (status == STATUS_CLEAN_READ) {
					processes.clear();
					BackupAgentReadCleanThread process = new BackupAgentReadCleanThread(this, new File(targetFolder.getText()));
					processes.add(process);
					process.start();
					System.out.println(dateTimeFormat.format(new Date()) + ": Read started...");
				} else if (status == STATUS_CLEAN_CLEAN) {
					processes.clear();
					BackupAgentCleanThread process = new BackupAgentCleanThread(this);
					processes.add(process);
					process.start();
					System.out.println(dateTimeFormat.format(new Date()) + ": Clean started...");
				}
			}
		} else {
			if (mode == MODE_COPY) {
				if (status == STATUS_COPY_READ) {
					if (!stopped) {
						copyReadCount++;
						if (copyReadCount == 2) {
							lastFileCount = getLastFileCount();
							System.out.println(dateTimeFormat.format(new Date()) + ": Read finished");
							System.out.println("Finding files finished. " + sourceFiles.size() + " file(s) found in source. " + targetFiles.size() + " file(s) found in target. Comparing started...");  
							updateStatus(true, STATUS_COPY_COMPARE);
						}
					} else {
						updateStatus(false, STATUS_COPY_DONE);
					}
				} else if (status == STATUS_COPY_COMPARE) {
					if (!stopped) {
						filesToDelete.clear();
						System.out.println(dateTimeFormat.format(new Date()) + ": Compare finished");
						System.out.println("Comparing files finished. " + filesToCopy.size() + " file(s) found to be copied!");
						if (filesToCopy.size() > 0) {
							int result = JOptionPane.showConfirmDialog(this, "Copy " + filesToCopy.size() + " file(s) from source to target.", filesToCopy.size() + " file(s) to be copied", JOptionPane.YES_NO_OPTION);
							if (result == JOptionPane.YES_OPTION) {
								updateStatus(true, STATUS_COPY_COPY);
								return;
							}
						} else {
							JOptionPane.showMessageDialog(this, "No files found!", "No files to be copied!", JOptionPane.INFORMATION_MESSAGE);
						}
					}
					updateStatus(false, STATUS_COPY_DONE);
				} else if (status == STATUS_COPY_COPY) {
					System.out.println(dateTimeFormat.format(new Date()) + ": Copy finished");
					updateStatus(false, STATUS_COPY_DONE);
				} else if (status == STATUS_COPY_DONE) {
					cleanup();
				}
			} else if (mode == MODE_REF_COPY) {
				if (status == STATUS_REF_COPY_READ) {
					if (!stopped) {
						lastFileCount = getLastFileCount();
						System.out.println(dateTimeFormat.format(new Date()) + ": Read finished");
						System.out.println("Finding files finished. " + sourceFiles.size() + " file(s) found in source. " + targetFiles.size() + " file(s) found in target. Comparing started...");
						if (filesToCopy.size() > 0) {
							int result = JOptionPane.showConfirmDialog(this, "Copy " + filesToCopy.size() + " file(s) from source to target.", filesToCopy.size() + " file(s) to be copied", JOptionPane.YES_NO_OPTION);
							if (result == JOptionPane.YES_OPTION) {
								updateStatus(true, STATUS_REF_COPY_COPY);
								return;
							}
						} else {
							JOptionPane.showMessageDialog(this, "No files found!", "No files to be copied!", JOptionPane.INFORMATION_MESSAGE);
						}
					} 
					updateStatus(false, STATUS_REF_COPY_DONE);
				} else if (status == STATUS_REF_COPY_COPY) {
					System.out.println(dateTimeFormat.format(new Date()) + ": Copy finished");
					if (!stopped) {
						int result = JOptionPane.showConfirmDialog(this, "Set reference date to today.", "Update ref. date", JOptionPane.YES_NO_OPTION);
						if (result == JOptionPane.YES_OPTION) {
							refCopyDate.setText(dateFormat.format(new Date()));					
						}
					}
					updateStatus(false, STATUS_REF_COPY_DONE);
				} else if (status == STATUS_REF_COPY_DONE) {
					cleanup();
				}
			} else if (mode == MODE_SYNC) {
				if (status == STATUS_SYNC_READ) {
					if (!stopped) {
						syncReadCount++;
						if (syncReadCount == 2) {
							lastFileCount = getLastFileCount();
							System.out.println(dateTimeFormat.format(new Date()) + ": Read finished");
							System.out.println("Finding files finished. " + sourceFiles.size() + " file(s) found in source. " + targetFiles.size() + " file(s) found in target. Comparing started..."); 
							updateStatus(true, STATUS_SYNC_COMPARE);
						}
					} else {
						updateStatus(false, STATUS_SYNC_DONE);
					}
				} else if (status == STATUS_SYNC_COMPARE) {
					if (!stopped) {
						System.out.println(dateTimeFormat.format(new Date()) + ": Compare finished");
						System.out.println("Comparing files finished. " + filesToCopy.size() + " file(s) found to be copied! " + filesToDelete.size() + " file(s) found to be deleted!");
						if (filesToCopy.size() > 0 || filesToDelete.size() > 0) {
							int result = JOptionPane.showConfirmDialog(this, "Copy " + filesToCopy.size() + " file(s) from source to target and delete " + filesToDelete.size() + " file(s) at target.", filesToCopy.size() + " file(s) to be copied, " + filesToDelete.size() + " file(s) to be deleted", JOptionPane.YES_NO_OPTION);
							if (result == JOptionPane.YES_OPTION) {
								updateStatus(true, STATUS_SYNC_SYNC);
								return;
							}
						} else {
							JOptionPane.showMessageDialog(this, "No files found!", "No files to be synced!", JOptionPane.INFORMATION_MESSAGE);
							refCopyDate.setText(dateFormat.format(new Date()));
						}
					}
					updateStatus(false, STATUS_SYNC_DONE);
				} else if (status == STATUS_SYNC_SYNC) {
					System.out.println(dateTimeFormat.format(new Date()) + ": Sync finished");
					if (!stopped) {
						refCopyDate.setText(dateFormat.format(new Date()));
					}
					updateStatus(false, STATUS_SYNC_DONE);
				} else if (status == STATUS_SYNC_DONE) {			
					cleanup();
				}
			} else if (mode == MODE_CLEAN) {
				if (status == STATUS_CLEAN_READ) {
					if (!stopped) {
						System.out.println(dateTimeFormat.format(new Date()) + ": Read finished");
						System.out.println("Finding files finished. " + filesToClean.size() + " folder(s) found!");
						if (filesToClean.size() > 0) {
							int result = JOptionPane.showConfirmDialog(this, "Clean " + filesToClean.size() + " folders(s) from target?", filesToClean.size() + " folder(s) to be cleaned", JOptionPane.YES_NO_OPTION);
							if (result == JOptionPane.YES_OPTION) {
								updateStatus(true, STATUS_CLEAN_CLEAN);
								return;
							}
						} else {
							JOptionPane.showMessageDialog(this, "No folders found!", "No folders to be cleaned!", JOptionPane.INFORMATION_MESSAGE);
						} 
					}
					updateStatus(false, STATUS_CLEAN_DONE);
				} else if (status == STATUS_CLEAN_CLEAN) {
					System.out.println(dateTimeFormat.format(new Date()) + ": Clean finished");
					updateStatus(false, STATUS_CLEAN_DONE);
				} else if (status == STATUS_CLEAN_DONE) {
					cleanup();
				}
			}
		}
	}
	
	private int getLastFileCount() {
		int lastFileCount = 0;
		for (BackupAgentBaseThread process : processes) {
			if (process.lastFileCount > lastFileCount) {
				lastFileCount = process.lastFileCount;
			}
		}
		return lastFileCount;
	}

	private void cleanup() {
		mode = MODE_UNKOWN;
		status = STATUS_INIT;
		blockCount = 0;
		copyReadCount = 0;
		syncReadCount = 0;
		filesToCopy.clear();
		filesToDelete.clear();
		filesToClean.clear();
		sourceFiles.clear();
		sourceFilesSize.clear();
		sourceFilesModified.clear();
		targetFiles.clear();
		targetFilesSize.clear();
		targetFilesModified.clear();
		processes.clear();
		stopped = false;
		progressBar.setValue(0);
		progressBar.setMaximum(0);
		progressLabel.setText("");
	}
	
	public void save() {
		storeProperties();
	}
}