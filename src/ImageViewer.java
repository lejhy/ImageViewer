import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

/**
 * ImageViewer is the main class of the image viewer application. It builds and
 * displays the application GUI and initialises all other components.
 * 
 * To start the application, create an object of this class.
 * 
 * @author Michael Kölling and David J. Barnes.
 * @version 3.1
 */
public class ImageViewer
{
    // static fields:
    private static final String VERSION = "Version 3.1";
    private static JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));

    // fields:
    private JFrame frame;
    private ImagePanel imagePanel;
    private JLabel filenameLabel;
    private JLabel statusLabel;
    private JButton smallerButton;
    private JButton largerButton;
    private OFImage currentImage;
    private UndoManager undoManager;

    private List<Filter> filters;
    
    /**
     * Create an ImageViewer and display its GUI on screen.
     */
    public ImageViewer()
    {
        currentImage = null;
        filters = createFilters();
        makeFrame();
        undoManager = new UndoManager();
        
    }

    // ---- implementation of menu functions ----
    
    /**
     * Open function: open a file chooser to select a new image file,
     * and then display the chosen image.
     */
    private void openFile()
    {
        int returnVal = fileChooser.showOpenDialog(frame);

        if(returnVal != JFileChooser.APPROVE_OPTION) {
            return;  // cancelled
        }
        File selectedFile = fileChooser.getSelectedFile();
        currentImage = ImageFileManager.loadImage(selectedFile);
        
        if(currentImage == null) {   // image file was not a valid image
            JOptionPane.showMessageDialog(frame,
                    "The file was not in a recognized image file format.",
                    "Image Load Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        imagePanel.setImage(currentImage);
        setButtonsEnabled(true);
        showFilename(selectedFile.getPath());
        showStatus("File loaded.");
        frame.pack();
    }

    /**
     * Close function: close the current image.
     */
    private void close()
    {
        currentImage = null;
        imagePanel.clearImage();
        showFilename(null);
        setButtonsEnabled(false);
    }

    /**
     * Save As function: save the current image to a file.
     */
    private void saveAs()
    {
        if(currentImage != null) {
            int returnVal = fileChooser.showSaveDialog(frame);
    
            if(returnVal != JFileChooser.APPROVE_OPTION) {
                return;  // cancelled
            }
            File selectedFile = fileChooser.getSelectedFile();
            ImageFileManager.saveImage(currentImage, selectedFile);
            
            showFilename(selectedFile.getPath());
        }
    }

    /**
     * Quit function: quit the application.
     */
    private void quit()
    {
        System.exit(0);
    }

    /**
     * Apply a given filter to the current image.
     * 
     * @param filter   The filter object to be applied.
     */
    private void applyFilter(Filter filter)
    {
        if(currentImage != null) {
        	FilterEdit edit = new FilterEdit(currentImage, filter);
        	undoManager.addEdit(edit);
            filter.apply(currentImage);
            frame.repaint();
            showStatus("Applied: " + filter.getName());
        }
        else {
            showStatus("No image loaded.");
        }
    }
    
    private class FilterEdit extends AbstractUndoableEdit{
    	private OFImage backupImage;
    	private Filter backupFilter;
    	private FilterEdit (OFImage image, Filter filter){
    		backupImage = new OFImage(image);
    		backupFilter = filter;
    	}
    	public void undo()throws CannotUndoException {
    		showStatus("undo");
    		currentImage = new OFImage(backupImage);
            imagePanel.setImage(currentImage);
            frame.repaint();	
    	}
    	public void redo()throws CannotRedoException{
    		showStatus("redo");   
    		backupFilter.apply(currentImage);
            frame.repaint();
    	}
    	public boolean canUndo(){return true;}
    	public boolean canRedo(){return true;}
    }
    
    private class SmallerEdit extends AbstractUndoableEdit{
    	private OFImage backupImage;
    	private SmallerEdit (OFImage image){
    		backupImage = new OFImage(image);
    	}
    	public void undo() {
    		showStatus("undo");
    		currentImage = new OFImage(backupImage);
            imagePanel.setImage(currentImage);
            frame.pack();
		}
    	public void redo()throws CannotRedoException{
    		showStatus("redo");   
    		makeSmaller();
    	}
    	public boolean canUndo(){return true;}
    	public boolean canRedo(){return true;}
    }
    
    private class LargerEdit extends AbstractUndoableEdit{
    	private OFImage backupImage;
    	private LargerEdit (OFImage image){
    		backupImage = new OFImage(image);
    	}
    	public void undo() {
    		showStatus("undo");
    		currentImage = new OFImage(backupImage);
            imagePanel.setImage(currentImage);
            frame.pack();
		}
    	public void redo()throws CannotRedoException{
    		showStatus("redo");   
    		makeLarger();
    	}
    	public boolean canUndo(){return true;}
    	public boolean canRedo(){return true;}
    }

    /**
     * 'About' function: show the 'about' box.
     */
    private void showAbout()
    {
        JOptionPane.showMessageDialog(frame, 
                    "ImageViewer\n" + VERSION,
                    "About ImageViewer", 
                    JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Make the current picture larger.
     */
    private void makeLarger()
    {
        if(currentImage != null) {
        	LargerEdit edit = new LargerEdit(currentImage);
        	undoManager.addEdit(edit);
            // create new image with double size
            int width = currentImage.getWidth();
            int height = currentImage.getHeight();
            OFImage newImage = new OFImage(width * 2, height * 2);

            // copy pixel data into new image
            for(int y = 0; y < height; y++) {
                for(int x = 0; x < width; x++) {
                    Color col = currentImage.getPixel(x, y);
                    newImage.setPixel(x * 2, y * 2, col);
                    newImage.setPixel(x * 2 + 1, y * 2, col);
                    newImage.setPixel(x * 2, y * 2 + 1, col);
                    newImage.setPixel(x * 2+1, y * 2 + 1, col);
                }
            }
            
            currentImage = newImage;
            imagePanel.setImage(currentImage);
            frame.pack();
        }
    }
    

    /**
     * Make the current picture smaller.
     */
    private void makeSmaller()
    {
        if(currentImage != null) {
        	SmallerEdit edit = new SmallerEdit(currentImage);
        	undoManager.addEdit(edit);
            // create new image with double size
            int width = currentImage.getWidth() / 2;
            int height = currentImage.getHeight() / 2;
            OFImage newImage = new OFImage(width, height);

            // copy pixel data into new image
            for(int y = 0; y < height; y++) {
                for(int x = 0; x < width; x++) {
                	
                    newImage.setPixel(x, y, new Color(
                    		(currentImage.getPixel(x * 2, y * 2).getRed() + currentImage.getPixel(x * 2 + 1, y * 2).getRed() + currentImage.getPixel(x * 2, y * 2 + 1).getRed() + currentImage.getPixel(x * 2 + 1, y * 2 + 1).getRed())/4,
                    		(currentImage.getPixel(x * 2, y * 2).getGreen() + currentImage.getPixel(x * 2 + 1, y * 2).getGreen() + currentImage.getPixel(x * 2, y * 2 + 1).getGreen() + currentImage.getPixel(x * 2 + 1, y * 2 + 1).getGreen())/4,
                    		(currentImage.getPixel(x * 2, y * 2).getBlue() + currentImage.getPixel(x * 2 + 1, y * 2).getBlue() + currentImage.getPixel(x * 2, y * 2 + 1).getBlue() + currentImage.getPixel(x * 2 + 1, y * 2 + 1).getBlue())/4
                    		));
                }
            }
            
            currentImage = newImage;
            imagePanel.setImage(currentImage);
            frame.pack();
        }
    }
    
    private class CropEdit extends AbstractUndoableEdit{
    	private OFImage backupImage;
    	private int left;
    	private int right;
    	private int bottom;
    	private int top;
    	private CropEdit (OFImage image, int l, int r, int b, int t){
    		backupImage = new OFImage(image);
    		left = l;
    		right = r;
    		bottom = b;
    		top = t;
    		
    	}
    	public void undo()throws CannotRedoException{
    		showStatus("undo");
    		currentImage = new OFImage(backupImage);
            imagePanel.setImage(currentImage);
            frame.pack();
		}
    	public void redo()throws CannotRedoException{
    		showStatus("redo");   
    		crop(left, right, bottom, top);
    	}
    	public boolean canUndo(){return true;}
    	public boolean canRedo(){return true;}
    }
    
    public void crop(int l, int r, int b, int t) {
    	if(currentImage != null){
    		CropEdit edit = new CropEdit(currentImage, l, r, b, t);
    		undoManager.addEdit(edit);
    		// crop the image
	    	int width = currentImage.getWidth() - l - r;
	    	int height = currentImage.getHeight() - t - b;
			OFImage newImage = new OFImage(width, height);
			for (int y = 0; y < height; y++){
				for (int x = 0; x < width; x++){
					newImage.setPixel(x, y, currentImage.getPixel(x+l, y+t));
				}
			}
		
			currentImage = newImage;
			imagePanel.setImage(currentImage);
			frame.pack();
    	}
	}
    
    public class CropWindow extends JDialog{
    	JPanel panel = new JPanel(new GridBagLayout());
    	JLabel leftLabel = new JLabel("Left");
    	JLabel rightLabel = new JLabel("Right");
    	JLabel botomLabel = new JLabel("Bottom");
    	JLabel topLabel = new JLabel("Top");
    	JTextField left = new JTextField("0", 4);
    	JTextField right = new JTextField("0", 4);
    	JTextField bottom = new JTextField("0", 4);
    	JTextField top = new JTextField("0", 4);
    	JButton proceed = new JButton("Proceed");

    	public CropWindow(){
    		super(frame, "Crop", true);
    		if (currentImage == null) dispose();
    		setSize(250, 120);
    		setResizable(false);
    		crop(0, 0, 0, 0);
    		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    		addWindowListener(new java.awt.event.WindowAdapter() {
    		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
    		        try {
    		        	undoManager.undo();
    		        	dispose();
    		        } catch (CannotUndoException e) {
    		        	dispose();
    		        }
    		    }
    		});
    		proceed.addActionListener(new ActionListener() {
    			public void actionPerformed(ActionEvent evt) {
    				try {
    					undoManager.undo();
        				crop(Integer.parseInt(left.getText()), Integer.parseInt(right.getText()), Integer.parseInt(bottom.getText()), Integer.parseInt(top.getText()));	
        				dispose();
    		        } catch (CannotUndoException e) {
    		        	dispose();
    		        }
    			}
    		});
    		left.addActionListener(new ActionListener() {
    			public void actionPerformed(ActionEvent e) {
    				undoManager.undo();
    				crop(Integer.parseInt(left.getText()), Integer.parseInt(right.getText()), Integer.parseInt(bottom.getText()), Integer.parseInt(top.getText()));			
    			}
    		});
    		right.addActionListener(new ActionListener() {
    			public void actionPerformed(ActionEvent e) {
    				undoManager.undo();
    				crop(Integer.parseInt(left.getText()), Integer.parseInt(right.getText()), Integer.parseInt(bottom.getText()), Integer.parseInt(top.getText()));			
    			}
    		});
    		bottom.addActionListener(new ActionListener() {
    			public void actionPerformed(ActionEvent e) {
    				undoManager.undo();
    				crop(Integer.parseInt(left.getText()), Integer.parseInt(right.getText()), Integer.parseInt(bottom.getText()), Integer.parseInt(top.getText()));			
    			}
    		});
    		top.addActionListener(new ActionListener() {
    			public void actionPerformed(ActionEvent e) {
    				undoManager.undo();
    				crop(Integer.parseInt(left.getText()), Integer.parseInt(right.getText()), Integer.parseInt(bottom.getText()), Integer.parseInt(top.getText()));			
    			}
    		});
    		
    		GridBagConstraints c = new GridBagConstraints();
    		c.insets = new Insets(2, 3, 2, 3);
    		c.gridy = 0;
    		c.gridx = 0;
    		panel.add(leftLabel, c);
    		c.gridx = 1;
    		panel.add(rightLabel, c);
    		c.gridx = 2;
    		panel.add(botomLabel, c);
    		c.gridx = 3;
    		panel.add(topLabel, c);
    		c.gridy = 1;
    		c.gridx = 0;
    		panel.add(left, c);
    		c.gridx = 1;
    		panel.add(right, c);
    		c.gridx = 2;
    		panel.add(bottom, c);
    		c.gridx = 3;
    		panel.add(top, c);
    		c.gridy = 2;
    		c.gridx = 1;
    		c.gridwidth = 2;
    		panel.add(proceed, c);
    		
    		add(panel);
    		
    		setVisible(true);
    		
    	}
    }
    
    // ---- support methods ----

    /**
     * Show the file name of the current image in the fils display label.
     * 'null' may be used as a parameter if no file is currently loaded.
     * 
     * @param filename  The file name to be displayed, or null for 'no file'.
     */
    private void showFilename(String filename)
    {
        if(filename == null) {
            filenameLabel.setText("No file displayed.");
        }
        else {
            filenameLabel.setText("File: " + filename);
        }
    }
    
    
    /**
     * Show a message in the status bar at the bottom of the screen.
     * @param text The status message.
     */
    private void showStatus(String text)
    {
        statusLabel.setText(text);
    }
    
    
    /**
     * Enable or disable all toolbar buttons.
     * 
     * @param status  'true' to enable the buttons, 'false' to disable.
     */
    private void setButtonsEnabled(boolean status)
    {
        smallerButton.setEnabled(status);
        largerButton.setEnabled(status);
    }
    
    
    /**
     * Create a list with all the known filters.
     * @return The list of filters.
     */
    private List<Filter> createFilters()
    {
        List<Filter> filterList = new ArrayList<Filter>();
        filterList.add(new DarkerFilter("Darker"));
        filterList.add(new LighterFilter("Lighter"));
        filterList.add(new ThresholdFilter("Threshold"));
        filterList.add(new InvertFilter("Invert"));
        filterList.add(new SolarizeFilter("Solarize"));
        filterList.add(new SmoothFilter("Smooth"));
        filterList.add(new PixelizeFilter("Pixelize"));
        filterList.add(new MirrorFilter("Mirror"));
        filterList.add(new GrayScaleFilter("Grayscale"));
        filterList.add(new EdgeFilter("Edge Detection"));
        filterList.add(new FishEyeFilter("Fish Eye"));
       
        return filterList;
    }
    
    // ---- Swing stuff to build the frame and all its components and menus ----
    
    /**
     * Create the Swing frame and its content.
     */
    private void makeFrame()
    {
        frame = new JFrame("ImageViewer");
        JPanel contentPane = (JPanel)frame.getContentPane();
        contentPane.setBorder(new EmptyBorder(12, 12, 12, 12));

        makeMenuBar(frame);
        
        // Specify the layout manager with nice spacing
        contentPane.setLayout(new BorderLayout(6, 6));
        
        // Create the image pane in the center
        imagePanel = new ImagePanel();
        imagePanel.setBorder(new EtchedBorder());
        contentPane.add(imagePanel, BorderLayout.CENTER);

        // Create two labels at top and bottom for the file name and status messages
        filenameLabel = new JLabel();
        contentPane.add(filenameLabel, BorderLayout.NORTH);

        statusLabel = new JLabel(VERSION);
        contentPane.add(statusLabel, BorderLayout.SOUTH);
        
        // Create the toolbar with the buttons
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new GridLayout(0, 1));
        
        smallerButton = new JButton("Smaller");
        smallerButton.addActionListener(new ActionListener() {
                               public void actionPerformed(ActionEvent e) { makeSmaller(); }
                           });
        toolbar.add(smallerButton);
        
        largerButton = new JButton("Larger");
        largerButton.addActionListener(new ActionListener() {
                               public void actionPerformed(ActionEvent e) { makeLarger(); }
                           });
        toolbar.add(largerButton);

        // Add toolbar into panel with flow layout for spacing
        JPanel flow = new JPanel();
        flow.add(toolbar);
        
        contentPane.add(flow, BorderLayout.WEST);
        
        // building is done - arrange the components      
        showFilename(null);
        setButtonsEnabled(false);
        frame.pack();
        
        // place the frame at the center of the screen and show
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(d.width/2 - frame.getWidth()/2, d.height/2 - frame.getHeight()/2);
        frame.setVisible(true);
    }
    
    /**
     * Create the main frame's menu bar.
     * 
     * @param frame   The frame that the menu bar should be added to.
     */
    private void makeMenuBar(JFrame frame)
    {
        final int SHORTCUT_MASK =
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        JMenuBar menubar = new JMenuBar();
        frame.setJMenuBar(menubar);
        
        JMenu menu;
        JMenuItem item;
        
        // create the File menu
        menu = new JMenu("File");
        menubar.add(menu);
        
        item = new JMenuItem("Open...");
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, SHORTCUT_MASK));
            item.addActionListener(new ActionListener() {
                               public void actionPerformed(ActionEvent e) { openFile(); }
                           });
        menu.add(item);

        item = new JMenuItem("Close");
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, SHORTCUT_MASK));
            item.addActionListener(new ActionListener() {
                               public void actionPerformed(ActionEvent e) { close(); }
                           });
        menu.add(item);
        menu.addSeparator();

        item = new JMenuItem("Save As...");
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, SHORTCUT_MASK));
            item.addActionListener(new ActionListener() {
                               public void actionPerformed(ActionEvent e) { saveAs(); }
                           });
        menu.add(item);
        menu.addSeparator();
        
        item = new JMenuItem("Quit");
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, SHORTCUT_MASK));
            item.addActionListener(new ActionListener() {
                               public void actionPerformed(ActionEvent e) { quit(); }
                           });
        menu.add(item);
        
        // create the Edit menu
        menu = new JMenu("Edit");
        menubar.add(menu);
        item = new JMenuItem("Undo");
        	item.addActionListener(new ActionListener() {
        						public void actionPerformed(ActionEvent e) {
        							undoManager.undo();		
        						}
							});
        menu.add(item);
        item = new JMenuItem("Redo");
        	item.addActionListener(new ActionListener() {
        						public void actionPerformed(ActionEvent e) {
        							undoManager.redo();				
        						}
							});
        menu.add(item);
        menu.addSeparator();
        
        item = new JMenuItem("Crop");
        	item.addActionListener(new ActionListener() {
        						public void actionPerformed(ActionEvent e) {
        							new CropWindow();		
        						}
							});
        menu.add(item);


        // create the Filter menu
        menu = new JMenu("Filter");
        menubar.add(menu);
        
        for(final Filter filter : filters) {
            item = new JMenuItem(filter.getName());
            item.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) { 
                                    applyFilter(filter);
                                }
                           });
             menu.add(item);
         }

        // create the Help menu
        menu = new JMenu("Help");
        menubar.add(menu);
        
        item = new JMenuItem("About ImageViewer...");
            item.addActionListener(new ActionListener() {
                               public void actionPerformed(ActionEvent e) { showAbout(); }
                           });
        menu.add(item);

    }
}
