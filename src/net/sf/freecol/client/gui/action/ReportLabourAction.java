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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.CompactLabourReport;
import net.sf.freecol.client.gui.panel.ReportLabourPanel;


/**
 * 
 */
public class ReportLabourAction extends MapboardAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ReportLabourAction.class.getName());


    public static final String id = "reportLabourAction";
    
    /**
     * Creates this action.
     * @param freeColClient The main controller object for the client.
     */
    ReportLabourAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.report.labour", null, KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));        
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
     * @return "reportLabourAction"
     */
    public String getId() {
        return id;
    }

    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        if (freeColClient.getClientOptions().getInteger(ClientOptions.LABOUR_REPORT) ==
            ClientOptions.LABOUR_REPORT_CLASSIC) {
            freeColClient.getCanvas().showPanel(new ReportLabourPanel(freeColClient.getCanvas()));
        } else {
            freeColClient.getCanvas().showPanel(new CompactLabourReport(freeColClient.getCanvas()));
        }
    }
}
