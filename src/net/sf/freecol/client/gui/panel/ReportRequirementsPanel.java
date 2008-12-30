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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JTextPane;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;

import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Advanced Colony Report.
 */
public final class ReportRequirementsPanel extends ReportPanel implements ActionListener {



    /**
     * A list of all the player's colonies.
     */
    private List<Colony> colonies;

    /**
     * Records the number of units indexed by colony and unit type.
     */
    private int[][] unitCount;

    /**
     * Records whether a colony can train a type of unit.
     */
    private boolean[][] canTrain; 

    /**
     * Records surplus production indexed by colony and goods type.
     */
    private int[][] surplus;


    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportRequirementsPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.requirements"));
    }

    /**
     * Prepares this panel to be displayed.
     */
    @Override
    public void initialize() {

        Player player = getCanvas().getClient().getMyPlayer();
        colonies = player.getColonies();
        Collections.sort(colonies, getCanvas().getClient().getClientOptions().getColonyComparator());

        // Display Panel
        reportPanel.setLayout(new HIGLayout(new int[] {780}, new int[] {0}));

        //Create a text pane.
        JTextPane textPane = getDefaultTextPane();
        StyledDocument doc = textPane.getStyledDocument();

        int numberUnitTypes = FreeCol.getSpecification().numberOfUnitTypes();
        int numberGoodsTypes = FreeCol.getSpecification().numberOfGoodsTypes();
        unitCount = new int[colonies.size()][numberUnitTypes];
        canTrain = new boolean[colonies.size()][numberUnitTypes];
        surplus = new int[colonies.size()][numberGoodsTypes];

        List<GoodsType> goodsTypes = FreeCol.getSpecification().getGoodsTypeList();
        // check which colonies can train which units
        for (int index = 0; index < colonies.size(); index++) {
            Colony colony = colonies.get(index);
            for (Unit unit : colony.getUnitList()) {
                unitCount[index][unit.getType().getIndex()]++;
                canTrain[index][unit.getType().getIndex()] = colony.canTrain(unit.getType());
            }
            for (GoodsType goodsType : goodsTypes) {
                surplus[index][goodsType.getIndex()] = colony.getProductionNetOf(goodsType);
            }
        }

        for (int index = 0; index < colonies.size(); index++) {

            Colony colony = colonies.get(index);

            // colonyLabel
            try {
                StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, true));
                doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                doc.insertString(doc.getLength(), "\n\n", doc.getStyle("regular"));
            } catch(Exception e) {
                logger.warning(e.toString());
            }

            boolean[] expertWarning = new boolean[numberUnitTypes];
            boolean[] productionWarning = new boolean[numberGoodsTypes];
            boolean hasWarning = false;

            // check if all unit requirements are met
            for (ColonyTile colonyTile : colony.getColonyTiles()) {
                Unit unit = colonyTile.getUnit();
                if (unit != null) {
                    GoodsType workType = unit.getWorkType();
                    UnitType expert = FreeCol.getSpecification().getExpertForProducing(workType);
                    int expertIndex = expert.getIndex();
                    if (unitCount[index][expertIndex] == 0 && !expertWarning[expertIndex]) {
                        addExpertWarning(doc, index, workType.getName(), expert);
                        expertWarning[expertIndex] = true;
                        hasWarning = true;
                    }
                }
            } 
            for (Building building : colony.getBuildings()) {
                GoodsType goodsType = building.getGoodsOutputType();
                UnitType expert = building.getExpertUnitType();
                    
                if (goodsType != null && expert != null) {
                    // check if this building has no expert producing goods
                    int expertIndex = expert.getIndex();
                    if (building.getFirstUnit() != null &&
                        !expertWarning[expertIndex] &&
                        unitCount[index][expertIndex] == 0) {
                        addExpertWarning(doc, index, goodsType.getName(), expert);
                        expertWarning[expertIndex] = true;
                        hasWarning = true;
                    }
                }
                if (goodsType != null) {
                    // not enough input
                    int goodsIndex = goodsType.getIndex();
                    if (building.getProductionNextTurn() < building.getMaximumProduction() &&
                        !productionWarning[goodsIndex]) {
                        addProductionWarning(doc, index, goodsType, building.getGoodsInputType());
                        productionWarning[goodsIndex] = true;
                        hasWarning = true;
                    }
                }
            }

            if (!hasWarning) {
                try {
                    doc.insertString(doc.getLength(), Messages.message("report.requirements.met") + "\n\n",
                                     doc.getStyle("regular"));
                } catch(Exception e) {
                    logger.warning(e.toString());
                }
            }

            // text area
            reportPanel.add(textPane, higConst.rc(1, 1));

        }
        textPane.setCaretPosition(0);

    }

    private void addExpertWarning(StyledDocument doc, int colonyIndex, String goods, UnitType workType) {
        String expertName = workType.getName();
        String colonyName = colonies.get(colonyIndex).getName();
        String newMessage = Messages.message("report.requirements.noExpert", "%colony%", colonyName, "%goods%", goods,
                "%unit%", expertName);

        try {
            doc.insertString(doc.getLength(), newMessage + "\n\n", doc.getStyle("regular"));

            ArrayList<Colony> severalExperts = new ArrayList<Colony>();
            ArrayList<Colony> canTrainExperts = new ArrayList<Colony>();
            for (int index = 0; index < colonies.size(); index++) {
                if (unitCount[index][workType.getIndex()] > 1) {
                    severalExperts.add(colonies.get(index));
                }
                if (canTrain[index][workType.getIndex()]) {
                    canTrainExperts.add(colonies.get(index));
                }
            }

            if (!severalExperts.isEmpty()) {
                doc.insertString(doc.getLength(), 
                        Messages.message("report.requirements.severalExperts", "%unit%", expertName), 
                        doc.getStyle("regular"));
                for (int index = 0; index < severalExperts.size() - 1; index++) {
                    Colony colony = severalExperts.get(index);
                    StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, false));
                    doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                    doc.insertString(doc.getLength(), ", ", doc.getStyle("regular"));
                }
                Colony colony = severalExperts.get(severalExperts.size() - 1);
                StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, false));
                doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                doc.insertString(doc.getLength(), "\n\n", doc.getStyle("regular"));
            }

            if (!canTrainExperts.isEmpty()) {
                doc.insertString(doc.getLength(), 
                        Messages.message("report.requirements.canTrainExperts", "%unit%", expertName), 
                        doc.getStyle("regular"));
                for (int index = 0; index < canTrainExperts.size() - 1; index++) {
                    Colony colony = canTrainExperts.get(index);
                    StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, false));
                    doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                    doc.insertString(doc.getLength(), ", ", doc.getStyle("regular"));
                }
                Colony colony = canTrainExperts.get(canTrainExperts.size() - 1);
                StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, false));
                doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                doc.insertString(doc.getLength(), "\n\n", doc.getStyle("regular"));
            }
  
        } catch(Exception e) {
            logger.warning(e.toString());
        }
        
    }

    private void addProductionWarning(StyledDocument doc, int colonyIndex, GoodsType output, GoodsType input) {
        String colonyName = colonies.get(colonyIndex).getName();
        String newMessage = Messages.message("report.requirements.missingGoods",
                "%colony%", colonyName,
                "%goods%", output.getName(),
                "%input%", input.getName());

        try {
            doc.insertString(doc.getLength(), newMessage + "\n\n", doc.getStyle("regular"));

            ArrayList<Colony> withSurplus = new ArrayList<Colony>();
            ArrayList<Integer> theSurplus = new ArrayList<Integer>();
            for (int index = 0; index < colonies.size(); index++) {
                if (surplus[index][input.getIndex()] > 0) {
                    withSurplus.add(colonies.get(index));
                    theSurplus.add(new Integer(surplus[index][input.getIndex()]));
                }
            }

            if (!withSurplus.isEmpty()) {
                doc.insertString(doc.getLength(),
                        Messages.message("report.requirements.surplus", "%goods%", input.getName()) + " ",
                        doc.getStyle("regular"));
                for (int index = 0; index < withSurplus.size() - 1; index++) {
                    Colony colony = withSurplus.get(index);
                    String amount = " (" + theSurplus.get(index) + ")";
                    StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, amount, false));
                    doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                    doc.insertString(doc.getLength(), ", ", doc.getStyle("regular"));
                }
                Colony colony = withSurplus.get(withSurplus.size() - 1);
                String amount = " (" + theSurplus.get(theSurplus.size() - 1) + ")";
                StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, amount, false));
                doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                doc.insertString(doc.getLength(), "\n\n", doc.getStyle("regular"));
            }


        } catch(Exception e) {
            logger.warning(e.toString());
        }
        
    }

    private JButton createColonyButton(Colony colony, boolean headline) {
        return createColonyButton(colony, "", headline);
    }

    private JButton createColonyButton(Colony colony, String info, boolean headline) {
        JButton button = getLinkButton(colony.getName() + info, null, colony.getId());
        if (headline) {
            button.setFont(smallHeaderFont);
        }
        button.addActionListener(this);
        return button;
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.equals(String.valueOf(OK))) {
            super.actionPerformed(event);
        } else {
            Colony colony = (Colony) getCanvas().getClient().getGame().getFreeColGameObject(command);
            if (colony instanceof Colony) {
                getCanvas().showColonyPanel((Colony) colony);
            }
        }
    }

}
