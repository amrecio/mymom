

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Player;


/**
 * An action for declaring independence.
 */
public class SaveAction extends MapboardAction {
	private static final Logger logger = Logger.getLogger(SaveAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "saveAction";
    
    /**
     * Creates a new <code>DeclareIndependenceAction</code>.
     * @param freeColClient The main controller object for the client.
     */
    SaveAction(FreeColClient freeColClient) {
    	super(freeColClient, "menuBar.game.save", null, KeyStroke.getKeyStroke('S', InputEvent.CTRL_MASK));        
    }
    
    /**
     * Checks if this action should be enabled.
     * 
     * @return true if this action should be enabled.
     */
    protected boolean shouldBeEnabled() {
    	return true;
    }    
    
    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "declareIndependenceAction"
     */
    public String getId() {
        return ID;
    }

    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        freeColClient.getInGameController().saveGame();
    }
}
