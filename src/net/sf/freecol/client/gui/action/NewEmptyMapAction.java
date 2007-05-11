package net.sf.freecol.client.gui.action;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.FreeColDialog;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.server.generator.MapGenerator;

/**
 * Creates a new empty map.
 */
public class NewEmptyMapAction extends MapboardAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(NewEmptyMapAction.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static final String ID = "newEmptyMapAction";


    /**
     * Creates this action
     * 
     * @param freeColClient The main controller object for the client.
     */
    NewEmptyMapAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.game.newEmptyMap", null, null);
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if currently
     *      in map editor mode.
     * @see FreeColClient#isMapEditor()
     */
    protected boolean shouldBeEnabled() {
        return freeColClient.isMapEditor();
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return The String: "newEmptyMapAction"
     */
    public String getId() {
        return ID;
    }

    /**
     * Applies this action.
     * 
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        final Canvas c = getFreeColClient().getCanvas();
        final Game game = freeColClient.getGame();

        MapSize size = showMapSizeDialog();
        if (size == null) {
            return;
        }
        
        final MapGenerator mapGenerator = freeColClient.getFreeColServer().getMapGenerator();
        mapGenerator.getTerrainGenerator().createMap(game, new boolean[size.width][size.height]);        
        
        freeColClient.getGUI().setFocus(1, 1);
        freeColClient.getActionManager().update();
        c.refresh();
    }
    
    private MapSize showMapSizeDialog() {
        final int DEFAULT_WIDTH = 28;
        final int DEFAULT_HEIGHT = 128;
        final int COLUMNS = 5;
        
        final Canvas canvas = getFreeColClient().getCanvas();
        final String okText = Messages.message("ok");
        final String cancelText = Messages.message("cancel");
        final String widthText = Messages.message("width");
        final String heightText = Messages.message("height");
        
        final JTextField inputWidth = new JTextField(Integer.toString(28), COLUMNS);
        final JTextField inputHeight = new JTextField(Integer.toString(128), COLUMNS);

        final FreeColDialog inputDialog = new FreeColDialog()  {
            public void requestFocus() {
                inputWidth.requestFocus();
            }
        };

        inputDialog.setLayout(new BoxLayout(inputDialog, BoxLayout.Y_AXIS));

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);

        final ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    int width = Integer.parseInt(inputWidth.getText());
                    int height = Integer.parseInt(inputHeight.getText());
                    if (width <= 0 || height <= 0) {
                        throw new NumberFormatException();
                    }
                    inputDialog.setResponse(new MapSize(width, height));
                } catch (NumberFormatException nfe) {
                    canvas.errorMessage("integerAboveZero");
                }
            }
        };
        JButton okButton = new JButton(okText);
        buttons.add(okButton);
        
        JButton cancelButton = new JButton(cancelText);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                inputDialog.setResponse(null);
            }
        });
        buttons.add(cancelButton);
        inputDialog.setCancelComponent(cancelButton);
        
        okButton.addActionListener(al);
        inputWidth.addActionListener(al);
        inputHeight.addActionListener(al);
        
        JLabel widthLabel = new JLabel(widthText);
        widthLabel.setLabelFor(inputWidth);
        JLabel heightLabel = new JLabel(heightText);
        heightLabel.setLabelFor(inputHeight);
        
        JPanel widthPanel = new JPanel(new FlowLayout());
        widthPanel.setOpaque(false);
        widthPanel.add(widthLabel);
        widthPanel.add(inputWidth);
        JPanel heightPanel = new JPanel(new FlowLayout());
        heightPanel.setOpaque(false);
        heightPanel.add(heightLabel);
        heightPanel.add(inputHeight);       
        
        inputDialog.add(widthPanel);
        inputDialog.add(heightPanel);
        inputDialog.add(buttons);

        inputDialog.setSize(inputDialog.getPreferredSize());

        return (MapSize) canvas.showFreeColDialog(inputDialog);
    }
    
    private class MapSize {
        int width;
        int height;
        
        MapSize(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
