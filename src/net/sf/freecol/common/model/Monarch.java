
package net.sf.freecol.common.model;

import java.util.Random;

import net.sf.freecol.client.gui.i18n.Messages;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class implements the player's monarch, whose functions prior
 * to the revolution include raising taxes, declaring war on other
 * European countries, and occasionally providing military support.
 */
public final class Monarch extends FreeColGameObject {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** The name of this monarch. */
    private String name;

    /** The player of this monarch. */
    private Player player;

    /** Constants describing monarch actions. */
    public static final int NO_ACTION = 0,
        RAISE_TAX = 1,
        ADD_TO_REF = 2,
        DECLARE_WAR = 3,
        SUPPORT_SEA = 4,
        SUPPORT_LAND = 5,
        OFFER_MERCENARIES = 6,
        NUMBER_OF_ACTIONS = 7,
        WAIVE_TAX = 8,
        ADD_UNITS = 9;

    /** Constants describing the REF. */
    public static final int INFANTRY = 0,
        DRAGOON = 1,
        ARTILLERY = 2,
        MAN_OF_WAR = 3,
        NUMBER_OF_TYPES = 4;

    /** The minimum price for mercenaries. */
    public static final int MINIMUM_PRICE = 100;

    /** The number of units in the REF. */
    private int[] ref = new int[NUMBER_OF_TYPES];       

    /** Whether a frigate has been provided. */
    // Setting this to true here disables the action completely.
    private boolean supportSea = true;
    
    public static final Random random = new Random();

    /** 
     * Constructor. 
     *
     * @param game The <code>Game</code> this <code>Monarch</code>
     *      should be created in.
     * @param player The <code>Player</code> to create the
     *      <code>Monarch</code> for.
     * @param name The name of the <code>Monarch</code>.
     */
    public Monarch(Game game, Player player, String name) {
        super(game);
        this.player = player;
        this.name = name;
        int dx = player.getDifficulty();
        ref[INFANTRY] = dx * 3 + 5;
        ref[DRAGOON] = dx * 2 + 3;
        ref[ARTILLERY] = dx + 3;
        ref[MAN_OF_WAR] = dx + 1;
    }


    /**
     * Initiates a new <code>Monarch</code> from an <code>Element</code>
     * and registers this <code>Monarch</code> at the specified game.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param element The <code>Element</code> in a DOM-parsed XML-tree that describes
     *                this object.
     */
    public Monarch(Game game, Element element) {
        super(game, element);

        readFromXMLElement(element);
    }

    /**
     * Returns a monarch action. Not all actions are always
     * applicable, and their probability depends on the player's
     * difficulty settings. This method can only be called by the
     * server.
     *
     * @return A monarch action.
     */
    public int getAction() {
        /**
         * Random numbers can be generated by this method since it is only
         * invoked by the endTurn method of the server's InGameController.
         */
        int dx = player.getDifficulty() + 1; // 1-5
        int turn = getGame().getTurn().getNumber();
        int grace = (6 - dx) * 10; // 10-50

        // nothing happens during the first few turns, nor after the
        // revolution
        if (turn < grace || player.getRebellionState() != Player.REBELLION_PRE_WAR) {
            return NO_ACTION;
        }

        boolean canDeclareWar = false;
        boolean atWar = false;
        // Benjamin Franklin puts an end to the monarch's interference
        if (!player.hasFather(FoundingFather.BENJAMIN_FRANKLIN)) {
            for (int i = 0; i < Player.NUMBER_OF_NATIONS; i++) {
                if (!getGame().getPlayer(i).isEuropean() || getGame().getPlayer(i).isREF()) {
                    continue;
                }
                if (player.hasContacted(i)) {
                    switch (player.getStance(i)) {
                    case Player.WAR:
                        atWar = true;
                        break;
                    case Player.PEACE:
                    case Player.CEASE_FIRE:
                        canDeclareWar = true;
                        break;
                    }
                }
            }
        }

        /** The probabilities of these actions. */
        int[] probability = new int[NUMBER_OF_ACTIONS];
    

	for (int j = 0; j < NUMBER_OF_ACTIONS; j++ ) {
            probability[j] = 0;
	}

        // the more time has passed, the less likely the monarch will
        // do nothing
        probability[NO_ACTION] = Math.max(200 - turn, 100);

        if (player.getTax() < 100) {
            probability[RAISE_TAX] = 10 + dx;
        }
        
        probability[ADD_TO_REF] = 10 + dx;

        if (canDeclareWar) {
            probability[DECLARE_WAR] = 5 + dx;
        }

        // provide no more than one frigate
        if (player.hasBeenAttackedByPrivateers() && !supportSea) {
            probability[SUPPORT_SEA] = 6 - dx;
        }
        
        if (atWar) {
            // disable for the moment
            //probability[SUPPORT_LAND] = 6 - dx;
            if (player.getGold() > MINIMUM_PRICE) {
                probability[OFFER_MERCENARIES] = 6 - dx;
            }
        }
        
        int accumulator = 0;
        for (int k = 0; k < NUMBER_OF_ACTIONS; k++ ) {
            accumulator += probability[k];
            probability[k] = accumulator;
        }
        
        int randomInt = random.nextInt(accumulator);
        
        for (int action = 0; action < NUMBER_OF_ACTIONS; action++) {
            if (randomInt < probability[action]) {
                return action;
            }
        }

        return NO_ACTION;
    }

    public int[] getREF() {
        return ref;        
    }
    
    public void clearREF() {
        for (int i=0; i<ref.length; i++) {
            ref[i] = 0;
        }
    }
    
    /**
     * Returns the new increased tax, but never more than 100 percent.
     *
     * @return The increased tax.
     */
    public int getNewTax() {
        int turn = getGame().getTurn().getNumber();
        int adjustment = (6 - player.getDifficulty()) * 10; // 20-60
        // later in the game, the taxes will increase by more
        int increase = random.nextInt(5 + turn/adjustment) + 1;
        int newTax = player.getTax() + increase;
        return Math.min(newTax, 100);            
    }


    /**
     * Returns units available as mercenaries.
     * 
     * @return A troop of mercenaries.    
     */
    public int[] getMercenaries() {
        int[] units = new int[ARTILLERY + 1];
        int gold = player.getGold();
        int price = 0;
        for (int i = 0; i < 6; i++) {
            int type = random.nextInt(NUMBER_OF_TYPES);
            if (type > ARTILLERY) {
                break;
            }
            int newPrice = getPrice(type);
            if (price + newPrice <= gold) {
                units[type]++;
                price += newPrice;
            } else {
                break;
            }
        }

        if (price == 0) {
            units[INFANTRY] = 1;
        }

        return units;
    }        
    

    /**
     * Returns units to be added to the Royal Expeditionary Force.
     *
     * @return An addition to the Royal Expeditionary Force.
     */
    public int[] addToREF() {
        int[] units = new int[NUMBER_OF_TYPES];
        if (ref[INFANTRY] + ref[DRAGOON] + ref[ARTILLERY] >
            ref[MAN_OF_WAR] * 6) {
            units[MAN_OF_WAR] = 1;
        } else {        
            int number = random.nextInt(3) + 1;
            int type = random.nextInt(3);
            
            units[type] = number;
            ref[type] += number;
        }
        return units;
    }

    /**
     * Adds units to the Royal Expeditionary Force.
     *
     * @param units The addition to the Royal Expeditionary Force.
     */
     public void addToREF(int[] units) {
         for (int type = 0; type < units.length; type++) {
             ref[type] += units[type];
         }
     }

    public static String getName(int type) {
        return getName(type, 1);
    }

    public static String getName(int type, int number) {
        String name = "INVALID";
        switch (type) {
        case INFANTRY:
            if (number == 1) {
                name = Messages.message("model.monarch.infantry");
            } else {
                name = Messages.message("model.monarch.infantries");
            }
            break;
        case DRAGOON:
            if (number == 1) {
                name = Messages.message("model.monarch.dragoon");
            } else {
                name = Messages.message("model.monarch.dragoons");
            }
            break;
        case ARTILLERY:
            if (number == 1) {
                name = Messages.message("model.monarch.artillery");
            } else {
                name = Messages.message("model.monarch.artilleries");
            }
            break;
        case MAN_OF_WAR:
            if (number == 1) {
                name = Messages.message("model.monarch.manofwar");
            } else {
                name = Messages.message("model.monarch.menofwar");
            }
            break;
        }
        return String.valueOf(number) + " " + name;
    }


    public String getName(int[] units) {
        String name = null;
        for (int type = 0; type < units.length; type++) {
            if (units[type] > 0) {
                if (name == null) {
                    name = getName(type, units[type]);
                } else {
                    name = name + " " + Messages.message("and") +
                        " " + getName(type, units[type]);
                }
            }
        }
        return name;
    }
    

    public int getPrice(int type) {
        int dx = player.getDifficulty();
        switch (type) {
        case INFANTRY:
            return 300 + dx * 25;
        case DRAGOON:
            return 450 + dx * 25;
        case ARTILLERY:
            return 600 + dx * 25;
        case MAN_OF_WAR:
        default:
            return 1000000;
        }
    }

    /**
     * Returns the price for the given units.
     *
     * @param units The units to get a price for.
     * @param rebate Whether to grant a rebate.
     * @return The price fo the units.
     */
    public int getPrice(int[] units, boolean rebate) {
        int price = 0;
        for (int type = 0; type < units.length; type++) {
            price += units[type] * getPrice(type);
        }
        if (price > player.getGold() && rebate) {
            return player.getGold();
        } else {
            return price;
        }
    }

     /**
     * Returns the nation of another player to declare war on.
     *
     * @return The enemy nation.
     */
    public int declareWar() {
        int offset = random.nextInt(Player.NUMBER_OF_NATIONS);
        for (int i = 0; i < Player.NUMBER_OF_NATIONS; i++) {
            int nation = (i + offset) % Player.NUMBER_OF_NATIONS;
            if (nation == player.getNation()) {
                continue;
            } else if (!player.hasContacted(nation)) {
                continue;
            } else if (!getGame().getPlayer(i).isEuropean() || getGame().getPlayer(i).isREF()) {
                continue;
            }
            int stance = player.getStance(nation);
            if (stance == Player.PEACE ||
                stance == Player.CEASE_FIRE) {
                player.setStance(nation, Player.WAR);
                return nation;
            }
        }
        return Player.NO_NATION;
    }

    /**
     * Returns an addition to the colonial forces.
     *
     * @return An addition to the colonial forces.
     */     
    public int[] supportLand() {
        int[] units = new int[NUMBER_OF_TYPES];
        switch (player.getDifficulty()) {
        case Player.VERY_EASY:
            units[ARTILLERY] = 1;
            units[DRAGOON] = 2;
            break;
        case Player.EASY:
            units[DRAGOON] = 2;
            units[INFANTRY] = 1;
            break;
        case Player.MEDIUM:
            units[DRAGOON] = 2;
            break;
        case Player.HARD:
            units[DRAGOON] = 1;
            units[INFANTRY] = 1;
            break;
        case Player.VERY_HARD:
            units[INFANTRY] = 1;
            break;
        }
        return units;
    }
            

    
    /**
     * Prepares the object for a new turn.
     */
    public void newTurn() {
    }
    
    /**
     * This method should return an XML-representation of this object.
     * Only attributes visible to <code>player</code> will be added to
     * that representation if <code>showAll</code> is set to <i>false</i>.
     *
     * @param xmlPlayer The <code>Player</code> this XML-representation is
     *         made for.
     * @param document The document to use when creating new components.
     * @param showAll Only attributes visible to <code>player</code> will be added to
     *         the representation if <code>showAll</code> is set to <i>false</i>.
     * @param toSavedGame If <i>true</i> then information that is only needed when saving a
     *         game is added.
     * @return The DOM-element ("Document Object Model").
     */    
    public Element toXMLElement(Player xmlPlayer, Document document, boolean showAll, boolean toSavedGame) {
        Element monarchElement = document.createElement(getXMLElementTagName());
        monarchElement.setAttribute("ID", getID());
        monarchElement.setAttribute("player", player.getID());
        monarchElement.setAttribute("name", name);
        monarchElement.setAttribute("supportSea", String.valueOf(supportSea));
        monarchElement.appendChild(toArrayElement("ref", ref, document));
        return monarchElement;
    }


    /**
     * Initialize this object from an XML-representation of this object.
     * @param monarchElement The DOM-element ("Document Object Model") 
     *      made to represent this object.
     */
    public void readFromXMLElement(Element monarchElement) {
        setID(monarchElement.getAttribute("ID"));
        player = (Player) getGame().getFreeColGameObject(monarchElement.getAttribute("player"));
        name = monarchElement.getAttribute("name");
        supportSea = Boolean.valueOf(monarchElement.getAttribute("supportSea")).booleanValue();
        if (getChildElement(monarchElement, "ref") != null) {
            ref = readFromArrayElement("ref", getChildElement(monarchElement, "ref"), new int[0]);
        } else {
            ref = new int[NUMBER_OF_TYPES];
        }

    }


    /**
     * Gets the tag name of the root element representing this object.
     * This method should be overwritten by any sub-class, preferably
     * with the name of the class with the first letter in lower case.
     *
     * @return "monarch".
     */
    public static String getXMLElementTagName() {
        return "monarch";
    }


}

