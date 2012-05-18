/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Unit.Role;


/**
 * This panel displays the Labour Report.
 */
public final class ReportLabourDetailPanel extends ReportPanel implements ActionListener {
    
    private Map<UnitType, Map<Location, Integer>> data;
    private TypeCountMap<UnitType> unitCount;
    private List<Colony> colonies;
    private UnitType unitType;

    
    /**
     * Creates the detail portion of a labour report.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param gui The <code>GUI</code> to display on.
     * @param unitType The <code>UnitType</code> to display detail for.
     * @param data The location data.
     * @param unitCount The unit counts by type.
     * @param colonies A list of <code>Colony</code>s for this player.
     */
    public ReportLabourDetailPanel(FreeColClient freeColClient, GUI gui,
                                   UnitType unitType,
                                   Map<UnitType, Map<Location, Integer>> data,  
                                   TypeCountMap<UnitType> unitCount,
                                   List<Colony> colonies) {
        super(freeColClient, gui, Messages.message("report.labour.details"));
        this.unitType = unitType;
        this.data = data;
        this.unitCount = unitCount;
        this.colonies = colonies;
    }


    public void initialize() {

        JPanel detailPanel = new JPanel(new MigLayout("wrap 7", "[]30[][]30[][]30[][]", ""));
        detailPanel.setOpaque(false);

        Role role = Role.DEFAULT;
        if (unitType.hasAbility(Ability.EXPERT_PIONEER)) {
            role = Role.PIONEER;
        } else if (unitType.hasAbility(Ability.EXPERT_MISSIONARY)) {
            role = Role.MISSIONARY;
        }

        // summary
        detailPanel.add(new JLabel(getLibrary().getUnitImageIcon(unitType, role)), "spany");
        detailPanel.add(localizedLabel(unitType.getNameKey()));
        detailPanel.add(new JLabel(String.valueOf(unitCount.getCount(unitType))), "wrap 10");
        boolean canTrain = false;
        Map<Location, Integer> unitLocations = data.get(unitType);
        for (Colony colony : colonies) {
            if (unitLocations.get(colony) != null) {
                String colonyName = colony.getName();
                if (colony.canTrain(unitType)) {
                    canTrain = true;
                    colonyName += "*";
                }
                JButton colonyButton = getLinkButton(colonyName, null, colony.getId());
                colonyButton.addActionListener(this);
                detailPanel.add(colonyButton);
                JLabel countLabel = new JLabel(unitLocations.get(colony).toString());
                countLabel.setForeground(LINK_COLOR);
                detailPanel.add(countLabel);
            }
        }
        for (Entry<Location, Integer> entry : unitLocations.entrySet()) {
            if (!(entry.getKey() instanceof Colony)) {
                String locationName = Messages.message(entry.getKey().getLocationName());
                JButton linkButton = getLinkButton(locationName, null, entry.getKey().getId());
                linkButton.addActionListener(this);
                detailPanel.add(linkButton);
                JLabel countLabel = new JLabel(entry.getValue().toString());
                countLabel.setForeground(LINK_COLOR);
                detailPanel.add(countLabel);
            }
        }
        if (canTrain) {
            detailPanel.add(new JLabel(Messages.message("report.labour.canTrain")),
                            "newline 20, span");
        }
        reportPanel.add(detailPanel);
    }
}
