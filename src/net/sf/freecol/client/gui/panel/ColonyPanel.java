
package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.logging.Logger;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;

//import net.sf.freecol.client.model.ClientGame;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.FreeColException;
//import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;


/**
 * This is a panel for the Colony display. It shows the units that are working in the
 * colony, the buildings and much more.
 */
public final class ColonyPanel extends JLayeredPane implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(ColonyPanel.class.getName());

    private static final int    EXIT = 0;

    private final Canvas  parent;
    private final FreeColClient freeColClient;
    private InGameController inGameController;

    private final JLabel                    cargoLabel;
    private final JLabel                    goldLabel;
    private final JLabel                    warehouseLabel;
    private final OutsideColonyPanel        outsideColonyPanel;
    private final InPortPanel               inPortPanel;
    private final CargoPanel                cargoPanel;
    private final WarehousePanel            warehousePanel;
    private final TilePanel                 tilePanel;
    private final BuildingsPanel            buildingsPanel;
    private final DefaultTransferHandler    defaultTransferHandler;
    private final MouseListener             pressListener;
    private final MouseListener             releaseListener;

    private Colony      colony;
    private Game        game;
    private UnitLabel   selectedUnit;






    /**
     * The constructor for the panel.
     * @param parent The parent of this panel
     */
    public ColonyPanel(Canvas parent, FreeColClient freeColClient) {
        this.parent = parent;
        this.freeColClient = freeColClient;
        this.inGameController = freeColClient.getInGameController();

        outsideColonyPanel = new OutsideColonyPanel(this);
        inPortPanel = new InPortPanel();
        cargoPanel = new CargoPanel(this);
        warehousePanel = new WarehousePanel(this);
        tilePanel = new TilePanel(this);
        buildingsPanel = new BuildingsPanel(this);

        outsideColonyPanel.setBackground(Color.WHITE);
        inPortPanel.setBackground(Color.WHITE);
        cargoPanel.setBackground(Color.WHITE);
        warehousePanel.setBackground(Color.WHITE);
        buildingsPanel.setBackground(Color.WHITE);

        defaultTransferHandler = new DefaultTransferHandler(this);
        outsideColonyPanel.setTransferHandler(defaultTransferHandler);
        inPortPanel.setTransferHandler(defaultTransferHandler);
        cargoPanel.setTransferHandler(defaultTransferHandler);
        warehousePanel.setTransferHandler(defaultTransferHandler);

        pressListener = new DragListener(this);
        releaseListener = new DropListener();
        outsideColonyPanel.addMouseListener(releaseListener);
        inPortPanel.addMouseListener(releaseListener);
        cargoPanel.addMouseListener(releaseListener);
        warehousePanel.addMouseListener(releaseListener);

        outsideColonyPanel.setLayout(new GridLayout(0 , 2));
        inPortPanel.setLayout(new GridLayout(0 , 2));
        cargoPanel.setLayout(new GridLayout(1 , 0));
	warehousePanel.setLayout(new GridLayout(1 , 0));

        cargoLabel = new JLabel("<html><strike>Cargo</strike></html>");
        goldLabel = new JLabel("Gold: 0");
	warehouseLabel = new JLabel("Goods");

        JButton exitButton = new JButton("Close");
        JScrollPane outsideColonyScroll = new JScrollPane(outsideColonyPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
                    inPortScroll = new JScrollPane(inPortPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
                    cargoScroll = new JScrollPane(cargoPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                    warehouseScroll = new JScrollPane(warehousePanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                    tilesScroll = new JScrollPane(tilePanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                    buildingsScroll = new JScrollPane(buildingsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JLabel  outsideColonyLabel = new JLabel("In front of colony"),
                inPortLabel = new JLabel("In port"),
                tilesLabel = new JLabel("Tiles"),
                buildingsLabel = new JLabel("Buildings");

        exitButton.setSize(80, 20);
        outsideColonyScroll.setSize(200, 100);
        inPortScroll.setSize(200, 100);
        cargoScroll.setSize(410, 96);
	warehouseScroll.setSize(620, 120);
        tilesScroll.setSize(390, 200);
        buildingsScroll.setSize(400,200);
        outsideColonyLabel.setSize(200, 20);
        inPortLabel.setSize(200, 20);
        cargoLabel.setSize(410, 20);
        goldLabel.setSize(100, 20);
	warehouseLabel.setSize(100, 20);
        tilesLabel.setSize(100, 20);
        buildingsLabel.setSize(300, 20);

        exitButton.setLocation(760, 570);
        outsideColonyScroll.setLocation(640, 300);
        inPortScroll.setLocation(640, 450);
        cargoScroll.setLocation(220, 370);
	warehouseScroll.setLocation(10, 470);
        tilesScroll.setLocation(10, 40);
        buildingsScroll.setLocation(400, 40);
        outsideColonyLabel.setLocation(640, 275);
        inPortLabel.setLocation(640, 425);
        cargoLabel.setLocation(220, 345);
	warehouseLabel.setLocation(10, 445);
        goldLabel.setLocation(15, 345);
        tilesLabel.setLocation(10, 10);
        buildingsLabel.setLocation(400, 10);

        setLayout(null);

        exitButton.setActionCommand(String.valueOf(EXIT));

        exitButton.addActionListener(this);

        add(exitButton);
        add(outsideColonyScroll);
        add(inPortScroll);
        add(cargoScroll);
	add(warehouseScroll);
        add(tilesScroll);
        add(buildingsScroll);
        add(outsideColonyLabel);
        add(inPortLabel);
        add(cargoLabel);
	add(warehouseLabel);
        add(goldLabel);
        add(tilesLabel);
        add(buildingsLabel);

        try {
            BevelBorder border = new BevelBorder(BevelBorder.RAISED);
            setBorder(border);
        } catch(Exception e) {}

        setSize(850, 600);

        selectedUnit = null;
    }


    
    

    /**
    * Refreshes this panel.
    */
    public void refresh() {
        repaint(0, 0, getWidth(), getHeight());
    }


    /**
     * Initialize the data on the window.
     */
    public void initialize(Colony colony, Game game) {
        this.colony = colony;
        this.game = game;

        //
        // Remove the old components from the panels.
        //

        cargoPanel.removeAll();
	warehousePanel.removeAll();
        outsideColonyPanel.removeAll();
        inPortPanel.removeAll();
        tilePanel.removeAll();


        //
        // Units outside the colony:
        //

        Tile tile = colony.getTile();

        UnitLabel lastCarrier = null;

        Iterator tileUnitIterator = tile.getUnitIterator();
        while (tileUnitIterator.hasNext()) {
            Unit unit = (Unit) tileUnitIterator.next();

            UnitLabel unitLabel = new UnitLabel(unit, parent);
            unitLabel.setTransferHandler(defaultTransferHandler);
            unitLabel.addMouseListener(pressListener);

            //if (((unit.getState() == Unit.ACTIVE) || (unit.getState() == Unit.SENTRY)) && (!unit.isNaval())) {
            if (!unit.isNaval() && !unit.isType(Unit.WAGON_TRAIN)) {
                outsideColonyPanel.add(unitLabel, false);
            } else {
                inPortPanel.add(unitLabel);
                lastCarrier = unitLabel;
            }
        }
        
        setSelectedUnit(lastCarrier);

        //
        // Warehouse panel:
        //

        Iterator goodsIterator = colony.getGoodsIterator();
        while (goodsIterator.hasNext()) {
            Goods goods = (Goods) goodsIterator.next();

            GoodsLabel goodsLabel = new GoodsLabel(goods, parent);
            goodsLabel.setTransferHandler(defaultTransferHandler);
            goodsLabel.addMouseListener(pressListener);

            warehousePanel.add(goodsLabel, false);
        }

        warehousePanel.revalidate();
        
        setSelectedUnit(lastCarrier);

        //
        // Units in buildings:
        //

        buildingsPanel.initialize();

        //
        // TilePanel:
        //

        tilePanel.initialize();

        goldLabel.setText("Gold: " + freeColClient.getMyPlayer().getGold());
    }


    /**
    * Updates the label that is placed above the cargo panel. It shows the name
    * of the unit whose cargo is displayed and the amount of space left on that unit.
    */
    private void updateCargoLabel() {
        if (selectedUnit != null) {
            try {
                cargoLabel.setText("Cargo (" + selectedUnit.getUnit().getName() + ") space left: " + selectedUnit.getUnit().getSpaceLeft());
            }
            catch (FreeColException e) {
                e.printStackTrace();
                cargoLabel.setText("Cargo");
            }
        }
        else {
            cargoLabel.setText("<html><strike>Cargo</strike></html>");
        }
    }


    /**
    * Returns the currently select unit.
    * @return The currently select unit.
    */
    public Unit getSelectedUnit() {
        return selectedUnit.getUnit();
    }


    /**
     * Analyzes an event and calls the right external methods to take
     * care of the user's request.
     * @param event The incoming action event
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Integer.valueOf(command).intValue()) {
                case EXIT:
                    parent.remove(this);
                    parent.showMapControls();
                    break;
                default:
                    logger.warning("Invalid action");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid action number");
        }
    }


    /**
    * Paints this component.
    * @param g The graphics context in which to paint.
    */
    public void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
    }



    /**
    * Selects a unit that is located somewhere on this panel.
    *
    * @param unit The unit that is being selected.
    */
    public void setSelectedUnit(UnitLabel unitLabel) {
        if (selectedUnit != unitLabel) {
            if (selectedUnit != null) {
                selectedUnit.setSelected(false);
            }
            cargoPanel.removeAll();
            selectedUnit = unitLabel;

            if (selectedUnit != null) {
                selectedUnit.setSelected(true);
                Unit selUnit = selectedUnit.getUnit();

                Iterator unitIterator = selUnit.getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit unit = (Unit) unitIterator.next();

                    UnitLabel label = new UnitLabel(unit, parent);
                    label.setTransferHandler(defaultTransferHandler);
                    label.addMouseListener(pressListener);

                    cargoPanel.add(label, false);
                }

                Iterator goodsIterator = selUnit.getGoodsIterator();
                while (goodsIterator.hasNext()) {
                    Goods g = (Goods) goodsIterator.next();

                    GoodsLabel label = new GoodsLabel(g, parent);
                    label.setTransferHandler(defaultTransferHandler);
                    label.addMouseListener(pressListener);

                    cargoPanel.add(label, false);
                }

            }

            updateCargoLabel();
        }
        cargoPanel.revalidate();
        refresh();
    }


    /**
    * Returns a pointer to the <code>cargoPanel</code>-object in use.
    */
    public final CargoPanel getCargoPanel() {
        return cargoPanel;
    }

    /**
    * Returns a pointer to the <code>warehousePanel</code>-object in use.
    */
    public final WarehousePanel getWarehousePanel() {
        return warehousePanel;
    }
    
    /**
    * Returns a pointer to the <code>tilePanel</code>-object in use.
    */
    public final TilePanel getTilePanel() {
        return tilePanel;
    }


    /**
    * This panel is a list of the colony's buildings.
    */
    public final class BuildingsPanel extends JPanel {
        private final ColonyPanel colonyPanel;

        /**
        * Creates this BuildingsPanel.
        * @param colonyPanel The panel that holds this BuildingsPanel.
        */
        public BuildingsPanel(ColonyPanel colonyPanel) {
            this.colonyPanel = colonyPanel;
        }


        /**
        * Initializes the <code>BuildingsPanel</code> by loading/displaying the buildings of the colony.
        */
        public void initialize() {
            removeAll();
            setLayout(new GridLayout(Building.NUMBER_OF_TYPES, 1));

            //Building[] buildings = colony.getBuildings();

            int displayedBuildings = 0;
            ASingleBuildingPanel aSingleBuildingPanel;

            Iterator buildingIterator = colony.getBuildingIterator();
            while (buildingIterator.hasNext()) {
                Building building = (Building) buildingIterator.next();
                if (building.isBuilt()) {
                    displayedBuildings++;
                    aSingleBuildingPanel = new ASingleBuildingPanel(building);
                    aSingleBuildingPanel.addMouseListener(releaseListener);
                    aSingleBuildingPanel.setTransferHandler(defaultTransferHandler);
                    aSingleBuildingPanel.setOpaque(false);                    
                    add(aSingleBuildingPanel);
                }
            }
        }


        /**
        * This panel is a single line (one building) in the <code>BuildingsPanel</code>.
        */
        public final class ASingleBuildingPanel extends JPanel {
            Building building;


            /**
            * Creates this ASingleBuildingPanel.
            * @param building The building to display information from.
            */
            public ASingleBuildingPanel(Building building) {
                this.building = building;

                removeAll();
                setBackground(Color.WHITE);
                setLayout(new GridLayout(1, 2));

                JPanel colonistsInBuildingPanel = new JPanel();
                colonistsInBuildingPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
                colonistsInBuildingPanel.setBackground(Color.WHITE);

                add(new JLabel(building.getName()));

                Iterator unitIterator = building.getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit unit = (Unit) unitIterator.next();
                    UnitLabel unitLabel = new UnitLabel(unit, parent, true);
                    unitLabel.setTransferHandler(defaultTransferHandler);
                    unitLabel.addMouseListener(pressListener);
                    colonistsInBuildingPanel.add(unitLabel);
                }

                colonistsInBuildingPanel.setOpaque(false);
                add(colonistsInBuildingPanel);
            }


            /**
            * Adds a component to this ASingleBuildingPanel and makes sure that the unit
            * that the component represents gets modified so that it will be located
            * in the colony.
            * @param comp The component to add to this ColonistsPanel.
            * @param editState Must be set to 'true' if the state of the component
            * that is added (which should be a dropped component representing a Unit)
            * should be changed so that the underlying unit will be located in the colony.
            * @return The component argument.
            */
            public Component add(Component comp, boolean editState) {
                Component c;

                if (editState) {
                    if (comp instanceof UnitLabel) {
                        Unit unit = ((UnitLabel) comp).getUnit();
                        inGameController.work(unit, building);
                    } else {
                        logger.warning("An invalid component got dropped on this BuildingsPanel.");
                    }
                }

                ((UnitLabel) comp).setSmall(true);
                c = ((JPanel) getComponent(1)).add(comp);
                refresh();
                return c;
            }
        }
    }



    /**
    * A panel that holds UnitsLabels that represent Units that are
    * standing in front of a colony.
    */
    public final class OutsideColonyPanel extends JPanel {
        private final ColonyPanel colonyPanel;

        /**
        * Creates this OutsideColonyPanel.
        * @param colonyPanel The panel that holds this OutsideColonyPanel.
        */
        public OutsideColonyPanel(ColonyPanel colonyPanel) {
            this.colonyPanel = colonyPanel;
        }

        /**
        * Adds a component to this OutsideColonyPanel and makes sure that the unit
        * that the component represents gets modified so that it will be located
        * in the colony.
        * @param comp The component to add to this ColonistsPanel.
        * @param editState Must be set to 'true' if the state of the component
        * that is added (which should be a dropped component representing a Unit)
        * should be changed so that the underlying unit will be located in the colony.
        * @return The component argument.
        */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (comp instanceof UnitLabel) {
                    UnitLabel unitLabel = ((UnitLabel) comp);
                    Unit unit = unitLabel.getUnit();
                    inGameController.putOutsideColony(unit);
                } else {
                    logger.warning("An invalid component got dropped on this ColonistsPanel.");
                }
            }

            ((UnitLabel) comp).setSmall(false);
            updateCargoLabel();
            Component c = add(comp);
            refresh();
            return c;
        }
    }



    /**
    * A panel that holds UnitsLabels that represent naval Units that are
    * waiting in the port of the colony.
    */
    public final class InPortPanel extends JPanel {
        /**
        * Adds a component to this InPortPanel.
        * @param comp The component to add to this InPortPanel.
        * @return The component argument.
        */
        public Component add(Component comp) {
            return super.add(comp);
        }
    }



    /**
    * A panel that holds goods that represent cargo that is inside the
    * Colony.
    */
    public final class WarehousePanel extends JPanel {
        private final ColonyPanel colonyPanel;

        /**
        * Creates this CargoPanel.
        * @param colonyPanel The panel that holds this CargoPanel.
        */
        public WarehousePanel(ColonyPanel colonyPanel) {
            this.colonyPanel = colonyPanel;
        }

        /**
        * Adds a component to this CargoPanel and makes sure that the unit
        * or good that the component represents gets modified so that it is
        * on board the currently selected ship.
        * @param comp The component to add to this CargoPanel.
        * @param editState Must be set to 'true' if the state of the component
        * that is added (which should be a dropped component representing a Unit or
        * good) should be changed so that the underlying unit or goods are
        * on board the currently selected ship.
        * @return The component argument.
        */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (comp instanceof GoodsLabel) {
                    Goods g = ((GoodsLabel)comp).getGoods();
                    ((GoodsLabel) comp).setSmall(false);
                    //inGameController.unloadCargo(g, selectedUnit.getUnit());
                    colonyPanel.getWarehousePanel().revalidate();
                    colonyPanel.getCargoPanel().revalidate();
                } else {
                    logger.warning("An invalid component got dropped on this WarehousePanel.");
                }
            }

            Component c = add(comp);

            refresh();
            return c;
        }

        public void remove(Component comp) {
            if (comp instanceof GoodsLabel) {
                Goods g = ((GoodsLabel)comp).getGoods();
                //inGameController.leaveShip(unit);

                super.remove(comp);
                    colonyPanel.getWarehousePanel().revalidate();
                    colonyPanel.getCargoPanel().revalidate();
            }
        }
    }

    /**
    * A panel that holds units and goods that represent Units and cargo that are
    * on board the currently selected ship.
    */
    public final class CargoPanel extends JPanel {
        private final ColonyPanel colonyPanel;

        /**
        * Creates this CargoPanel.
        * @param colonyPanel The panel that holds this CargoPanel.
        */
        public CargoPanel(ColonyPanel colonyPanel) {
            this.colonyPanel = colonyPanel;
        }

        /**
        * Adds a component to this CargoPanel and makes sure that the unit
        * or good that the component represents gets modified so that it is
        * on board the currently selected ship.
        * @param comp The component to add to this CargoPanel.
        * @param editState Must be set to 'true' if the state of the component
        * that is added (which should be a dropped component representing a Unit or
        * good) should be changed so that the underlying unit or goods are
        * on board the currently selected ship.
        * @return The component argument.
        */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (comp instanceof UnitLabel) {
                    Unit unit = ((UnitLabel)comp).getUnit();
                    if (!unit.isCarrier()) // No, you cannot load ships onto other ships.
                    {
                      ((UnitLabel) comp).setSmall(false);
                      inGameController.boardShip(unit, selectedUnit.getUnit());
                    } else {
                      return comp;
                    }
                } else if (comp instanceof GoodsLabel) {
                    Goods g = ((GoodsLabel)comp).getGoods();
                    ((GoodsLabel) comp).setSmall(false);
		    logger.warning("Attempting to load cargo.");
                    inGameController.loadCargo(g, selectedUnit.getUnit());
                    colonyPanel.getWarehousePanel().revalidate();
                    colonyPanel.getCargoPanel().revalidate();
                } else {
                    logger.warning("An invalid component got dropped on this CargoPanel.");
                }
            }
            updateCargoLabel();
            Component c = add(comp);

            refresh();
            return c;
        }

        public void remove(Component comp) {
            if (comp instanceof UnitLabel) {
                Unit unit = ((UnitLabel)comp).getUnit();
                inGameController.leaveShip(unit);

                super.remove(comp);
            } else if (comp instanceof GoodsLabel) {
                Goods g = ((GoodsLabel)comp).getGoods();
                inGameController.unloadCargo(g);
                super.remove(comp);
                colonyPanel.getWarehousePanel().revalidate();
                colonyPanel.getCargoPanel().revalidate();
            }
        }
    }


    /**
    * A panel that displays the tiles in the immediate area around the colony.
    */
    public final class TilePanel extends JLayeredPane {
        private final ColonyPanel colonyPanel;

        /**
        * Creates this TilePanel.
        * @param colonyPanel The panel that holds this TilePanel.
        */
        public TilePanel(ColonyPanel colonyPanel) {
            this.colonyPanel = colonyPanel;
            setBackground(Color.BLACK);
            //setOpaque(false);
            setLayout(null);
        }


        public void initialize() {
            GUI gui = ((Canvas) parent).getGUI();
            int layer = 1;

            for (int x=0; x<3; x++) {
                for (int y=0; y<3; y++) {
                    ASingleTilePanel p = new ASingleTilePanel(colony.getColonyTile(x, y), x, y);
                    add(p, new Integer(layer));
                    layer++;
                }
            }
        }


        public void paintComponent(Graphics g) {
            g.setColor(Color.black);
            g.fillRect(0, 0, getWidth(), getHeight());
        }



        public final class ASingleTilePanel extends JPanel {

            private ColonyTile colonyTile;
            private int x;
            private int y;
	    private JLabel staticGoodsLabel;

            public ASingleTilePanel(ColonyTile colonyTile, int x, int y) {
                this.colonyTile = colonyTile;
                this.x = x;
                this.y = y;
                
                setOpaque(false);

                if (colonyTile.getUnit() != null) {
                    Unit unit = colonyTile.getUnit();

                    UnitLabel unitLabel = new UnitLabel(unit, parent);
                    unitLabel.setTransferHandler(defaultTransferHandler);
                    unitLabel.addMouseListener(pressListener);

                    add(unitLabel);

                    staticGoodsLabel = new JLabel(parent.getImageProvider().getGoodsImageIcon(unit.getWorkType()));
                    staticGoodsLabel.setText(Integer.toString(unit.getFarmedPotential(unit.getWorkType(), colonyTile.getWorkTile())));
                    add(staticGoodsLabel);
                }
		
		if (colonyTile.isColonyCenterTile())
		{
                    staticGoodsLabel = new JLabel(parent.getImageProvider().getGoodsImageIcon(Goods.FOOD));
                    staticGoodsLabel.setText(Integer.toString(colonyTile.getTile().potential(Goods.FOOD)));
                    add(staticGoodsLabel);
                    staticGoodsLabel = new JLabel(parent.getImageProvider().getGoodsImageIcon(colonyTile.getTile().secondaryGoods()));
                    staticGoodsLabel.setText(Integer.toString(colonyTile.getTile().potential(colonyTile.getTile().secondaryGoods())));
                    add(staticGoodsLabel);
		}

                setTransferHandler(defaultTransferHandler);
                addMouseListener(releaseListener);

                // Size and position:
                ImageLibrary lib = ((Canvas)parent).getGUI().getImageLibrary();
                setSize(lib.getTerrainImageWidth(1), lib.getTerrainImageHeight(1));
                setLocation(((2-x)+y)*lib.getTerrainImageWidth(1)/2, (x+y)*lib.getTerrainImageHeight(1)/2);
            }


            public void paintComponent(Graphics g) {
                GUI gui = parent.getGUI();
                ImageLibrary lib = parent.getGUI().getImageLibrary();

                gui.displayTile((Graphics2D) g, game.getMap(), colony.getTile(x, y), 0, 0);
            }


            /**
            * Adds a component to this CargoPanel and makes sure that the unit
            * or good that the component represents gets modified so that it is
            * on board the currently selected ship.
            * @param comp The component to add to this CargoPanel.
            * @param editState Must be set to 'true' if the state of the component
            * that is added (which should be a dropped component representing a Unit or
            * good) should be changed so that the underlying unit or goods are
            * on board the currently selected ship.
            * @return The component argument.
            */
            public Component add(Component comp, boolean editState) {
                if (editState) {
                    if (comp instanceof UnitLabel) {
                        Unit unit = ((UnitLabel)comp).getUnit();
                        inGameController.work(unit, colonyTile);

                        ((UnitLabel) comp).setSmall(false);
                        
                        if (staticGoodsLabel != null) {
                            super.remove(staticGoodsLabel);
		            staticGoodsLabel = null;
                        }
                        staticGoodsLabel = new JLabel(parent.getImageProvider().getGoodsImageIcon(unit.getWorkType()));
                        staticGoodsLabel.setText(Integer.toString(unit.getFarmedPotential(unit.getWorkType(), colonyTile.getWorkTile())));
                        add(staticGoodsLabel);
                    } else {
                        logger.warning("An invalid component got dropped on this CargoPanel.");
                    }
                }

                updateCargoLabel();
                Component c = add(comp);
                refresh();
                return c;
            }
	    
            public void remove(Component comp) {
                if (comp instanceof UnitLabel) {
                    super.remove(staticGoodsLabel);
		    staticGoodsLabel = null;
                }
                super.remove(comp);
            }
        }

    }



}
