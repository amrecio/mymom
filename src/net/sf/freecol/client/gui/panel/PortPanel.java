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

import java.awt.event.MouseListener;
import java.util.List;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Unit;

/**
 * This class provides common functionality for panels that display
 * ports, such as the ColonyPanel and the EuropePanel. This includes
 * an InPortPanel for displaying the carriers in port, and a
 * CargoPanel for displaying the cargo aboard that carrier.
 */
public abstract class PortPanel extends FreeColPanel {

    protected CargoPanel cargoPanel;
    protected UnitLabel selectedUnitLabel;
    protected DefaultTransferHandler defaultTransferHandler;
    protected MouseListener pressListener;
    protected InPortPanel inPortPanel;

    public PortPanel(FreeColClient client, GUI gui) {
        super(client, gui);
    }

    /**
     * Gets the cargo panel.
     *
     * @return The cargo panel.
     */
    public final CargoPanel getCargoPanel() {
        return cargoPanel;
    }

    /**
     * Returns the currently select unit.
     *
     * @return The currently select unit.
     */
    public Unit getSelectedUnit() {
        return (selectedUnitLabel == null) ? null
            : selectedUnitLabel.getUnit();
    }

    /**
     * Returns the currently select unit label.
     *
     * @return The currently select unit label.
     */
    public UnitLabel getSelectedUnitLabel() {
        return selectedUnitLabel;
    }

    public void setSelectedUnitLabel(UnitLabel label) {
        selectedUnitLabel = label;
    }

    public DefaultTransferHandler getTransferHandler() {
        return defaultTransferHandler;
    }

    public MouseListener getPressListener() {
        return pressListener;
    }

    public abstract List<Unit> getUnitList();

}