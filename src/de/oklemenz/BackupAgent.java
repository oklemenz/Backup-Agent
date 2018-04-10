package de.oklemenz;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class BackupAgent extends JFrame implements WindowListener {

	private static final long serialVersionUID = -6972140396917564150L;

	public static String BACKUP_CONFIG_NAME   = "BackupAgent";
	public static String BACKUP_CONFIG_SUFFIX = "config";
	
	private static String PROP_KEY_WINDOW_WIDTH  = "WindowWidth";
	private static String PROP_KEY_WINDOW_HEIGHT = "WindowHeight";
	private static String PROP_KEY_WINDOW_X      = "WindowX";
	private static String PROP_KEY_WINDOW_Y      = "WindowY";
	private static String PROP_KEY_DATA_PATH     = "DataPath";
		
	public static void main(String[] args) {
		try {
			Runtime.getRuntime().exec("pmset noidle");
			new BackupAgent();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	JButton addButton;
	JButton removeButton;
	JButton leftButton;
	JButton rightButton;
	JTabbedPane tabbedPane;
	
	String propertyFileName;
	
	int blockCount = 0;
	
	int windowWidth = 1200;
	int windowHeight = 500;
	int windowX = 100;
	int windowY = 200;
	String defaultDataPath = "BackupAgent";
	JTextField dataPath;
	JButton browseDataPathButton;
	JButton loadDataButton;
	
	public static double JAVA_VERSION = getVersion ();

	static double getVersion () {
	    String version = System.getProperty("java.version");
	    String[] parts = version.split("\\.");
	    if (parts.length >= 2) {
		    return Double.parseDouble (parts[0] + "." + parts[1]);
	    }
	    return Double.MAX_VALUE;
	}
	
	public BackupAgent() {
		
		if (getVersion() > 1.6) {
			JOptionPane.showMessageDialog(this, "Backup Agent does only run with Java JRE 1.6!", "Java JRE error", JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
		}
		
		setTitle("Backup Agents");
		
		propertyFileName = BACKUP_CONFIG_NAME + "." + BACKUP_CONFIG_SUFFIX;
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
				
		JPanel headerPanel = new JPanel();
		JButton saveButton= new JButton("Save");
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				save();
			}
		});
		headerPanel.add(saveButton);
		JLabel dataPathLabel = new JLabel("Backups Agents: ");
		headerPanel.add(dataPathLabel);
		dataPath = new JTextField();
		dataPath.setText(defaultDataPath);
		headerPanel.add(dataPath);
		browseDataPathButton = new JButton("Browse...");
		browseDataPathButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fc = new JFileChooser(dataPath.getText());
				fc.setDialogTitle("Select source folder");
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = fc.showOpenDialog(BackupAgent.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
		            File file = fc.getSelectedFile();
		            dataPath.setText(file.getAbsolutePath());
				}
			}
		});
		headerPanel.add(browseDataPathButton);
		loadDataButton = new JButton("Refresh");
		loadDataButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				BackupAgent.this.reload();
			}
		});
		headerPanel.add(loadDataButton);
		
		leftButton = new JButton("<");
		leftButton.setToolTipText("Move selected backup agent one position left");
		leftButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				BackupAgentTab backupAgentTab = (BackupAgentTab)tabbedPane.getSelectedComponent();
				if (backupAgentTab != null) {
					moveTabLeft();
				}
			}
		});
		headerPanel.add(leftButton);
		addButton = new JButton("+");
		addButton.setToolTipText("Append new backup agent");
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				addEmptyTab();
			}
		});
		headerPanel.add(addButton);
		removeButton = new JButton("-");
		removeButton.setToolTipText("Remove selected backup agent");
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				removeCurrentTab();
			}
		});
		headerPanel.add(removeButton);
		rightButton = new JButton(">");
		rightButton.setToolTipText("Move selected backup agent one position right");
		rightButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				BackupAgentTab backupAgentTab = (BackupAgentTab)tabbedPane.getSelectedComponent();
				if (backupAgentTab != null) {
					moveTabRight();
				}
			}
		});
		headerPanel.add(rightButton);
		add(headerPanel, BorderLayout.NORTH);
		
		tabbedPane = new JTabbedPane();
		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				BackupAgent.this.updateHeaderButtons();
			}
		});
		
		loadProperties();
		reload();
		add(tabbedPane, BorderLayout.CENTER);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		addWindowListener(this);
		
		updateHeaderButtons();
		
		setBounds(windowX, windowY, windowWidth, windowHeight);
		setVisible(true);
		setResizable(true);
	}	
	
	private void loadProperties() {
	    Properties prop = new Properties();

	    try {
	        prop.load(new FileInputStream(propertyFileName));
	    } catch (IOException e) {
	    }
	    
	    if (prop.containsKey(PROP_KEY_WINDOW_WIDTH)) {
    		windowWidth = Integer.parseInt(prop.getProperty(PROP_KEY_WINDOW_WIDTH)); 
    	}
	    if (prop.containsKey(PROP_KEY_WINDOW_HEIGHT)) {
    		windowHeight = Integer.parseInt(prop.getProperty(PROP_KEY_WINDOW_HEIGHT)); 
    	}
	    if (prop.containsKey(PROP_KEY_WINDOW_X)) {
    		windowX = Integer.parseInt(prop.getProperty(PROP_KEY_WINDOW_X)); 
    	}
	    if (prop.containsKey(PROP_KEY_WINDOW_Y)) {
    		windowY = Integer.parseInt(prop.getProperty(PROP_KEY_WINDOW_Y)); 
    	}
	    if (prop.containsKey(PROP_KEY_DATA_PATH)) {
    		dataPath.setText(prop.getProperty(PROP_KEY_DATA_PATH)); 
    	}
	}
	
	private void storeProperties() {
		Properties prop = new Properties();
		Rectangle rect = this.getBounds();
		prop.setProperty(PROP_KEY_WINDOW_WIDTH, ""+rect.width);
		prop.setProperty(PROP_KEY_WINDOW_HEIGHT, ""+rect.height);
		prop.setProperty(PROP_KEY_WINDOW_X, ""+rect.x);
		prop.setProperty(PROP_KEY_WINDOW_Y, ""+rect.y);
		prop.setProperty(PROP_KEY_DATA_PATH, ""+dataPath.getText());
		
	    try {
	        prop.store(new FileOutputStream(propertyFileName), null);
	    } catch (IOException e) {
	    	throw new RuntimeException("Error storing properties!");
	    } 
	}
	
	private void reload() {
		tabbedPane.removeAll();
		File folder = new File(dataPath.getText());
		if (folder.exists()) {
			int count = 0;
			for (File file : folder.listFiles()) {
				if (file.getName().endsWith(BackupAgentTab.BACKUP_DATA_SUFFIX)) {
					count++;
				}
			}
			BackupAgentTab[] backups = new BackupAgentTab[count];
			for (File file : folder.listFiles()) {
				if (file.getName().endsWith(BackupAgentTab.BACKUP_DATA_SUFFIX)) {
		 			int length = file.getName().lastIndexOf(".");
		 			if (length > 0) {
		 				BackupAgentTab backupAgentTab = new BackupAgentTab(this, file.getName().substring(0, length));
		 				backups[backupAgentTab.getIndex()] = backupAgentTab;
					}
				}
			}
			for (BackupAgentTab backupAgentTab : backups) {
				if (backupAgentTab != null) {
					tabbedPane.addTab(backupAgentTab.getTitle(), backupAgentTab);
				}
			}
			if (tabbedPane.getTabCount() > 0) {
				tabbedPane.setSelectedIndex(0);
			}
		}
		updateHeaderButtons();
	}
	
	public String getDataPath() {
		return dataPath.getText();
	}
	
	public void addEmptyTab() {
		File folder = new File(dataPath.getText());
		if (!folder.exists()) {
			folder.mkdirs();
		}
		String title = JOptionPane.showInputDialog ( "Backup Agent Title:" );
		if (new File(dataPath.getText() + title + "." + BackupAgentTab.BACKUP_DATA_SUFFIX).exists()) {
			JOptionPane.showMessageDialog(this, "Backup with title '" + title + "' already exists.", "Backup already exists!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (title != null && !title.isEmpty()) {
			BackupAgentTab backupAgentTab = new BackupAgentTab(this, title);
			tabbedPane.addTab(title, backupAgentTab);
			tabbedPane.setSelectedComponent(backupAgentTab);
		}
	}
	
	public void removeCurrentTab() {
		BackupAgentTab backupAgentTab = (BackupAgentTab)tabbedPane.getSelectedComponent();
		if (backupAgentTab != null) {
			int result = JOptionPane.showConfirmDialog(this, "Do you really want to remove selected backup agent?", "Remove selected backup agent?", JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.YES_OPTION) {
				File propertFile = new File(backupAgentTab.getPopertyFileName());
				if (propertFile.exists()) {
					propertFile.delete();
				}
				tabbedPane.remove(backupAgentTab);
			}
		}
		File folder = new File(dataPath.getText());
		if (folder.exists()) {
			File[] files = folder.listFiles();
			if (files.length == 0) {
				folder.delete();
			} else if (files.length == 1 && files[0].isFile() && files[0].getName().equals(".DS_Store")) {
				files[0].delete();
				folder.delete();
			}			
		}
		updateHeaderButtons();
	}
	
	public void moveTabLeft() {
		BackupAgentTab backupAgentTab = (BackupAgentTab)tabbedPane.getSelectedComponent();
		if (backupAgentTab != null) {
			int index = tabbedPane.indexOfComponent(backupAgentTab);
			if (index > 0) {
				tabbedPane.remove(backupAgentTab);
				tabbedPane.insertTab(backupAgentTab.getTitle(), null, backupAgentTab, "", index-1);
				tabbedPane.setSelectedComponent(backupAgentTab);
			}
		}
		updateHeaderButtons();
	}

	public void moveTabRight() {
		BackupAgentTab backupAgentTab = (BackupAgentTab)tabbedPane.getSelectedComponent();
		if (backupAgentTab != null) {
			int index = tabbedPane.indexOfComponent(backupAgentTab);
			if (index < tabbedPane.getTabCount() - 1) {
				tabbedPane.remove(backupAgentTab);
				tabbedPane.insertTab(backupAgentTab.getTitle(), null, backupAgentTab, "", index+1);
				tabbedPane.setSelectedComponent(backupAgentTab);
			}
		}
		updateHeaderButtons();
	}
	
	private void updateHeaderButtons() {
		leftButton.setEnabled(tabbedPane.getSelectedIndex() > 0);
		removeButton.setEnabled(tabbedPane.getSelectedIndex() != -1);
		rightButton.setEnabled(tabbedPane.getSelectedIndex() < tabbedPane.getTabCount() - 1);
	}
	
	public void updateStatus(boolean block) {
		blockCount += block ? 1 : -1;
		if (blockCount < 0) {
			blockCount = 0;
		}
		dataPath.setEnabled(blockCount == 0);
		browseDataPathButton.setEnabled(blockCount == 0);
		loadDataButton.setEnabled(blockCount == 0);
	}
	
	public void save() {
		storeProperties();
		for (Component c : tabbedPane.getComponents()) {
			((BackupAgentTab)c).save();
		}
		//JOptionPane.showMessageDialog(BackupAgent.this, "Configuration has been saved!", "Saved!", JOptionPane.INFORMATION_MESSAGE);
	}
	
	public void windowClosing(WindowEvent e) {
		save();
	}
	
	public void windowActivated(WindowEvent arg0) {
	}

	public void windowClosed(WindowEvent arg0) {
	}

	public void windowDeactivated(WindowEvent arg0) {
	}

	public void windowDeiconified(WindowEvent arg0) {
	}

	public void windowIconified(WindowEvent arg0) {
	}

	public void windowOpened(WindowEvent arg0) {
	}
}
