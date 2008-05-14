/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;

/**
 * An action for initiating chatting.
 * 
 * @see net.sf.freecol.client.gui.panel.MapControls
 */
public class ChatAction extends FreeColAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ChatAction.class.getName());




    public static final String id = "chatAction";


    /**
     * Creates a new <code>ChatAction</code>.
     * 
     * @param freeColClient The main controller object for the client.
     */
    ChatAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.game.chat", null, KeyStroke.getKeyStroke('T', Toolkit.getDefaultToolkit()
                .getMenuShortcutKeyMask()));
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if the mapboard is selected.
     */
    protected boolean shouldBeEnabled() {
        return super.shouldBeEnabled()
        		&& !getFreeColClient().isSingleplayer()
                && getFreeColClient().getCanvas() != null
                && (!getFreeColClient().getCanvas().isShowingSubPanel() || getFreeColClient().getGame() != null
                        && getFreeColClient().getGame().getCurrentPlayer() != getFreeColClient().getMyPlayer());
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "chatAction"
     */
    public String getId() {
        return id;
    }

    /**
     * Applies this action.
     * 
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getCanvas().showChatPanel();
    }
}
