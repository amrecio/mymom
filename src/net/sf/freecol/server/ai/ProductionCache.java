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

package net.sf.freecol.server.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitTypeChange;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.WorkLocation;


public class ProductionCache {

    private final Colony colony;

    /**
     * The units available in the colony.
     */
    private final Set<Unit> units;

    /**
     * The available colony tiles.
     */
    private final Set<ColonyTile> colonyTiles;

    /**
     * Sorted entries per goods type.
     */
    private final Map<GoodsType, List<Entry>> entries;

    /**
     * The assigned entries.
     */
    private final List<Entry> assigned = new ArrayList<Entry>();

    /**
     * The reserved entries.
     */
    private final List<Entry> reserved = new ArrayList<Entry>();

    /**
     * Compares entries by production.
     */
    private static final Comparator<Entry> defaultComparator =
        new CacheEntryComparator();

    /**
     * Compares entries by market value of production.
     */
    private static final Comparator<Entry> marketValueComparator =
        new CacheEntryComparator() {
            public int compareProduction(Entry entry1, Entry entry2) {
                int production = entry2.getProduction() - entry1.getProduction();
                Market market = entry1.getUnit().getOwner().getMarket();
                if (market != null) {
                    production = market.getSalePrice(entry2.getGoodsType(), entry2.getProduction())
                        - market.getSalePrice(entry1.getGoodsType(), entry1.getProduction());
                }
                return production;
            }
        };

    /**
     * The number of units available.
     */
    private int unitCount;

    /**
     * The number of Units in various buildings.
     */
    private TypeCountMap<BuildingType> unitCounts = new TypeCountMap<BuildingType>();


    public ProductionCache(Colony colony) {
        this.colony = colony;
        this.units = new HashSet<Unit>(colony.getUnitList());
        this.unitCount = units.size();
        this.colonyTiles = new HashSet<ColonyTile>();
        // this assumes all colonists can be added to any tile
        Unit someUnit = colony.getUnitList().get(0);
        for (ColonyTile colonyTile : colony.getColonyTiles()) {
            if (colonyTile.getUnit() != null || colonyTile.canAdd(someUnit)) {
                colonyTiles.add(colonyTile);
            }
        }
        this.entries = new HashMap<GoodsType, List<Entry>>();
    }

    private List<Entry> createEntries(GoodsType goodsType) {
        List<Entry> result = new ArrayList<Entry>();
        if (goodsType.isFarmed()) {
            for (ColonyTile colonyTile : colonyTiles) {
                Tile tile = colonyTile.getWorkTile();
                if (tile.potential(goodsType, null) > 0
                    || (tile.hasResource()
                        && !tile.getTileItemContainer().getResource().getType()
                        .getModifierSet(goodsType.getId()).isEmpty())) {
                    for (Unit unit : units) {
                        result.add(new Entry(goodsType, colonyTile, unit));
                    }
                }
            }
        } else {
            Building building = colony.getBuildingForProducing(goodsType);
            if (building != null && building.getType().getWorkPlaces() > 0) {
                for (Unit unit : units) {
                    result.add(new Entry(goodsType, building, unit));
                }
            }
        }
        Collections.sort(result, defaultComparator);
        entries.put(goodsType, result);
        return result;
    }

    public Set<Unit> getUnits() {
        return units;
    }

    public int getUnitCount() {
        return unitCount;
    }

    public int getUnitCount(BuildingType buildingType) {
        return unitCounts.getCount(buildingType);
    }

    public int decrementUnitCount(BuildingType buildingType) {
        Integer result = unitCounts.incrementCount(buildingType, -1);
        return (result == null) ? 0 : result.intValue();
    }

    public List<Entry> getAssigned() {
        return assigned;
    }

    public List<Entry> getReserved() {
        return reserved;
    }


    public List<Entry> getEntries(GoodsType goodsType) {
        List<Entry> result = entries.get(goodsType);
        if (result == null) {
            result = createEntries(goodsType);
        }
        return result;
    }

    public List<Entry> getEntries(List<GoodsType> goodsTypes) {
        return getEntries(goodsTypes, false);
    }

    public List<Entry> getEntries(List<GoodsType> goodsTypes, boolean useMarketValues) {
        List<Entry> result = new ArrayList<Entry>();
        for (GoodsType goodsType : goodsTypes) {
            result.addAll(getEntries(goodsType));
        }
        if (useMarketValues) {
            Collections.sort(result, marketValueComparator);
        } else {
            Collections.sort(result, defaultComparator);
        }
        return result;
    }



    public void assign(Entry entry) {
        ColonyTile colonyTile = null;
        Building building = null; 
        if (entry.getWorkLocation() instanceof ColonyTile) {
            colonyTile = (ColonyTile) entry.getWorkLocation();
            colonyTiles.remove(colonyTile);
        } else if (entry.getWorkLocation() instanceof Building) {
            building = (Building) entry.getWorkLocation();
            unitCounts.incrementCount(building.getType(), 1);
        }
        Unit unit = null;
        if (!entry.isOtherExpert()) {
            unit = entry.getUnit();
            units.remove(unit);
            assigned.add(entry);
            removeEntries(unit, colonyTile, reserved);
        } else {
            if (colonyTile == null) {
                if (unitCounts.getCount(building.getType()) == 1) {
                    // only add building once
                    reserved.addAll(entries.get(entry.getGoodsType()));
                }
            } else {
                reserved.addAll(removeEntries(null, colonyTile, entries.get(entry.getGoodsType())));
            }
        }
        // if work location is a colony tile, remove it from all other
        // lists, because it only supports a single unit
        for (List<Entry> entryList : entries.values()) {
            removeEntries(unit, colonyTile, entryList);
        }
        unitCount--;
    }

    private void removeEntries(Unit unit, WorkLocation workLocation) {
        units.remove(unit);
        if (workLocation instanceof ColonyTile) {
            colonyTiles.remove((ColonyTile) workLocation);
        }
        for (List<Entry> entryList : entries.values()) {
            removeEntries(unit, workLocation, entryList);
        }
        removeEntries(unit, null, reserved);
    }


    public static List<Entry> removeEntries(Unit unit, WorkLocation workLocation, List<Entry> entryList) {
        Iterator<Entry> entryIterator = entryList.iterator();
        List<Entry> removedEntries = new ArrayList<Entry>();
        while (entryIterator.hasNext()) {
            Entry entry = entryIterator.next();
            if (entry.getUnit() == unit
                || entry.getWorkLocation() == workLocation) {
                removedEntries.add(entry);
                entryIterator.remove();
            }
        }
        return removedEntries;
    }

    public class Entry {
        private final GoodsType goodsType;
        private final WorkLocation workLocation;
        private final Unit unit;
        private final int production;
        private boolean isExpert = false;
        private boolean isOtherExpert = false;
        private boolean unitUpgrades = false;
        private boolean unitUpgradesToExpert = false;

        public Entry(GoodsType g, WorkLocation w, Unit u) {
            goodsType = g;
            workLocation = w;
            unit = u;
            if (workLocation instanceof ColonyTile) {
                production = ((ColonyTile) workLocation).getWorkTile().potential(goodsType, unit.getType());
            } else if (workLocation instanceof Building) {
                production = ((Building) workLocation).getUnitProductivity(unit);
            } else {
                production = 0;
            }
            GoodsType expertProduction = unit.getType().getExpertProduction();
            if (expertProduction != null) {
                if (expertProduction == goodsType) {
                    isExpert = true;
                } else {
                    isOtherExpert = true;
                }
            } else {
                for (UnitTypeChange change : unit.getType().getTypeChanges()) {
                    if (change.asResultOf(ChangeType.EXPERIENCE)) {
                        if (change.getNewUnitType().getExpertProduction() == goodsType) {
                            unitUpgrades = true;
                            unitUpgradesToExpert = true;
                            break;
                        } else {
                            unitUpgrades = true;
                        }
                    }
                }
            }
        }

        public GoodsType getGoodsType() {
            return goodsType;
        }

        public WorkLocation getWorkLocation() {
            return workLocation;
        }

        public Unit getUnit() {
            return unit;
        }

        public int getProduction() {
            return production;
        }

        public boolean isExpert() {
            return isExpert;
        }

        public boolean isOtherExpert() {
            return isOtherExpert;
        }

        public boolean unitUpgrades() {
            return unitUpgrades;
        }

        public boolean unitUpgradesToExpert() {
            return unitUpgradesToExpert;
        }

        public String toString() {
            String result = "Cache entry: " + unit.getName() + "(" + unit.getId() + ") ";
            if (workLocation instanceof ColonyTile) {
                return result
                    + ((ColonyTile) workLocation).getTile().getName()
                    + "(" + workLocation.getId() + ") " + goodsType.getName();
            } else if (workLocation instanceof Building) {
                return result
                    + ((Building) workLocation).getName() + "(" + workLocation.getId() + ") ";
            } else {
                return result;
            }
        }
    }




}