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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;

import net.miginfocom.swing.MigLayout;

/**
 * This panel displays the Religious Report.
 */
public final class ReportReligiousPanel extends ReportPanel implements ActionListener {


    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportReligiousPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.religion"));

        reportPanel.setLayout(new MigLayout("wrap 5, gap 20 20", "", ""));
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {

        Player player = getCanvas().getClient().getMyPlayer();

        reportPanel.add(new JLabel(Messages.message("crosses")));
        FreeColProgressBar progressBar = new FreeColProgressBar(getCanvas(), Goods.CROSSES);
        reportPanel.add(progressBar, "wrap");

        List<Colony> colonies = player.getColonies();
        Collections.sort(colonies, getCanvas().getClient().getClientOptions().getColonyComparator());

        int production = 0;
        for (Colony colony : colonies) {
            reportPanel.add(createColonyButton(colony), "split 2, flowy, align center");
            reportPanel.add(new BuildingPanel(colony.getBuildingForProducing(Goods.CROSSES), getCanvas()));
            production += colony.getProductionOf(Goods.CROSSES);
        }

        progressBar.update(0, player.getCrossesRequired(), player.getCrosses(), production);

    }


    private JButton createColonyButton(Colony colony) {
        JButton button = FreeColPanel.getLinkButton(colony.getName(), null, colony.getId());
        button.addActionListener(this);
        return button;
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.equals(Integer.toString(ReportPanel.OK))) {
            getCanvas().remove(this);
        } else {
            FreeColGameObject object = getCanvas().getClient().getGame().getFreeColGameObject(command);
            getCanvas().showColonyPanel((Colony) object);
        }
    }

}

