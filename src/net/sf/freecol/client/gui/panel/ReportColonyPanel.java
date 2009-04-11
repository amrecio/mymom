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

import java.awt.Color;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;

import net.miginfocom.swing.MigLayout;

/**
 * This panel displays the Colony Report.
 */
public final class ReportColonyPanel extends ReportPanel {

    private List<Colony> colonies;

    private final int ROWS_PER_COLONY = 4;

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportColonyPanel(Canvas parent) {

        super(parent, Messages.message("menuBar.report.colony"));
        Player player = getMyPlayer();
        colonies = player.getColonies();

        // Display Panel
        Collections.sort(colonies, getClient().getClientOptions().getColonyComparator());

        reportPanel.setLayout(new MigLayout("wrap 12, fillx", "", ""));

        for (Colony colony : colonies) {

            // Name
            JButton button = getLinkButton(colony.getName(), null, colony.getId());
            button.addActionListener(this);
            reportPanel.add(button, "newline 20, span, split 2");
            reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

            // Units
            List<Unit> unitList = colony.getUnitList();
            Collections.sort(unitList, getUnitTypeComparator());
            for (Unit unit : unitList) {
                UnitLabel unitLabel = new UnitLabel(unit, getCanvas(), true, true);
                reportPanel.add(unitLabel);
            }
            unitList = colony.getTile().getUnitList();
            Collections.sort(unitList, getUnitTypeComparator());
            for (Unit unit : unitList) {
                UnitLabel unitLabel = new UnitLabel(unit, getCanvas(), true, true);
                reportPanel.add(unitLabel);
            }
            reportPanel.add(new JLabel(), "newline, span");

            // Production
            int netFood = colony.getFoodProduction() - colony.getFoodConsumption();
            if (netFood != 0) {
                ProductionLabel productionLabel = new ProductionLabel(Goods.FOOD, netFood, getCanvas());
                productionLabel.setStockNumber(colony.getFoodCount());
                reportPanel.add(productionLabel, "span 2");
            }
            for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
                if (goodsType.isFoodType()) {
                    continue;
                }
                int newValue = colony.getProductionNetOf(goodsType);
                int stockValue = colony.getGoodsCount(goodsType);
                if (newValue != 0 || stockValue > 0) {
                    Building building = colony.getBuildingForProducing(goodsType);
                    ProductionLabel productionLabel = new ProductionLabel(goodsType, newValue, getCanvas());
                    if (building != null) {
                        productionLabel.setMaximumProduction(building.getMaximumProduction());
                    }
                    if (goodsType == Goods.HORSES) {
                        productionLabel.setMaxGoodsIcons(1);
                    }
                    productionLabel.setStockNumber(stockValue);   // Show stored items in ReportColonyPanel
                    reportPanel.add(productionLabel, "span 2");
                }
            }
            reportPanel.add(new JLabel(), "newline, span");

            for (Building building : colony.getBuildings()) {
                reportPanel.add(new JLabel(building.getName()), "span 3");
            }

            // Buildings
            BuildableType currentType = colony.getCurrentlyBuilding();
            JLabel buildableLabel = new JLabel();
            if (currentType == null) {
                buildableLabel.setText(Messages.message("nothing"));
                buildableLabel.setForeground(Color.RED);
            } else {
                buildableLabel.setText(currentType.getName());
                buildableLabel.setForeground(Color.GRAY);
            }
            reportPanel.add(buildableLabel, "span 3");

        }

    }
}
