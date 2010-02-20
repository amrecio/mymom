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
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.Specification;

public final class GoodsType extends FreeColGameObjectType {

    public static final int NO_BREEDING = Integer.MAX_VALUE;
    public static final int NO_PRICE = Integer.MAX_VALUE;

    private static int nextIndex = 0;

    private boolean isFarmed;
    private boolean isFood;
    private boolean ignoreLimit;
    private boolean newWorldGoods;

    /**
     * Whether this type of goods is required for building.
     */
    private boolean buildingMaterial;

    /**
     * Whether this type of goods is required for building equipment
     * that grants an offence bonus or defence bonus.
     */
    private boolean militaryGoods;

    /**
     * Whether these are trade goods that can only be obtained in
     * Europe.
     */
    private boolean tradeGoods;

    /**
     * Whether this type of goods can be stored in a warehouse.
     */
    private boolean storable;

    private GoodsType madeFrom;
    private GoodsType makes;
    private GoodsType storedAs;
    
    private int initialAmount;
    private int initialPrice;
    private int priceDiff;

    /**
     * The number of units required to breed this type of goods. This
     * obviously only applies to animals.
     */
    private int breedingNumber = NO_BREEDING;

    /**
     * The price of this type of goods. This is only used for goods
     * that can not be traded in the market, such as hammers.
     */
    private int price = NO_PRICE;


    // ----------------------------------------------------------- constructors

    public GoodsType() {
        setIndex(nextIndex++);
    }

    // ----------------------------------------------------------- retriveal methods

    public StringTemplate getLabel(boolean sellable) {
        if (sellable) {
            return StringTemplate.key(getNameKey());
        } else {
            return StringTemplate.template("model.goods.goodsBoycotted")
                .add("%goods%", getNameKey());
        }
    }

    public boolean isRawMaterial() {
        return makes != null;
    }

    public boolean isRefined() {
        return madeFrom != null;
    }

    public GoodsType getRawMaterial() {
        return madeFrom;
    }

    public GoodsType getProducedMaterial() {
        return makes;
    }

    public boolean isFarmed() {
        return isFarmed;
    }

    public boolean limitIgnored() {
        return ignoreLimit;
    }

    public boolean isNewWorldGoodsType() {
        return newWorldGoods;
    }

    public boolean isNewWorldLuxuryType() {
        return (madeFrom != null && madeFrom.isNewWorldGoodsType());
    }

    /**
     * Get the <code>ImmigrationType</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isImmigrationType() {
        return !getModifierSet("model.modifier.immigration").isEmpty();
    }

    /**
     * Get the <code>LibertyType</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isLibertyType() {
        return !getModifierSet("model.modifier.liberty").isEmpty();
    }

    public boolean isStorable() {
        return storable;
    }

    public boolean isStoredAs() {
        return storedAs!=null;
    }

    public GoodsType getStoredAs() {
        if (storedAs==null) {
            return this;
        } else {
            return storedAs;
        }
    }

    public int getInitialAmount() {
        return initialAmount;
    }

    public int getInitialSellPrice() {
        return initialPrice;
    }

    public int getInitialBuyPrice() {
        return initialPrice + priceDiff;
    }

    public int getPriceDifference() {
        return priceDiff;
    }

    // TODO: give this some meaning
    // Originally intended for when there are no static variables
    public boolean isFoodType() {
        return isFood;
    }
    
    public GoodsType outputType() {
        return makes;
    }

    public GoodsType inputType() {
        return madeFrom;
    }

    /**
     * Returns true if this type of goods is required for building a
     * BuildableType.
     *
     * @return a <code>boolean</code> value
     * @see BuildableType
     */
    public boolean isBuildingMaterial() {
        return buildingMaterial;
    }

    /**
     * Set the <code>BuildingMaterial</code> value.
     *
     * @param newBuildingMaterial The new BuildingMaterial value.
     */
    public void setBuildingMaterial(final boolean newBuildingMaterial) {
        this.buildingMaterial = newBuildingMaterial;
    }

    /**
     * Returns the production chain of the goods type, beginning with
     * a raw material that can not be produced from any other. The
     * last element of the production chain is the goods type itself.
     *
     * @return  the production chain of the goods type
     */
    public List<GoodsType> getProductionChain() {
        List<GoodsType> result = new ArrayList<GoodsType>();
        GoodsType currentGoods = this;
        while (currentGoods != null) {
            result.add(0, currentGoods);
            currentGoods = currentGoods.madeFrom;
        }
        return result;
    }

    /**
     * Returns true if this type of goods is required for producing a
     * type of goods required for building a BuildableType.
     *
     * @return a <code>boolean</code> value
     * @see BuildableType
     */
    public boolean isRawBuildingMaterial() {
        if (this.madeFrom!=null) {
            return false;
        }
        GoodsType refinedType = makes;
        while (refinedType != null) {
            if (refinedType.isBuildingMaterial()) {
                return true;
            } else {
                refinedType = refinedType.makes;
            }
        }
        return false;
    }

    /**
     * Get the <code>MilitaryGoods</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isMilitaryGoods() {
        return militaryGoods;
    }

    /**
     * Set the <code>MilitaryGoods</code> value.
     *
     * @param newMilitaryGoods The new MilitaryGoods value.
     */
    public void setMilitaryGoods(final boolean newMilitaryGoods) {
        this.militaryGoods = newMilitaryGoods;
    }

    /**
     * Get the <code>TradeGoods</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isTradeGoods() {
        return tradeGoods;
    }

    /**
     * Set the <code>TradeGoods</code> value.
     *
     * @param newTradeGoods The new TradeGoods value.
     */
    public void setTradeGoods(final boolean newTradeGoods) {
        this.tradeGoods = newTradeGoods;
    }


    /**
     * Whether this type of goods produces liberty points.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isLibertyGoodsType() {
        return getFeatureContainer().containsModifierKey("model.modifier.liberty");
    }

    /**
     * Whether this type of goods causes immigration.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isImmigrationGoodsType() {
        return getFeatureContainer().containsModifierKey("model.modifier.liberty");
    }

    /**
     * Get the <code>BreedingNumber</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getBreedingNumber() {
        return breedingNumber;
    }

    /**
     * Set the <code>BreedingNumber</code> value.
     *
     * @param newBreedingNumber The new BreedingNumber value.
     */
    public void setBreedingNumber(final int newBreedingNumber) {
        this.breedingNumber = newBreedingNumber;
    }

    /**
     * Returns <code>true</code> if this type of Goods is
     * breedable. This should only be true for animals, such as
     * horses.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isBreedable() {
        return breedingNumber != NO_BREEDING;
    }

    /**
     * Get the <code>Price</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getPrice() {
        return price;
    }

    /**
     * Set the <code>Price</code> value.
     *
     * @param newPrice The new Price value.
     */
    public void setPrice(final int newPrice) {
        this.price = newPrice;
    }

    // ------------------------------------------------------------ API methods

    public void readAttributes(XMLStreamReader in, Specification specification) 
        throws XMLStreamException {
        isFarmed = getAttribute(in, "is-farmed", false);
        isFood = getAttribute(in, "is-food", false);
        ignoreLimit = getAttribute(in, "ignore-limit", false);
        newWorldGoods = getAttribute(in, "new-world-goods", false);
        breedingNumber = getAttribute(in, "breeding-number", NO_BREEDING);
        price = getAttribute(in, "price", NO_PRICE);

        madeFrom = specification.getType(in, "made-from", GoodsType.class, null);
        if (madeFrom != null) {
            madeFrom.makes = this;
        }

        storable = getAttribute(in, "storable", true);
        storedAs = specification.getType(in, "stored-as", GoodsType.class, null);
    }

    public void readChildren(XMLStreamReader in, Specification specification) 
        throws XMLStreamException {

        // Only expected child is 'market'
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if ("market".equals(in.getLocalName())) {
                initialAmount = Integer.parseInt(in.getAttributeValue(null, "initial-amount"));
                initialPrice = getAttribute(in, "initial-price", 1);
                priceDiff = getAttribute(in, "price-difference", 1);
                in.nextTag(); // close this element
            } else {
                super.readChild(in, specification);
            }
        }
    }

}
