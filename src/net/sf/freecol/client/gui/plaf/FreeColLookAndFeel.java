package net.sf.freecol.client.gui.plaf;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

import net.sf.freecol.common.FreeColException;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.logging.Logger;


/**
 * Implements the "FreeCol Look and Feel".
 */
public class FreeColLookAndFeel extends MetalLookAndFeel {
    private static final Logger logger = Logger.getLogger(FreeColLookAndFeel.class.getName());
    
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private final static Class resourceLocator = net.sf.freecol.FreeCol.class;
    private File dataDirectory;
    
    private static final Color PRIMARY_1 = new Color(122, 109, 82),
                               BG_COLOR_SELECT = new Color(255, 244, 195),
                               PRIMARY_3 = new Color(203, 182, 136),
                               SECONDARY_1 = new Color(10, 10, 10),
                               DISABLED_COLOR = new Color(166, 144, 95),
                               BG_COLOR = new Color(216, 194, 145);
                               
                               


    /**
     * Initiates a new "FreeCol Look and Feel".
     *
     * @exception FreeColException If the ui directory could not be found.
     */
    public FreeColLookAndFeel() throws FreeColException {
        this("");
    }
    

   /**
    * Initiates a new "FreeCol Look and Feel".
    *
    * @param dataFolder The home of the FreeCol data files.
    * @exception FreeColException If the ui directory could not be found.
    */
    public FreeColLookAndFeel(String dataFolder) throws FreeColException {
        super();
         
        if(dataFolder.equals("")) { // lookup is necessary
            dataDirectory = new File("data" + System.getProperty("file.separator"));            

            if(!dataDirectory.exists() || !dataDirectory.isDirectory()) {
                dataDirectory = null;                                
            }
        } else {
            dataDirectory = new File(dataFolder);

            if(!dataDirectory.exists() || !dataDirectory.isDirectory()) {
                throw new FreeColException("Data directory not found in: " + dataDirectory.getName());
            }
        }
        
        setCurrentTheme(new DefaultMetalTheme() {
            protected ColorUIResource getPrimary1() {
                return new ColorUIResource(PRIMARY_1);
            }

            protected ColorUIResource getPrimary2() {
                return new ColorUIResource(BG_COLOR_SELECT);
            }

            protected ColorUIResource getPrimary3() {
                return new ColorUIResource(PRIMARY_3);
            }

            protected ColorUIResource getSecondary1() {
                return new ColorUIResource(SECONDARY_1);
            }

            protected ColorUIResource getSecondary2() {
                return new ColorUIResource(DISABLED_COLOR);
            }

            protected ColorUIResource getSecondary3() {
                return new ColorUIResource(BG_COLOR);
            }
        });
    }


    
    
   /**
    * Creates the look and feel specific defaults table.
    * @return The defaults table.
    */
    public UIDefaults getDefaults() {
        UIDefaults u = super.getDefaults();

        try {
            u.put("CheckBoxUI", "net.sf.freecol.client.gui.plaf.FreeColCheckBoxUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColCheckBoxUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColCheckBoxUI"));
            u.put("ComboBoxUI", "net.sf.freecol.client.gui.plaf.FreeColComboBoxUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColComboBoxUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColComboBoxUI"));
            u.put("RadioButtonUI", "net.sf.freecol.client.gui.plaf.FreeColRadioButtonUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColRadioButtonUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColRadioButtonUI"));
            u.put("ButtonUI", "net.sf.freecol.client.gui.plaf.FreeColButtonUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColButtonUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColButtonUI"));
            u.put("TextFieldUI", "net.sf.freecol.client.gui.plaf.FreeColTextFieldUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColTextFieldUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColTextFieldUI"));
            u.put("TextAreaUI", "net.sf.freecol.client.gui.plaf.FreeColTextAreaUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColTextAreaUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColTextAreaUI"));
            u.put("PanelUI", "net.sf.freecol.client.gui.plaf.FreeColPanelUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColPanelUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColPanelUI"));
            u.put("MenuBarUI", "net.sf.freecol.client.gui.plaf.FreeColMenuBarUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColMenuBarUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColMenuBarUI"));
            u.put("PopupMenuUI", "net.sf.freecol.client.gui.plaf.FreeColPopupMenuUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColPopupMenuUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColPopupMenuUI"));
            u.put("LabelUI", "net.sf.freecol.client.gui.plaf.FreeColLabelUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColLabelUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColLabelUI"));
            u.put("MenuItemUI", "net.sf.freecol.client.gui.plaf.FreeColMenuItemUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColMenuItemUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColMenuItemUI"));
            u.put("ListUI", "net.sf.freecol.client.gui.plaf.FreeColListUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColListUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColListUI"));
            u.put("TableUI", "net.sf.freecol.client.gui.plaf.FreeColTableUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColTableUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColTableUI"));
            u.put("TableHeaderUI", "net.sf.freecol.client.gui.plaf.FreeColTableHeaderUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColTableHeaderUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColTableHeaderUI"));
            u.put("ScrollPaneUI", "net.sf.freecol.client.gui.plaf.FreeColScrollPaneUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColScrollPaneUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColScrollPaneUI"));
            u.put("ToolTipUI", "net.sf.freecol.client.gui.plaf.FreeColToolTipUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColToolTipUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColToolTipUI"));
            //u.put("CargoPanelUI", "net.sf.freecol.client.gui.plaf.FreeColCargoPanelUI");
            //u.put("net.sf.freecol.client.gui.plaf.FreeColCargoPanelUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColCargoPanelUI"));

            // Sharing FreeColBrightPanelUI:
            u.put("net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI"));
            u.put("ToAmericaPanelUI", "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI");            
            u.put("ToEuropePanelUI", "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI");            
            u.put("DocksPanelUI", "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI");
            u.put("InPortPanelUI", "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI");
            u.put("MarketPanelUI", "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI");
            u.put("CargoPanelUI", "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI");
            u.put("BuildingsPanelUI", "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI");
            u.put("OutsideColonyPanelUI", "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI");
            u.put("InPortPanelUI", "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI");
            u.put("WarehousePanelUI", "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI");

            // Add image UI resources:
            String [][] resources = {                
                {"BackgroundImage", "bg.png"},
                {"BackgroundImage2", "bg2.png"},
                {"CanvasBackgroundImage", "bg_map1.jpg"},
                {"TitleImage", "freecol2.png"},
                {"EventImage.firstLanding", "landing.png"},
                {"EventImage.meetingNatives", "meet_natives.png"},
                {"EventImage.meetingEuropeans", "meet_europeans.png"},
                {"EventImage.meetingAztec", "meet_aztec.png"},
                {"EventImage.meetingInca", "meet_inca.png"},
                {"VictoryImage", "victory.png"},
                {"FoundingFather.trade", "trade.png"},
                {"FoundingFather.exploration", "exploration.png"},
                {"FoundingFather.military", "military.png"},
                {"FoundingFather.political", "political.png"},
                {"FoundingFather.religious", "religious.png"},
                {"cursor.go.image", "go.png"},
                {"MiniMap.skin", "minimap-skin.png"},
                {"InfoPanel.skin", "infopanel-skin.png"}
            };

            /*
              Use a media tracker to ensure that the resources are loaded
              before we start the GUI.
            */
            MediaTracker mt = new MediaTracker(new Component() {});
            
            for (int i=0; i<resources.length; i++) {
                Image image = null;                
                File file = new File(dataDirectory, "images" + System.getProperty("file.separator") + "ui" + System.getProperty("file.separator") + resources[i][1]);
                
                if (file.exists() && file.isFile()) {
                    image = Toolkit.getDefaultToolkit().getImage(file.toString());    
                } else {
                    URL url = resourceLocator.getResource("data/images/ui/"+  resources[i][1]);
                    if (url != null) {
                        image = Toolkit.getDefaultToolkit().getImage(url);
                    }
                }

                if (image == null) {
                    logger.warning("Could not find image: " + resources[i][1]);
                } else {
                    mt.addImage(image, 0);
                    u.put(resources[i][0], image);
                }
            }
            
            try {
                mt.waitForID(0, 15000); // Wait a maximum of 15 seconds for the images to load.
            } catch (InterruptedException e) {
                logger.warning("Interrupted while loading resources!");
            }    
            
                        
            // Add font UI resources:
            resources = new String[][] {                
                {"HeaderFont", "ShadowedBlack.ttf"},
            };                  
            
            for (int i=0; i<resources.length; i++) {
                InputStream fontStream = null; 
                
                File file = new File(dataDirectory, "fonts" + System.getProperty("file.separator") + resources[i][1]);
                if (file.exists() && file.isFile()) {
                    try {
                        fontStream = new FileInputStream(file.toString());
                    } catch (FileNotFoundException e) {} // Ignored.
                } else {
                    URL url = resourceLocator.getResource("data/fonts/" + resources[i][1]);
                    if (url != null) {
                        try {
                            fontStream = url.openStream();
                        } catch (IOException e) {} // Ignored.
                    }
                }    

                if (fontStream != null) {  
                    try {
                        u.put(resources[i][0], Font.createFont(Font.TRUETYPE_FONT, fontStream));
                    } catch (FontFormatException e) {
                        logger.warning("Could not load font: " + resources[i][1] + " because it has the wrong format.");
                        u.put(resources[i][0], new Font("SansSerif", Font.PLAIN, 1));
                    } catch (IOException ie) {
                        logger.warning("Could not load font: " + resources[i][1] + " because of an IO problem.");
                        u.put(resources[i][0], new Font("SansSerif", Font.PLAIN, 1));                
                    }                                
                } else {           
                    logger.warning("Could not find font: " + resources[i][1]);
                    u.put(resources[i][0], new Font("SansSerif", Font.PLAIN, 1));
                }
            }          
            
            
            // Add cursors:
            Image im = (Image) u.get("cursor.go.image");
            if (im != null) {
                u.put("cursor.go", Toolkit.getDefaultToolkit().createCustomCursor(im, new Point(im.getWidth(null)/2, im.getHeight(null)/2), "go"));
            } else {
                u.put("cursor.go", Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

        } catch (ClassNotFoundException e) {
            System.err.println(e);
            System.exit(-1);
        }

        return u;
    }
    
    
    /**
    * Gets a one line description of this Look and Feel.
    * @return "The default Look and Feel for FreeCol"
    */
    public String getDescription() {
        return "The default Look and Feel for FreeCol";
    }


    /**
    * Gets the name of this Look and Feel.
    * @return "FreeCol Look and Feel"
    */
    public String getName() {
        return "FreeCol Look and Feel";
    }
}
