/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

package net.sf.freecol.client.gui.plaf;


import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;

import net.sf.freecol.client.gui.ImageLibrary;


/**
 * Draw the "background.FreeColBrightPanel" resource as a tiled
 * background image.  BrightPanel is intended to provide a lighter
 * background than the default panel, and is used for panels that
 * might contain icons and text annotations, for example, many of the
 * subpanels in the ColonyPanel.
 */
public class FreeColBrightPanelUI extends BasicPanelUI {
    
    private static FreeColBrightPanelUI sharedInstance = new FreeColBrightPanelUI();
    
    public static ComponentUI createUI(JComponent c) {
        return sharedInstance;
    }

    public void paint(java.awt.Graphics g, javax.swing.JComponent c) {
        if (c.isOpaque()) {
            ImageLibrary.drawTiledImage("background.FreeColBrightPanel", g, c, null);
        }
    }

}
