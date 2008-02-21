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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.Specification;
import net.sf.freecol.client.gui.i18n.Messages;

public final class TileImprovementType extends FreeColGameObjectType
{

    private boolean natural;
    private String typeId;
    private int magnitude;
    private int addWorkTurns;

    private int artOverlay;
    private boolean artOverTrees;

    private List<TileType> allowedTileTypes;
    private TileImprovementType requiredImprovementType;

    private Set<String> allowedWorkers;
    private EquipmentType expendedEquipmentType;
    private int expendedAmount;
    private GoodsType deliverGoodsType;
    private int deliverAmount;

    private Map<String, Modifier> modifiers = new HashMap<String, Modifier>();
    private Map<TileType, TileType> tileTypeChange = new HashMap<TileType, TileType>();

    private int movementCost;
    private float movementCostFactor;

    // ------------------------------------------------------------ constructors

    public TileImprovementType(int index) {
        setIndex(index);
    }

    // ------------------------------------------------------------ retrieval methods

    public boolean isNatural() {
        return natural;
    }

    public int getMagnitude() {
        return magnitude;
    }

    public int getAddWorkTurns() {
        return addWorkTurns;
    }

    public String getOccupationString() {
        return Messages.message(getId() + ".occupationString");
    }

    // TODO: Make this work like the other *types with images using Hashtable
    // Currently only Plowing has any art, the others have special display methods (roads/rivers)
    public int getArtOverlay() {
        return artOverlay;
    }

    public boolean isArtOverTrees() {
        return artOverTrees;
    }

    public TileImprovementType getRequiredImprovementType() {
        return requiredImprovementType;
    }

    public EquipmentType getExpendedEquipmentType() {
        return expendedEquipmentType;
    }

    public int getExpendedAmount() {
        return expendedAmount;
    }

    public GoodsType getDeliverGoodsType() {
        return deliverGoodsType;
    }

    public int getDeliverAmount() {
        return deliverAmount;
    }

    public boolean isWorkerTypeAllowed(UnitType unitType) {
    	return allowedWorkers.isEmpty() || allowedWorkers.contains(unitType.getId());
    }

    /**
     * Check if a given <code>Unit</code> can perform this TileImprovement.
     * @return true if Worker UnitType is allowed and expended Goods are available
     */
    public boolean isWorkerAllowed(Unit unit) {
        if (!isWorkerTypeAllowed(unit.getType())) {
            return false;
        }
        if (expendedAmount == 0) {
            return true;
        }
        int count = 0;
        for (EquipmentType equipmentType : unit.getEquipment()) {
            if (equipmentType == expendedEquipmentType) {
                count++;
                if (count >= expendedAmount) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This will check if in principle this type of improvement can be used on
     * this kind of tile, disregarding the current state of an actual tile.
     *
     * If you want to find out if an improvement is allowed for a tile, call
     * {@link #isTileAllowed(Tile)}.
     *
     * @param tileType The type of terrain
     * @return true if improvement is possible
     */
    public boolean isTileTypeAllowed(TileType tileType) {
        return (allowedTileTypes.indexOf(tileType) >= 0);
    }

    /**
     * Check if a given <code>Tile</code> is valid for this TileImprovement.
     *
     * @return true if Tile TileType is valid and required Improvement (if any)
     *         is present.
     */
    public boolean isTileAllowed(Tile tile) {
        if (!isTileTypeAllowed(tile.getType())) {
            return false;
        }
        if (requiredImprovementType != null && tile.findTileImprovementType(requiredImprovementType) == null) {
            return false;
        }
        if (tile.findTileImprovementType(this) != null) {
            return false;
        }
        return true;
    }

    public int getBonus(GoodsType goodsType) {
        Modifier result = modifiers.get(goodsType.getId());
        if (result == null) {
            return 0;
        } else {
            return (int) result.getValue();
        }
    }

    public Modifier getProductionBonus(GoodsType goodsType) {
        return modifiers.get(goodsType.getId());
    }

    public TileType getChange(TileType tileType) {
        return tileTypeChange.get(tileType);
    }

    /**
     * Returns a value for use in AI decision making.
     * @param tileType The <code>TileType</code> to be considered. A <code>null</code> entry
     *        denotes no interest in a TileImprovementType that changes TileTypes
     * @param goodsType A preferred <code>GoodsType</code> or <code>null</code>
     * @return Sum of all bonuses with a triple bonus for the preferred GoodsType
     */
    public int getValue(TileType tileType, GoodsType goodsType) {
        List<GoodsType> goodsList = FreeCol.getSpecification().getGoodsTypeList();
        // 2 main types TileImprovementTypes - Changing of TileType and Simple Bonus
        TileType newTileType = getChange(tileType);
        int value = 0;
        if (newTileType != null) {
            // Calculate difference in output
            for (GoodsType g : goodsList) {
                if (!g.isFarmed())
                    continue;
                int change = newTileType.getPotential(g) - tileType.getPotential(g);
                if (goodsType == g) {
                    if (change < 0) {
                        return 0;   // Reject if there is a drop in preferred GoodsType
                    } else {
                        change *= 3;
                    }
                }
                value += change;
            }
        } else {
            // Calculate bonuses from TileImprovementType
            for (Modifier modifier : modifiers.values()) {
                float change = modifier.applyTo(1);
                if (modifier.getId().equals(goodsType.getId())) {
                    if (change < 1) {
                        // Reject if there is a drop in preferred GoodsType
                        return 0;
                    } else {
                        change *= 3;
                    }
                }
                value += change;
            }
        }
        return value;
    }

    /**
     * Performs reduction of the movement-cost.
     * @param moveCost Original movement cost
     * @return The movement cost after any change
     */
    public int getMovementCost(int moveCost) {
        int cost = moveCost;
        if (movementCostFactor >= 0) {
            float cost2 = (float)cost * movementCostFactor;
            cost = (int)cost2;
            if (cost < cost2) {
                cost++;
            }
        }
        if (movementCost >= 0) {
            if (movementCost < cost) {
                return movementCost;
            } else {
                return cost;
            }
        }
        return cost;
    }

    // ------------------------------------------------------------ API methods

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        throw new UnsupportedOperationException("Call 'readFromXML' instead.");
    }

    public void readFromXML(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        setId(in.getAttributeValue(null, "id"));
        natural = getAttribute(in, "natural", false);
        addWorkTurns = getAttribute(in, "add-work-turns", 0);
        movementCost = -1;
        movementCostFactor = -1;
        magnitude = getAttribute(in, "magnitude", 1);

        String req = in.getAttributeValue(null, "required-improvement");
        if (req != null) {
            requiredImprovementType = specification.getTileImprovementType(req);
        }
        artOverlay = getAttribute(in, "overlay", -1);
        artOverTrees = getAttribute(in, "over-trees", false);

        String g = in.getAttributeValue(null, "expended-equipment-type");
        if (g != null) {
            expendedEquipmentType = specification.getEquipmentType(g);
        }
        expendedAmount = getAttribute(in, "expended-amount", 0);
        g = in.getAttributeValue(null, "deliver-goods-type");
        if (g != null) {
            deliverGoodsType = specification.getGoodsType(g);
        }
        deliverAmount = getAttribute(in, "deliver-amount", 0);

        allowedWorkers = new HashSet<String>();
        allowedTileTypes = new ArrayList<TileType>();
        modifiers = new HashMap<String, Modifier>();
        tileTypeChange = new HashMap<TileType, TileType>();

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if ("tiles".equals(childName)) {
                boolean allLand = getAttribute(in, "all-land-tiles", false);
                boolean allForestUndefined = in.getAttributeValue(null, "all-forest-tiles") == null;
                boolean allForest = getAttribute(in, "all-forest-tiles", false);
                boolean allWater = getAttribute(in, "all-water-tiles", false);

                for (TileType t : specification.getTileTypeList()) {
                    if (t.isWater()){
                        if (allWater)
                            allowedTileTypes.add(t);
                    } else {
                        if (t.isForested()){
                            if ((allLand && allForestUndefined) || allForest){
                                allowedTileTypes.add(t);
                            }
                        } else {
                            if (allLand){
                                allowedTileTypes.add(t);
                            }
                        }
                		
                    }
                }
                in.nextTag(); // close this element

            } else if ("tile".equals(childName)) {
                String tileId = in.getAttributeValue(null, "id");
                allowedTileTypes.add(specification.getTileType(tileId));
                in.nextTag(); // close this element

            } else if ("worker".equals(childName)) {
                allowedWorkers.add(in.getAttributeValue(null, "id"));
                in.nextTag(); // close this element

            } else if (Modifier.getXMLElementTagName().equals(childName)) {
                Modifier modifier = new Modifier(in);
                if (modifier.getSource() == null) {
                    modifier.setSource(this.getId());
                }
                modifiers.put(modifier.getId(), modifier);
            } else if ("change".equals(childName)) {
                tileTypeChange.put(specification.getTileType(in.getAttributeValue(null, "from")),
                                   specification.getTileType(in.getAttributeValue(null, "to")));
                in.nextTag(); // close this element

            } else {
                throw new RuntimeException("unexpected: " + childName);
            }
        }
    }
}
