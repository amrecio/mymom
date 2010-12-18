/**
 *  Copyright (C) 2002-2010  The FreeCol Team
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


package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.JMenuBar;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.FreeColMenuBar;
import net.sf.freecol.client.gui.action.MapControlsAction;


/**
 * Dialog for changing the {@link net.sf.freecol.client.ClientOptions}.
 */
public final class ClientOptionsDialog extends OptionsDialog  {

    private static final Logger logger = Logger.getLogger(ClientOptionsDialog.class.getName());

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ClientOptionsDialog(Canvas parent) {
        super(parent, true);
        getButtons().clear();
        initialize(getClient().getClientOptions(), getClient().getClientOptions().getName(), null);
    }

    /**
     * This function analyses an event and calls the right methods to take
     * care of the user's requests.
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        super.actionPerformed(event);
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            getClient().saveClientOptions();
            getClient().getActionManager().update();
            JMenuBar menuBar = getClient().getFrame().getJMenuBar();
            if (menuBar != null) {
                ((FreeColMenuBar) menuBar).reset();
            }

            // Immediately redraw the minimap if that was updated.
            MapControlsAction mca = (MapControlsAction) getClient()
                .getActionManager().getFreeColAction(MapControlsAction.id);
            if (mca.getMapControls() != null) {
                mca.getMapControls().update();
            }
        }
    }
}
