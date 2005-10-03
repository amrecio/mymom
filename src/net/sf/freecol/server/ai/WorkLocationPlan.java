
package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.WorkLocation;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
* Objects of this class contains AI-information for a single {@link WorkLocation}.
*/
public class WorkLocationPlan {
    private static final Logger logger = Logger.getLogger(WorkLocationPlan.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private AIMain aiMain;
    

    /**
    * The FreeColGameObject this AIObject contains AI-information for.
    */
    private WorkLocation workLocation;
    private int priority;
    private int goodsType;


    public WorkLocationPlan(AIMain aiMain, WorkLocation workLocation, int goodsType) {
        this.aiMain = aiMain;
        this.workLocation = workLocation;
        this.goodsType = goodsType;
    }



    public WorkLocationPlan(AIMain aiMain, Element element) {
        this.aiMain = aiMain;
        readFromXMLElement(element);
    }


    public AIMain getAIMain() {
        return aiMain;
    }

    
    public Game getGame() {
        return aiMain.getGame();
    }
    

    /**
    * Gets the <code>WorkLocation</code> this <code>WorkLocationPlan</code> controls.
    */
    public WorkLocation getWorkLocation() {
        return workLocation;
    }

    
    /**
    * Gets the production of the given type of goods according to this
    * <code>WorkLocationPlan</code>. The plan has been created for either
    * a {@link ColonyTile} or a {@link Building}. If this is a plan for a
    * <code>ColonyTile</code> then the maximum possible production of the
    * tile gets returned, while the <code>Building</code>-plans only returns
    * a number used for identifying the value of the goods produced.
    *
    * @param goodsType The type of goods to get the production for.
    * @return The production.
    */
    public int getProductionOf(int goodsType) {
        if (goodsType != this.goodsType) {
            return 0;
        }
        
        if (workLocation instanceof ColonyTile) {
            if (!Goods.isFarmedGoods(goodsType)) {
                return 0;
            }

            ColonyTile ct = (ColonyTile) workLocation;
            Tile t = ct.getWorkTile();
            int expertUnitType = ct.getExpertForProducing(goodsType);

            int base = t.getMaximumPotential(goodsType);

            if (t.isLand() && base != 0) {
                base++;
            }

            return Unit.getProductionUsing(expertUnitType, goodsType, base, t) * ((goodsType == Goods.FURS) ? 2 : 1);
        } else {
            if (Goods.isFarmedGoods(goodsType)) {
                return 0;
            } else {
                /* These values are not really the production, but are
                   being used while sorting the WorkLocationPlans:
                */

                if (goodsType == Goods.HAMMERS) {
                    return 16;
                } else if (goodsType == Goods.BELLS) {
                    return 12;
                } else if (goodsType == Goods.CROSSES) {
                    return 10;
                } else {
                    return getGame().getMarket().getSalePrice(goodsType, 1);
                }
            }
        }
    }


    /**
    * Gets the type of goods which should be produced at the <code>WorkLocation</code>.
    *
    * @return The type of goods.
    * @see Goods
    * @see WorkLocation
    */
    public int getGoodsType() {
        return goodsType;
    }
    
    
    /**
    * Sets the type of goods to be produced at the <code>WorkLocation</code>.
    *
    * @param goodsType The type of goods.
    * @see Goods
    * @see WorkLocation
    */
    public void setGoodsType(int goodsType) {
        this.goodsType = goodsType;
    }

    
    /**
     * Creates an XML-representation of this object.
     * @param document The <code>Document</code> in which
     * 		the XML-representation should be created.
     * @return The XML-representation.
     */    
    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("ID", workLocation.getID());
        element.setAttribute("priority", Integer.toString(priority));
        element.setAttribute("goodsType", Integer.toString(goodsType));

        return element;
    }


    /**
     * Updates this object from an XML-representation of
     * a <code>WorkLocationPlan</code>.
     * 
     * @param element The XML-representation.
     */    
    public void readFromXMLElement(Element element) {
        workLocation = (WorkLocation) getAIMain().getFreeColGameObject(element.getAttribute("ID"));
        priority = Integer.parseInt(element.getAttribute("priority"));
        goodsType = Integer.parseInt(element.getAttribute("goodsType"));
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return "workLocationPlan"
    */
    public static String getXMLElementTagName() {
        return "workLocationPlan";
    }
}
