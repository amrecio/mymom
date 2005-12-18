
package net.sf.freecol.common.model;

import java.util.Random;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Player;

/**
 * This class implements the player's monarch, whose functions prior
 * to the revolution include raising taxes, declaring war on other
 * European countries, and occasionally providing military support.
 */
public final class Monarch extends FreeColGameObject {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
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
        NUMBER_OF_ACTIONS = 6;

    /** Constants describing the REF. */
    public static final int INFANTRY = 0,
        DRAGOON = 1,
        ARTILLERY = 2,
        NUMBER_OF_TYPES = 3;

    /* The number of units in the REF. */
    private int[] ref = new int[NUMBER_OF_TYPES];       
    
    /** The probabilities of these actions. */
    private final int probability[] = new int[NUMBER_OF_ACTIONS];
    
    public static final Random random = new Random();

    /** Constructor. */
    public Monarch(Game game, Player player, String name) {
        super(game);
        this.player = player;
        this.name = name;
    }


    /**
     * Initiates a new <code>Player</code> from an <code>Element</code>
     * and registers this <code>Player</code> at the specified game.
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
     * difficulty settings.
     *
     * @return A monarch action.
     */
    public int getAction() {
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
        for (int i = 0; i < Player.NUMBER_OF_NATIONS; i++) {
            if (player.hasContacted(i)) {
                switch (player.getStance(i)) {
                case Player.WAR:
                    atWar = true;
                    break;
                case Player.PEACE:
                case Player.CEASE_FIRE:
                    canDeclareWar = true;
                }
            }
        }

        // TODO: check whether the player has been attacked by privateers
        boolean privateers = true;

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
        
        if (privateers) {
            probability[SUPPORT_SEA] = 7 - dx;
        }
        
        if (atWar) {
            probability[SUPPORT_LAND] = 7 - dx;
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
     * Returns units to be added to the Royal Expeditionary Force.
     *
     * @return An addition to the Royal Expeditionary Force.
     */
    public Addition addToREF() {
        int number = random.nextInt(3) + 1;
        int type = random.nextInt(3);

        ref[type] += number;

        return new Addition(type, number);
    }

    /**
     * Adds units to the Royal Expeditionary Force.
     *
     * @param addition The addition to the Royal Expeditionary Force.
     */
    public void addToREF(Addition addition) {
        ref[addition.type] += addition.number;
    }

    /** An addition to the  Royal Expeditionary Force. */
    public static final class Addition {
        public final int type, number;

        public Addition(int type, int number) {
            this.type = type;
            this.number = number;
        }

        public String getName() {
            String name = "INVALID";
            switch (type) {
            case INFANTRY:
                if (number == 1) {
                    name = Messages.message("model.monarch.infantry");
                } else {
                    name = Messages.message("model.monarch.infantries");
                }
            case DRAGOON:
                if (number == 1) {
                    name = Messages.message("model.monarch.dragoon");
                } else {
                    name = Messages.message("model.monarch.dragoons");
                }
            case ARTILLERY:
                if (number == 1) {
                    name = Messages.message("model.monarch.artillery");
                } else {
                    name = Messages.message("model.monarch.artilleries");
                }
            }
            return String.valueOf(number) + " " + name;
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
            int index = (i + offset) % Player.NUMBER_OF_NATIONS;
            if (index == player.getNation()) {
                continue;
            } else if (!player.hasContacted(index)) {
                continue;
            }
            int stance = player.getStance(index);
            if (stance == Player.PEACE ||
                stance == Player.CEASE_FIRE) {
                player.setStance(index, Player.WAR);
                return index;
            }
        }
        return Player.NO_NATION;
    }

    /**
     * Returns an addition to the colonial forces.
     *
     * @return An addition to the colonial forces.
     */     
    public Addition[] supportLand() {
        switch (player.getDifficulty()) {
        case Player.VERY_EASY:
            return new Addition[] {new Addition(ARTILLERY, 1),
                                   new Addition(DRAGOON, 1),
                                   new Addition(DRAGOON, 1)};
        case Player.EASY:
            return new Addition[] {new Addition(DRAGOON, 1),
                                   new Addition(DRAGOON, 1),
                                   new Addition(INFANTRY, 1)};
        case Player.MEDIUM:
            return new Addition[] {new Addition(DRAGOON, 1),
                                   new Addition(DRAGOON, 1)};
        case Player.HARD:
            return new Addition[] {new Addition(DRAGOON, 1),
                                   new Addition(INFANTRY, 1)};
        case Player.VERY_HARD:
            return new Addition[] {new Addition(INFANTRY, 1)};
        default:
            return new Addition[0];
        }
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
     * @param player The <code>Player</code> this XML-representation is
     *               made for.
     * @param document The document to use when creating new components.
     * @param showAll Only attributes visible to <code>player</code> will be added to
     *                the representation if <code>showAll</code> is set to <i>false</i>.
     * @param toSavedGame If <i>true</i> then information that is only needed when saving a
     *                    game is added.
     * @return The DOM-element ("Document Object Model").
     */    
    public Element toXMLElement(Player player, Document document, boolean showAll, boolean toSavedGame) {
        Element monarchElement = document.createElement(getXMLElementTagName());
        monarchElement.setAttribute("ID", getID());
        monarchElement.setAttribute("player", player.getID());
        monarchElement.setAttribute("name", name);
        monarchElement.appendChild(toArrayElement("ref", ref, document));
        return monarchElement;
    }


    /**
     * Initialize this object from an XML-representation of this object.
     * @param element The DOM-element ("Document Object Model") made to represent this object.
     */
    public void readFromXMLElement(Element monarchElement) {
        setID(monarchElement.getAttribute("ID"));
        player = (Player) getGame().getFreeColGameObject(monarchElement.getAttribute("player"));
        name = monarchElement.getAttribute("name");
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

