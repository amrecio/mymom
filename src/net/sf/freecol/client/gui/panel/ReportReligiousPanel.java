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

package net.sf.freecol.client.gui.panel;


import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;

import net.miginfocom.swing.MigLayout;

/**
 * This panel displays the Religious Report.
 */
public final class ReportReligiousPanel extends ReportPanel {


    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportReligiousPanel(Canvas parent) {
        super(parent, Messages.message("reportReligionAction.name"));

        reportPanel.setLayout(new MigLayout("wrap 5, gap 20 20", "", ""));
        Player player = getMyPlayer();

        reportPanel.add(new JLabel(Messages.message("crosses")));
        GoodsType crosses = getSpecification().getGoodsType("model.goods.crosses");
        FreeColProgressBar progressBar = new FreeColProgressBar(getCanvas(), crosses);
        reportPanel.add(progressBar, "span");

        List<Colony> colonies = getSortedColonies();
        int production = 0;
        for (Colony colony : colonies) {
            Building building = colony.getBuildingForProducing(crosses);
            reportPanel.add(createColonyButton(colony), "split 2, flowy, align center");
            reportPanel.add(new BuildingPanel(building, getCanvas()));
            production += colony.getNetProductionOf(crosses);
        }

        progressBar.update(0, player.getImmigrationRequired(), player.getImmigration(), production);

    }

}

