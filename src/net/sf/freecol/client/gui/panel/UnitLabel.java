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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;

/**
 * This label holds Unit data in addition to the JLabel data, which makes it
 * ideal to use for drag and drop purposes.
 */
public final class UnitLabel extends JLabel implements ActionListener {



    private static Logger logger = Logger.getLogger(UnitLabel.class.getName());

    public static final int ARM = 0, MOUNT = 1, TOOLS = 2, DRESS = 3,
            CLEAR_SPECIALITY = 4, ACTIVATE_UNIT = 5, FORTIFY = 6, SENTRY = 7,
            COLOPEDIA = 8, LEAVE_TOWN = 9;

    public static final int WORK_FARMING = 1000,
        WORK_LASTFARMING = WORK_FARMING + FreeCol.getSpecification().numberOfGoodsTypes();
    
    public static final int WORK_AT_BUILDING = 2000,
        WORK_AT_LASTBUILDING = WORK_AT_BUILDING + FreeCol.getSpecification().numberOfBuildingTypes();
        
    private final Unit unit;

    private final Canvas parent;

    private boolean selected;

    private boolean ignoreLocation;

    private InGameController inGameController;


    /**
     * Initializes this JLabel with the given unit data.
     * 
     * @param unit The Unit that this JLabel will visually represent.
     * @param parent The parent that knows more than we do.
     */
    public UnitLabel(Unit unit, Canvas parent) {
        ImageLibrary lib = parent.getGUI().getImageLibrary();
        setIcon(lib.getUnitImageIcon(unit));
        setDisabledIcon(lib.getUnitImageIcon(unit, true));
        this.unit = unit;
        setDescriptionLabel(unit.getName());
        this.parent = parent;
        selected = false;

        setSmall(false);
        setIgnoreLocation(false);

        this.inGameController = parent.getClient().getInGameController();
    }

    /**
     * Initializes this JLabel with the given unit data.
     * 
     * @param unit The Unit that this JLabel will visually represent.
     * @param parent The parent that knows more than we do.
     * @param isSmall The image will be smaller if set to <code>true</code>.
     */
    public UnitLabel(Unit unit, Canvas parent, boolean isSmall) {
        this(unit, parent);
        setSmall(isSmall);
        setIgnoreLocation(false);
    }

    /**
     * Initializes this JLabel with the given unit data.
     * 
     * @param unit The Unit that this JLabel will visually represent.
     * @param parent The parent that knows more than we do.
     * @param isSmall The image will be smaller if set to <code>true</code>.
     * @param ignoreLocation The image will not include production or state
     *            information if set to <code>true</code>.
     */
    public UnitLabel(Unit unit, Canvas parent, boolean isSmall, boolean ignoreLocation) {
        this(unit, parent);
        setSmall(isSmall);
        setIgnoreLocation(ignoreLocation);
    }

    /**
     * Returns the parent Canvas object.
     * 
     * @return This UnitLabel's Canvas.
     */
    public Canvas getCanvas() {
        return parent;
    }

    /**
     * Returns this UnitLabel's unit data.
     * 
     * @return This UnitLabel's unit data.
     */
    public Unit getUnit() {
        return unit;
    }

    /**
     * Sets whether or not this unit should be selected.
     * 
     * @param b Whether or not this unit should be selected.
     */
    public void setSelected(boolean b) {
        selected = b;
    }

    /**
     * Sets whether or not this unit label should include production and state
     * information.
     * 
     * @param b Whether or not this unit label should include production and
     *            state information.
     */
    public void setIgnoreLocation(boolean b) {
        ignoreLocation = b;
    }

    /**
     * Makes a smaller version.
     * 
     * @param isSmall The image will be smaller if set to <code>true</code>.
     */
    public void setSmall(boolean isSmall) {
        ImageIcon imageIcon = parent.getGUI().getImageLibrary().getUnitImageIcon(unit);
        ImageIcon disabledImageIcon = parent.getGUI().getImageLibrary().getUnitImageIcon(unit, true);
        if (isSmall) {
            setPreferredSize(null);
            // setIcon(new
            // ImageIcon(imageIcon.getImage().getScaledInstance(imageIcon.getIconWidth()
            // / 2, imageIcon.getIconHeight() / 2, Image.SCALE_DEFAULT)));
            setIcon(new ImageIcon(imageIcon.getImage().getScaledInstance((imageIcon.getIconWidth() / 3) * 2,
                    (imageIcon.getIconHeight() / 3) * 2, Image.SCALE_SMOOTH)));

            setDisabledIcon(new ImageIcon(disabledImageIcon.getImage().getScaledInstance(
                    (imageIcon.getIconWidth() / 3) * 2, (imageIcon.getIconHeight() / 3) * 2, Image.SCALE_SMOOTH)));
            setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        } else {
            if (unit.getLocation() instanceof ColonyTile) {
                TileType tileType = ((ColonyTile) unit.getLocation()).getTile().getType();
                setSize(new Dimension(parent.getGUI().getImageLibrary().getTerrainImageWidth(tileType) / 2,
                                      imageIcon.getIconHeight()));
            } else {
                setPreferredSize(null);
            }

            setIcon(imageIcon);
            setDisabledIcon(disabledImageIcon);
            if (unit.getLocation() instanceof ColonyTile) {
                setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
            } else {
                setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            }
        }

    }

    /**
     * Gets the description label.
     * 
     * The description label is a tooltip with the unit name and description of
     * the terrain its on if applicable *
     * 
     * @return This UnitLabel's description label.
     */
    public String getDescriptionLabel() {
        return getToolTipText();
    }

    /**
     * Sets the description label.
     * 
     * The description label is a tooltip with the unit name and description of
     * the terrain its on if applicable
     * 
     * @param label The string to set the label to.
     */
    public void setDescriptionLabel(String label) {
        setToolTipText(label);

    }

    /**
     * Paints this UnitLabel.
     * 
     * @param g The graphics context in which to do the painting.
     */
    public void paintComponent(Graphics g) {

        if (getToolTipText() == null) {
            setToolTipText(unit.getName());
        }

        if (ignoreLocation || selected || (!unit.isCarrier() && unit.getState() != UnitState.SENTRY)) {
            setEnabled(true);
        } else if (unit.getOwner() != parent.getClient().getMyPlayer() && unit.getColony() == null) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }

        super.paintComponent(g);
        if (ignoreLocation)
            return;

        if (unit.getLocation() instanceof ColonyTile) {
            GoodsType workType = unit.getWorkType();
            int production = ((ColonyTile) unit.getLocation()).getProductionOf(workType);

            ProductionLabel pl = new ProductionLabel(workType, production, getCanvas());
            g.translate(0, 10);
            pl.paintComponent(g);
            g.translate(0, -10);
        } else if (getParent() instanceof ColonyPanel.OutsideColonyPanel || 
            getParent() instanceof ColonyPanel.InPortPanel || 
            getParent() instanceof EuropePanel.InPortPanel || 
            getParent().getParent() instanceof ReportUnitPanel) {
            int x = (getWidth() - getIcon().getIconWidth()) / 2;
            int y = (getHeight() - getIcon().getIconHeight()) / 2;
            parent.getGUI().displayOccupationIndicator(g, unit, x, y);

            if (unit.isUnderRepair()) {
                BufferedImage repairImage = parent.getGUI()
                    .createStringImage((Graphics2D) g,
                                       Messages.message("underRepair", "%turns%",
                                                        Integer.toString(unit.getTurnsForRepair())),
                                       Color.RED, getWidth(), 16);
                g.drawImage(repairImage, (getIcon().getIconWidth() - repairImage.getWidth()) / 2,
                            (getHeight() - repairImage.getHeight()) / 2, null);
            }
        }
    }

    /**
     * Analyzes an event and calls the right external methods to take care of
     * the user's request.
     * 
     * @param event The incoming action event
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            if (command.startsWith("assign")) {
                Unit teacher = (Unit) unit.getGame().getFreeColGameObject(command.substring(6));
                inGameController.assignTeacher(unit, teacher);
                Component uc = getParent();
                while (uc != null) {
                    if (uc instanceof ColonyPanel) {
                        ((ColonyPanel) uc).reinitialize();
                        break;
                    }
                    uc = uc.getParent();
                }
                return;
            }
            int intCommand = Integer.valueOf(command).intValue();
            if (intCommand == ACTIVATE_UNIT) {
                parent.getGUI().setActiveUnit(unit);
            } else if (intCommand == FORTIFY) {
                inGameController.changeState(unit, UnitState.FORTIFYING);
            } else if (intCommand == SENTRY) {
                inGameController.changeState(unit, UnitState.SENTRY);
            } else if (!unit.isCarrier()) {
                switch (intCommand) {
                case LEAVE_TOWN:
                    inGameController.putOutsideColony(unit);
                    break;
                case CLEAR_SPECIALITY:
                    inGameController.clearSpeciality(unit);
                    break;
                case COLOPEDIA:
                    getCanvas().showColopediaPanel(ColopediaPanel.PanelType.UNITS, unit.getType());
                    break;
                default:
                    if (intCommand >= WORK_FARMING && intCommand <= WORK_LASTFARMING) {
                        GoodsType goodsType = FreeCol.getSpecification().getGoodsType(intCommand - WORK_FARMING);
                        // Move unit to best producing ColonyTile
                        ColonyTile bestTile = unit.getColony().getVacantColonyTileFor(unit, goodsType);
                        inGameController.work(unit, bestTile);
                        // Change workType
                        inGameController.changeWorkType(unit, goodsType);
                    } else if (intCommand >= WORK_AT_BUILDING && intCommand <= WORK_AT_LASTBUILDING) {
                        BuildingType buildingType = FreeCol.getSpecification().getBuildingType(intCommand - WORK_AT_BUILDING);
                        Building building = unit.getColony().getBuilding(buildingType);
                        inGameController.work(unit, building);
                    } else {
                        logger.warning("Invalid action");
                    }
                }
                updateIcon();
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid action number");
        }
    }


    public void updateIcon() {
        setIcon(parent.getGUI().getImageLibrary().getUnitImageIcon(unit));
        setDisabledIcon(parent.getGUI().getImageLibrary().getUnitImageIcon(unit, true));

        Component uc = getParent();
        while (uc != null) {
            if (uc instanceof ColonyPanel) {
                if (unit.getColony() == null) {
                    parent.remove(uc);
                    parent.getClient().getActionManager().update();
                } else {
                    ((ColonyPanel) uc).reinitialize();
                }

                break;
            } else if (uc instanceof EuropePanel) {
                break;
            }

            uc = uc.getParent();
        }

        // repaint(0, 0, getWidth(), getHeight());
        // uc.refresh();
    }
}
