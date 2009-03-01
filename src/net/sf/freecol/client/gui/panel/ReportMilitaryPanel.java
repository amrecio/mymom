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

package net.sf.freecol.client.gui.panel;

import java.awt.Dimension;
import java.awt.event.ActionListener;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 * This panel displays the Military Report.
 */
public final class ReportMilitaryPanel extends ReportPanel {

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportMilitaryPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.military"));
        reportPanel.add(new ReportUnitPanel(ReportUnitPanel.ReportType.MILITARY, false, getCanvas(), this));
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(750, 600);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }
    
}
