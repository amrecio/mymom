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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Colony.ColonyChangeEvent;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;


/**
 * Helper container to remember a unit state prior to some change,
 * and fire off any consequent property changes.
 */
public class UnitWas {

    private static final Logger logger = Logger.getLogger(UnitWas.class.getName());

    private Unit unit;
    private UnitType type;
    private Unit.Role role;
    private Location loc;
    private GoodsType work;
    private int workAmount;
    private Colony colony;
    private GoodsContainer goodsContainer;
    private TypeCountMap<EquipmentType> equipment;


    /**
     * Main constructor.
     *
     * @param unit The <code>Unit</code> to check changes to.
     */
    public UnitWas(Unit unit) {
        this.unit = unit;
        this.type = unit.getType();
        this.role = unit.getRole();
        this.loc = unit.getLocation();
        this.work = unit.getWorkType();
        this.workAmount = getAmount(loc, work);
        this.colony = unit.getColony();
        this.equipment = new TypeCountMap<EquipmentType>();
        this.equipment.add(unit.getEquipment());
        if (unit.getGoodsContainer() != null) {
            unit.getGoodsContainer().saveState();
        }
    }

    /**
     * Fire any property changes resulting from actions of a unit.
     */
    public void fireChanges() {
        UnitType newType = null;
        Unit.Role newRole = null;
        Location newLoc = null;
        GoodsType newWork = null;
        int newWorkAmount = 0;
        TypeCountMap<EquipmentType> newEquipment = null;
        if (!unit.isDisposed()) {
            newLoc = unit.getLocation();
            if (colony != null) {
                newType = unit.getType();
                newRole = unit.getRole();
                newWork = unit.getWorkType();
                newWorkAmount = (newWork == null) ? 0
                    : getAmount(newLoc, newWork);
                newEquipment = unit.getEquipment();
            }
        }

        if (loc != newLoc) {
            FreeColGameObject oldFcgo = (FreeColGameObject) loc;
            oldFcgo.firePropertyChange(change(oldFcgo), unit, null);
            if (newLoc != null) {
                FreeColGameObject newFcgo = (FreeColGameObject) newLoc;
                newFcgo.firePropertyChange(change(newFcgo), null, unit);
            }
        }
        if (colony != null) {
            if (type != newType && newType != null) {
                String pc = ColonyChangeEvent.UNIT_TYPE_CHANGE.toString();
                colony.firePropertyChange(pc, type, newType);
            } else if (role != newRole && newRole != null) {
                String pc = Tile.UNIT_CHANGE.toString();
                colony.firePropertyChange(pc, role.toString(),
                    newRole.toString());
            }
            if (work == newWork) {
                if (work != null && workAmount != newWorkAmount) {
                    colony.firePropertyChange(work.getId(),
                        workAmount, newWorkAmount);
                }
            } else {
                if (work != null) {
                    colony.firePropertyChange(work.getId(), workAmount, 0);
                }
                if (newWork != null) {
                    colony.firePropertyChange(newWork.getId(),
                        0, newWorkAmount);
                }
            }
        }
        if (newEquipment != null) {
            Set<EquipmentType> keys = new HashSet<EquipmentType>();
            keys.addAll(equipment.keySet());
            keys.addAll(newEquipment.keySet());
            for (EquipmentType e : keys) {
                int cOld = equipment.getCount(e);
                int cNew = newEquipment.getCount(e);
                if (cOld != cNew) {
                    unit.firePropertyChange(Unit.EQUIPMENT_CHANGE, cOld, cNew);
                }
            }
        }
        if (unit.getGoodsContainer() != null) {
            unit.getGoodsContainer().fireChanges();
        }
    }

    // TODO: fix this non-OO nastiness
    private String change(FreeColGameObject fcgo) {
        return (fcgo instanceof Tile) ? Tile.UNIT_CHANGE
            : (fcgo instanceof Europe) ? Europe.UNIT_CHANGE
            : (fcgo instanceof ColonyTile) ? ColonyTile.UNIT_CHANGE
            : (fcgo instanceof Building) ? Building.UNIT_CHANGE
            : (fcgo instanceof Unit) ? Unit.CARGO_CHANGE
            : null;
    }

    // TODO: fix this non-OO nastiness
    private int getAmount(Location location, GoodsType goodsType) {
        if (goodsType != null) {
            if (location instanceof Building) {
                Building building = (Building) location;
                ProductionInfo info = building.getProductionInfo();
                return (info == null || info.getProduction() == null
                    || info.getProduction().size() == 0) ? 0
                    : info.getProduction().get(0).getAmount();
            } else if (location instanceof ColonyTile) {
                return ((ColonyTile)location).getProductionOf(goodsType);
            }
        }
        return 0;
    }
}
