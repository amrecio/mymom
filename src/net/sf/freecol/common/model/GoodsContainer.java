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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.Specification;

import org.w3c.dom.Element;

/**
* Contains goods and can be used by a {@link Location} to make certain
* tasks easier.
*/
public class GoodsContainer extends FreeColGameObject {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(Location.class.getName());

    public static final int CARGO_SIZE = 100;
    public static final String STORED_GOODS_TAG = "storedGoods";
    public static final String OLD_STORED_GOODS_TAG = "oldStoredGoods";

    /** The list of Goods stored in this <code>GoodsContainer</code>. */
    private Map<GoodsType, Integer> storedGoods = new HashMap<GoodsType, Integer>();

    /** The previous list of Goods stored in this <code>GoodsContainer</code>. */
    private Map<GoodsType, Integer> oldStoredGoods = new HashMap<GoodsType, Integer>();

    /** The owner of this <code>GoodsContainer</code>. */
    private final Location parent;

    /**
     * Creates an empty <code>GoodsContainer</code>.
     *
     * @param game The <code>Game</code> in which this <code>GoodsContainer</code> belong.
     * @param parent The <code>Location</code> this <code>GoodsContainer</code> will be containg goods for.
     */
    public GoodsContainer(Game game, Location parent) {
        super(game);

        if (parent == null) {
            throw new IllegalArgumentException("Location of GoodsContainer must not be null!");
        }

        this.parent = parent;
    }

    /**
     * Initiates a new <code>GoodsContainer</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this <code>GoodsContainer</code>
     *       belong.
     * @param parent The object using this <code>GoodsContainer</code>
     *       for storing it's goods.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public GoodsContainer(Game game, Location parent, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        if (parent == null) {
            throw new IllegalArgumentException("Location of GoodsContainer must not be null!");
        }

        this.parent = parent;
        readFromXML(in);
    }

    /**
     * Initiates a new <code>GoodsContainer</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this <code>GoodsContainer</code>
     *       belong.
     * @param parent The object using this <code>GoodsContainer</code>
     *       for storing it's goods.
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public GoodsContainer(Game game, Location parent, Element e) {
        super(game, e);

        if (parent == null) {
            throw new IllegalArgumentException("Location of GoodsContainer must not be null!");
        }

        this.parent = parent;
        readFromXMLElement(e);
    }

    /**
     * Adds a <code>Goods</code> to this containter.
     * @param g The Goods to add to this container.
     */
    public void addGoods(AbstractGoods g) {
        addGoods(g.getType(), g.getAmount());
    }

    /**
     * Adds the given amount of the given type of goods.
     * @param type The type of goods to add.
     * @param amount The type of amount to add.
     */
    public void addGoods(GoodsType type, int amount) {
	int newAmount = amount;
	if (storedGoods.containsKey(type)) {
	    newAmount += storedGoods.get(type).intValue();
	}
	if (newAmount < 0) {
	    throw new IllegalStateException("Operation would leave " + (newAmount) + " goods of type " 
					    + type.getName() + " in Location " + parent);
	} else if (newAmount == 0) {
	    storedGoods.remove(type);
	} else {
	    storedGoods.put(type, newAmount);
	}
    }
    
    /**
     * Removes Goods from this containter.
     * @param g The Goods to remove from this container.
     */
    public Goods removeGoods(AbstractGoods g) {
        return removeGoods(g.getType(), g.getAmount());
    }

    public Goods removeGoods(GoodsType type) {
        return removeGoods(type, Integer.MAX_VALUE);
    }

    /**
     * Removes the given amount of the given type of goods.
     *
     * @param type The type of goods to remove.
     * @param amount The type of amount to remove.
     * @return A Goods with the requested or available amount that has been removed
     */
    public Goods removeGoods(GoodsType type, int amount) {
	Integer oldAmount = storedGoods.get(type);
	Goods removedGoods = null;
	if (oldAmount != null) {
	    if (oldAmount.intValue() < amount) {
		removedGoods = new Goods(getGame(), parent, type, oldAmount.intValue());
		storedGoods.remove(type);
	    } else {
		removedGoods = new Goods(getGame(), parent, type, amount);
		storedGoods.put(type, oldAmount.intValue() - amount);
	    }
	}
        return removedGoods;
    }

    /**
     * Removes all goods above given amount, provided that the goods
     * are storable and do not ignore warehouse limits.
     * 
     * @param amount The treshold.
     */
    public void removeAbove(int amount) {
        for (GoodsType goodsType : storedGoods.keySet()) {
            if (goodsType.isStorable() && !goodsType.limitIgnored() && 
                storedGoods.get(goodsType) > amount) {
		if (amount == 0) {
		    storedGoods.remove(goodsType);
		} else {
		    storedGoods.put(goodsType, amount);
		}
            }
        }
    }

    /**
     * Removes all goods.
     * 
     */
    public void removeAll() {
	storedGoods.clear();
    }

    
    /**
     * Checks if any storable type of goods has reached the given
     * amount.
     * 
     * @param amount The amount.
     * @return <code>true</code> if any type of goods, 
     * except for <code>Goods.FOOD</code>, has reached 
     * the given amount.
     */
    public boolean hasReachedCapacity(int amount) {
        for (GoodsType goodsType : storedGoods.keySet()) {
            if (goodsType.isStorable() && !goodsType.limitIgnored() && 
                storedGoods.get(goodsType) > amount) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the specified <code>Goods</code> is in this container.
     *
     * @param g The <code>Goods</code> to test the presence of.
     * @return The result.
     */
    public boolean contains(Goods g) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the amount of one type of Goods in this container.
     * @param type The type of Goods being looked for in this container.
     * @return The amount of this type of Goods in this container.
     */
    public int getGoodsCount(GoodsType type) {
	if (storedGoods.containsKey(type)) {
	    return storedGoods.get(type).intValue();
	} else {
	    return 0;
	}
    }

    public Goods getGoods(GoodsType goodsType) {
        return new Goods(getGame(), parent, goodsType, getGoodsCount(goodsType));
    }


    /**
    * Gets the number of goods-packages. A goods package contain between 1-CARGO_SIZE.
    * @return The number of goods packages.
    */
    public int getGoodsCount() {
        int count = 0;
        for (Integer amount : storedGoods.values()) {
	    if (amount % CARGO_SIZE == 0) {
		count += amount/CARGO_SIZE;
	    } else {
		count += amount/CARGO_SIZE + 1;
            }
        }
        return count;
    }


    /**
    * Gets an <code>Iterator</code> of every <code>Goods</code> in this
    * <code>GoodsContainer</code>. Each <code>Goods</code> have a maximum
    * amount of CARGO_SIZE.
    *
    * @return The <code>Iterator</code>.
    * @see #getCompactGoods
    */
    public Iterator<Goods> getGoodsIterator() {
        return getGoods().iterator();
    }

    /**
    * Returns an <code>ArrayList</code> containing all
    * <code>Goods</code> in this <code>GoodsContainer</code>. Each
    * <code>Goods</code> has a maximum amount of CARGO_SIZE.
    *
    * @return The <code>ArrayList</code>.
    * @see #getGoodsIterator
    */
    public List<Goods> getGoods() {
        ArrayList<Goods> totalGoods = new ArrayList<Goods>();

        for (GoodsType goodsType : storedGoods.keySet()) {
	    int amount = storedGoods.get(goodsType).intValue();
            while (amount > 0) {
		totalGoods.add(new Goods(getGame(), parent, goodsType, (amount >= CARGO_SIZE ? CARGO_SIZE : amount)));
		amount -= CARGO_SIZE;
            }
        }

        return totalGoods;
    }

    
    /**
    * Gets an <code>Iterator</code> of every <code>Goods</code> in this
    * <code>GoodsContainer</code>. There is only one <code>Goods</code>
    * for each type of goods.
    *
    * @return The <code>Iterator</code>.
    * @see #getGoodsIterator
    */
    public List<Goods> getCompactGoods() {
        ArrayList<Goods> totalGoods = new ArrayList<Goods>();

        for (GoodsType goodsType : storedGoods.keySet()) {
	    totalGoods.add(new Goods(getGame(), parent, goodsType, storedGoods.get(goodsType)));
        }

        return totalGoods;
    }

    /**
    * Gets an <code>Iterator</code> of every <code>Goods</code> in this
    * <code>GoodsContainer</code>. There is only one <code>Goods</code>
    * for each type of goods.
    *
    * @return The <code>Iterator</code>.
    * @see #getGoodsIterator
    */
    public List<Goods> getFullGoods() {
        ArrayList<Goods> totalGoods = new ArrayList<Goods>();

        for (GoodsType goodsType : storedGoods.keySet()) {
            totalGoods.add(new Goods(getGame(), parent, goodsType, storedGoods.get(goodsType)));
        }

        return totalGoods;
    }

    /**
     * Prepares this <code>GoodsContainer</code> for a new turn.
     */
    public void saveState() {
	oldStoredGoods.clear();
	for (Map.Entry<GoodsType, Integer> entry : storedGoods.entrySet()) {
	    oldStoredGoods.put(entry.getKey(), new Integer(entry.getValue().intValue()));
	}
    }

    /**
     * Removes goods exceeding limit and reports on goods exceeding levels.
     *
     */
    public void cleanAndReport() {
        if (!(parent instanceof Colony)) {
            return;
        }
        Colony colony = (Colony) parent;
        int limit = colony.getWarehouseCapacity();
        int adjustment = limit / CARGO_SIZE;

        for (GoodsType goodsType : storedGoods.keySet()) {
            if (goodsType.limitIgnored() || !goodsType.isStorable()) {
                continue;
            }
            ExportData exportData = colony.getExportData(goodsType);
            int low = exportData.getLowLevel() * adjustment;
            int high = exportData.getHighLevel() * adjustment;
	    int amount = storedGoods.get(goodsType).intValue();
	    int oldAmount = 0;
	    if (oldStoredGoods.containsKey(goodsType)) {
		oldAmount = oldStoredGoods.get(goodsType).intValue();
	    }
            if (amount > limit) {
                // limit has been exceeded
                int waste = amount - limit;
                storedGoods.put(goodsType, limit);
                addModelMessage(colony, ModelMessage.MessageType.WAREHOUSE_CAPACITY, goodsType,
                                "model.building.warehouseWaste",
                                "%goods%", goodsType.getName(),
                                "%waste%", String.valueOf(waste),
                                "%colony%", colony.getName());
            } else if (amount == limit && oldAmount < limit) {
                // limit has been reached during this turn
                addModelMessage(colony, ModelMessage.MessageType.WAREHOUSE_CAPACITY, goodsType,
                                "model.building.warehouseOverfull",
                                "%goods%", goodsType.getName(),
                                "%colony%", colony.getName());
            } else if (amount > high && oldAmount <= high) {
                addModelMessage(colony, ModelMessage.MessageType.WAREHOUSE_CAPACITY, goodsType,
                                "model.building.warehouseFull",
                                "%goods%", goodsType.getName(),
                                "%level%", String.valueOf(high),
                                "%colony%", colony.getName());
            } else if (amount < low && oldAmount >= low) {
                addModelMessage(colony, ModelMessage.MessageType.WAREHOUSE_CAPACITY, goodsType,
                                "model.building.warehouseEmpty",
                                "%goods%", goodsType.getName(),
                                "%level%", String.valueOf(low),
                                "%colony%", colony.getName());
            }
        }

    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * <br><br>
     * 
     * Only attributes visible to the given <code>Player</code> will 
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *  
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation 
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> 
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("ID", getId());
        if (!storedGoods.isEmpty()) {
            out.writeStartElement(STORED_GOODS_TAG);
            for (Map.Entry<GoodsType, Integer> entry : storedGoods.entrySet()) {
                out.writeStartElement(Goods.getXMLElementTagName());
                out.writeAttribute("type", entry.getKey().getId());
                out.writeAttribute("amount", entry.getValue().toString());
                out.writeEndElement();
            }
            out.writeEndElement();
        }
        if (!oldStoredGoods.isEmpty()) {
            out.writeStartElement(OLD_STORED_GOODS_TAG);
            for (Map.Entry<GoodsType, Integer> entry : oldStoredGoods.entrySet()) {
                out.writeStartElement(Goods.getXMLElementTagName());
                out.writeAttribute("type", entry.getKey().getId());
                out.writeAttribute("amount", entry.getValue().toString());
                out.writeEndElement();
            }
            out.writeEndElement();
        }
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
	    if (in.getLocalName().equals(STORED_GOODS_TAG)) {
		while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
		    if (in.getLocalName().equals(Goods.getXMLElementTagName())) {
			GoodsType goodsType = Specification.getSpecification().getGoodsType(in.getAttributeValue(null, "type"));
			Integer amount = new Integer(in.getAttributeValue(null, "amount"));
			storedGoods.put(goodsType, amount);
		    }
                    in.nextTag();
		}
	    } else if (in.getLocalName().equals(OLD_STORED_GOODS_TAG)) {
		while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
		    if (in.getLocalName().equals(Goods.getXMLElementTagName())) {
			GoodsType goodsType = Specification.getSpecification().getGoodsType(in.getAttributeValue(null, "type"));
			Integer amount = new Integer(in.getAttributeValue(null, "amount"));
			oldStoredGoods.put(goodsType, amount);
		    }
                    in.nextTag();
		}
	    }
	}
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "goodsContainer".
    */
    public static String getXMLElementTagName() {
        return "goodsContainer";
    }
    
    
    /**
    * Creates a <code>String</code> representation of this
    * <code>GoodsContainer</code>.    
    */
    public String toString() {
        StringBuffer sb = new StringBuffer(200);
        sb.append("GoodsContainer with: ");
        for (Map.Entry<GoodsType, Integer> entry : storedGoods.entrySet()) {
            sb.append(entry.getKey().getName() + "=" + entry.getValue() + ", ");
        }
        return sb.toString();
    }

}
