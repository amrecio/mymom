
package net.sf.freecol.client.gui.panel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.Image;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.border.*;
import javax.swing.UIManager;

/**
* A panel filled with 'main' items.
*/
public final class MainPanel extends FreeColPanel implements ActionListener {
    private static final Logger logger = Logger.getLogger(MainPanel.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    public static final int     NEW = 0,
                                OPEN = 1,
                                QUIT = 2;
    
    private final Canvas parent;
    private final FreeColClient freeColClient;
    private JButton newButton;
    

    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public MainPanel(Canvas parent, FreeColClient freeColClient) {
        setLayout(new BorderLayout());

        this.parent = parent;
        this.freeColClient = freeColClient;

        JButton         openButton = new JButton("Open"),
                        quitButton = new JButton("Quit");
        
        setCancelComponent(quitButton);
        newButton = new JButton("New");

        newButton.setActionCommand(String.valueOf(NEW));
        openButton.setActionCommand(String.valueOf(OPEN));
        quitButton.setActionCommand(String.valueOf(QUIT));

        newButton.addActionListener(this);
        openButton.addActionListener(this);
        quitButton.addActionListener(this);

        Image tempImage = (Image) UIManager.get("TitleImage");

        if (tempImage != null) {
            JLabel logoLabel = new JLabel(new ImageIcon(tempImage));
            logoLabel.setBorder(new CompoundBorder(new EmptyBorder(2,2,2,2), new BevelBorder(BevelBorder.LOWERED)));
            add(logoLabel, BorderLayout.CENTER);
        }

        JPanel buttons = new JPanel(new GridLayout(3, 1, 50, 10));

        buttons.add(newButton);
        buttons.add(openButton);
        buttons.add(quitButton);

        buttons.setBorder(new EmptyBorder(5, 25, 20, 25));        
        buttons.setOpaque(false);

        add(buttons, BorderLayout.SOUTH);

        setSize(getPreferredSize());
    }

    public void requestFocus() {
        newButton.requestFocus();
    }


    /**
    * Sets whether or not this component is enabled. It also does this for
    * its children.
    * @param enabled 'true' if this component and its children should be
    * enabled, 'false' otherwise.
    */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        Component components[] = getComponents();
        for (int i = 0; i < components.length; i++) {
            components[i].setEnabled(enabled);
        }
    }

    /**
    * This function analyses an event and calls the right methods to take
    * care of the user's requests.
    * @param event The incoming ActionEvent.
    */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Integer.valueOf(command).intValue()) {
                case NEW:
                    parent.showNewGamePanel();
                    parent.remove(this);
                    break;
                case OPEN:
                    freeColClient.getConnectController().loadGame();
                    break;
                case QUIT:
                    parent.quit();
                    break;
                default:
                    logger.warning("Invalid Actioncommand: invalid number.");
            }
        }
        catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}
