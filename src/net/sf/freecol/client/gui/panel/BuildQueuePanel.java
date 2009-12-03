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

package net.sf.freecol.client.gui.panel;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.resources.ResourceManager;

import net.miginfocom.swing.MigLayout;


public class BuildQueuePanel extends FreeColPanel implements ActionListener, ItemListener {

    private static Logger logger = Logger.getLogger(BuildQueuePanel.class.getName());

    private static final String BUY = "buy";

    private final BuildQueueTransferHandler buildQueueHandler = new BuildQueueTransferHandler();

    private static ListCellRenderer cellRenderer;
    private static JCheckBox compact = new JCheckBox();
    private static JCheckBox showAll = new JCheckBox();
    private JList buildQueueList;
    private JList unitList;
    private JList buildingList;
    private JButton buyBuilding;
    private Colony colony; 
    private int unitCount;

    private FeatureContainer featureContainer = new FeatureContainer();

    private Set<BuildableType> lockedTypes = new HashSet<BuildableType>();
    private Set<BuildableType> unbuildableTypes = new HashSet<BuildableType>();

    private BuildQueuePanel buildQueuePanel;

    public BuildQueuePanel(Colony colony, Canvas parent) {

        super(parent, new MigLayout("wrap 3", "[260:][260:][260:]", "[][][300:400:][]"));
        buildQueuePanel = this;
        this.colony = colony;
        this.unitCount = colony.getUnitCount();

        DefaultListModel current = new DefaultListModel();
        for (BuildableType type : colony.getBuildQueue()) {
            current.addElement(type);
            featureContainer.add(type.getFeatureContainer());
        }

        cellRenderer = new DefaultBuildQueueCellRenderer();

        compact.setText(Messages.message("colonyPanel.compactView"));
        compact.addItemListener(this);

        showAll.setText(Messages.message("colonyPanel.showAll"));
        showAll.addItemListener(this);

        buildQueueList = new JList(current);
        buildQueueList.setTransferHandler(buildQueueHandler);
        buildQueueList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        buildQueueList.setDragEnabled(true);
        buildQueueList.setCellRenderer(cellRenderer);
        buildQueueList.addMouseListener(new BuildQueueMouseAdapter(false));

        Action deleteAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    DefaultListModel model = (DefaultListModel) buildQueueList.getModel();
                    for (Object type : buildQueueList.getSelectedValues()) {
                        model.removeElement(type);
                    }
                    updateAllLists();
                }
            };

        buildQueueList.getInputMap().put(KeyStroke.getKeyStroke("DELETE"), "delete");
        buildQueueList.getActionMap().put("delete", deleteAction);

        Action addAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    DefaultListModel model = (DefaultListModel) buildQueueList.getModel();
                    for (Object type : ((JList) e.getSource()).getSelectedValues()) {
                        model.addElement(type);
                    }
                    updateAllLists();
                }
            };

        BuildQueueMouseAdapter adapter = new BuildQueueMouseAdapter(true);
        DefaultListModel units = new DefaultListModel();
        unitList = new JList(units);
        unitList.setTransferHandler(buildQueueHandler);
        unitList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        unitList.setDragEnabled(true);
        unitList.setCellRenderer(cellRenderer);
        unitList.addMouseListener(adapter);

        unitList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "add");
        unitList.getActionMap().put("add", addAction);

        DefaultListModel buildings = new DefaultListModel();
        buildingList = new JList(buildings);
        buildingList.setTransferHandler(buildQueueHandler);
        buildingList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        buildingList.setDragEnabled(true);
        buildingList.setCellRenderer(cellRenderer);
        buildingList.addMouseListener(adapter);

        buildingList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "add");
        buildingList.getActionMap().put("add", addAction);

        JLabel headLine = new JLabel(Messages.message("colonyPanel.buildQueue"));
        headLine.setFont(bigHeaderFont);

        buyBuilding = new JButton(Messages.message("colonyPanel.buyBuilding"));
        buyBuilding.setActionCommand(BUY);
        buyBuilding.addActionListener(this);

        updateAllLists();

        add(headLine, "span 3, align center, wrap 40");
        add(new JLabel(Messages.message("menuBar.colopedia.unit")), "align center");
        add(new JLabel(Messages.message("colonyPanel.buildQueue")), "align center");
        add(new JLabel(Messages.message("menuBar.colopedia.building")), "align center");
        add(new JScrollPane(unitList), "grow");
        add(new JScrollPane(buildQueueList), "grow");
        add(new JScrollPane(buildingList), "grow, wrap 20");
        add(buyBuilding, "span, split 4");
        add(compact);
        add(showAll);
        add(okButton, "tag ok");
    }

    private void updateUnitList() {
        DefaultListModel units = (DefaultListModel) unitList.getModel();
        units.clear();
        loop: for (UnitType unitType : FreeCol.getSpecification().getUnitTypeList()) {
            // compare colony.getNoBuildReason()
            boolean locked = false;
            if (unbuildableTypes.contains(unitType)) {
                continue;
            } else if (unitType.getGoodsRequired().isEmpty()) {
                // can't be built
                unbuildableTypes.add(unitType);
                continue;
            } else if (unitType.getPopulationRequired() > unitCount) {
                locked = true;
            } else if (!(colony.getFeatureContainer()
                         .hasAbility("model.ability.build", unitType, getGame().getTurn())
                         || featureContainer.hasAbility("model.ability.build", unitType))) {
                for (Ability ability : FreeCol.getSpecification().getAbilities("model.ability.build")) {
                    if (ability.appliesTo(unitType)
                        && ability.getValue()
                        && ability.getSource() != null
                        && !unbuildableTypes.contains(ability.getSource())) {
                        locked = true;
                        break;
                    }
                }
                if (!locked) {
                    unbuildableTypes.add(unitType);
                    continue;
                }
            } else {
                Map<String, Boolean> requiredAbilities = unitType.getAbilitiesRequired();
                for (Entry<String, Boolean> entry : requiredAbilities.entrySet()) {
                    if (colony.hasAbility(entry.getKey()) != entry.getValue()
                        && featureContainer.hasAbility(entry.getKey()) != entry.getValue()) {
                        if (FreeCol.getSpecification()
                            .getTypesProviding(entry.getKey(), entry.getValue()).isEmpty()) {
                            // no type provides the required ability
                            unbuildableTypes.add(unitType);
                            continue loop;
                        } else {
                            locked = true;
                        }
                    }
                }
            }
            if (locked) {
                lockedTypes.add(unitType);
            } else {
                lockedTypes.remove(unitType);
            }
            if (!locked || showAll.isSelected()) {
                units.addElement(unitType);
            }
        }
    }

    private void updateBuildingList() {
        DefaultListModel buildings = (DefaultListModel) buildingList.getModel();
        DefaultListModel current = (DefaultListModel) buildQueueList.getModel();
        buildings.clear();
        loop: for (BuildingType buildingType : FreeCol.getSpecification().getBuildingTypeList()) {
            // compare colony.getNoBuildReason()
            boolean locked = false;
            Building colonyBuilding = colony.getBuilding(buildingType);
            if (current.contains(buildingType)
                || (colonyBuilding != null
                    && colonyBuilding.getType() == buildingType)) {
                // only one building of any kind
                continue;
            } else if (unbuildableTypes.contains(buildingType)) {
                continue;
            } else if (buildingType.getGoodsRequired().isEmpty()) {
                // pre-built
                continue;
            } else if (unbuildableTypes.contains(buildingType.getUpgradesFrom())) {
                // impossible upgrade path
                unbuildableTypes.add(buildingType);
                continue;
            } else if (buildingType.getPopulationRequired() > unitCount) {
                locked = true;
            } else {
                Map<String, Boolean> requiredAbilities = buildingType.getAbilitiesRequired();
                for (Entry<String, Boolean> entry : requiredAbilities.entrySet()) {
                    if (colony.hasAbility(entry.getKey()) != entry.getValue()
                        && featureContainer.hasAbility(entry.getKey()) != entry.getValue()) {
                        if (FreeCol.getSpecification()
                            .getTypesProviding(entry.getKey(), entry.getValue()).isEmpty()) {
                            // no type provides the required ability
                            unbuildableTypes.add(buildingType);
                            continue loop;
                        } else {
                            locked = true;
                        }
                    }
                }
            }
            if (!locked
                && buildingType.getUpgradesFrom() != null
                && !current.contains(buildingType.getUpgradesFrom())) {
                if (colonyBuilding == null
                    || colonyBuilding.getType() != buildingType.getUpgradesFrom()) {
                    locked = true;
                }
            }
            if (locked) {
                lockedTypes.add(buildingType);
            } else {
                lockedTypes.remove(buildingType);
            }
            if (!locked || showAll.isSelected()) {
                buildings.addElement(buildingType);
            }
        }
    }

    private void updateAllLists() {
        DefaultListModel current = (DefaultListModel) buildQueueList.getModel();
        featureContainer = new FeatureContainer();
        for (Object type: current.toArray()) {
            if (getMinimumIndex((BuildableType) type) >= 0) {
                featureContainer.add(((BuildableType) type).getFeatureContainer());
            } else {
                current.removeElement(type);
            }
        }
        // ATTENTION: buildings must be updated first, since units
        // might depend on the build ability of an unbuildable
        // building
        updateBuildingList();
        updateUnitList();
        updateBuyBuildingButton();
    }

    private void updateBuyBuildingButton() {
        DefaultListModel current = (DefaultListModel) buildQueueList.getModel();
        if (current.getSize() == 0) {
            buyBuilding.setEnabled(false);
        } else {
            buyBuilding.setEnabled(colony.canPayToFinishBuilding((BuildableType) current.getElementAt(0)));
        }
    }

    private boolean hasBuildingType(Colony colony, BuildingType buildingType) {
        if (colony.getBuilding(buildingType) == null) {
            return false;
        } else if (colony.getBuilding(buildingType).getType() == buildingType) {
            return true;
        } else if (buildingType.getUpgradesTo() != null) {
            return hasBuildingType(colony, buildingType.getUpgradesTo());
        } else {
            return false;
        }
    }

    private List<BuildableType> getBuildableTypes(JList list) {
        List<BuildableType> result = new ArrayList<BuildableType>();
        if (list != null) {
            ListModel model = list.getModel();
            for (int index = 0; index < model.getSize(); index++) {
                Object object = model.getElementAt(index);
                if (object instanceof BuildableType) {
                    result.add((BuildableType) object);
                }
            }
        }
        return result;
    }

    private List<BuildableType> getBuildableTypes(Object[] objects) {
        List<BuildableType> result = new ArrayList<BuildableType>();
        if (objects != null) {
            for (Object object : objects) {
                if (object instanceof BuildableType) {
                    result.add((BuildableType) object);
                }
            }
        }
        return result;
    }

    private int getMinimumIndex(BuildableType buildableType) {
        ListModel buildQueue = buildQueueList.getModel();
        if (buildableType instanceof UnitType) {
            if (colony.canBuild(buildableType)) {
                return 0;
            } else {
                for (int index = 0; index < buildQueue.getSize(); index++) {
                    if (((BuildableType) buildQueue.getElementAt(index))
                        .hasAbility("model.ability.build", buildableType)) {
                        return index + 1;
                    }
                }
            }
        } else if (buildableType instanceof BuildingType) {
            BuildingType upgradesFrom = ((BuildingType) buildableType).getUpgradesFrom();
            if (upgradesFrom == null) {
                return 0;
            } else {
                Building building = colony.getBuilding((BuildingType) buildableType);
                BuildingType buildingType = (building == null) ? null : building.getType();
                if (buildingType == upgradesFrom) {
                    return 0;
                } else {
                    for (int index = 0; index < buildQueue.getSize(); index++) {
                        if (upgradesFrom.equals(buildQueue.getElementAt(index))) {
                            return index + 1;
                        }
                    }
                }
            }
        }
        return -1;
    }            


    /**
     * This function analyses an event and calls the right methods to take
     * care of the user's requests.
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        if (colony.getOwner() == getMyPlayer()) {
            String command = event.getActionCommand();
            List<BuildableType> buildables = getBuildableTypes(buildQueueList);
            if (!buildables.isEmpty() && lockedTypes.contains(buildables.get(0))) {
                getCanvas().showInformationMessage("colonyPanel.unbuildable",
                                                   buildables.get(0),
                                                   "%colony%", colony.getName(),
                                                   "%object%", buildables.get(0).getName());
                return;
            }
            getController().setBuildQueue(colony, buildables);
            if (OK.equals(command)) {
                // do nothing?
            } else if (BUY.equals(command)) {
                getController().payForBuilding(colony);
                getCanvas().updateGoldLabel();
            } else {
                logger.warning("Unsupported command " + command);
            }
        }
        getCanvas().remove(this);
    }

    public void itemStateChanged(ItemEvent event) {
        if (event.getSource() == compact) {
            if (compact.isSelected()) {
                if (cellRenderer instanceof DefaultBuildQueueCellRenderer) {
                    cellRenderer = new SimpleBuildQueueCellRenderer();
                }
            } else if (cellRenderer instanceof SimpleBuildQueueCellRenderer) {
                cellRenderer = new DefaultBuildQueueCellRenderer();
            }
            buildQueueList.setCellRenderer(cellRenderer);
            buildingList.setCellRenderer(cellRenderer);
            unitList.setCellRenderer(cellRenderer);
        } else if (event.getSource() == showAll) {
            updateAllLists();
        }
    }

    /**
     * This class implements a transfer handler able to transfer
     * <code>BuildQueueItem</code>s between the build queue list, the
     * unit list and the building list.
     */
    public class BuildQueueTransferHandler extends TransferHandler {

        private final DataFlavor buildQueueFlavor = new DataFlavor(List.class, "BuildingQueueFlavor");
        
        JList source = null;
        int[] indices = null;
        int targetIndex = -1;  // preferred index of target list
        int numberOfItems = 0;  // number of items to be added

        /**
         * Imports a build queue into the build queue list, the
         * building list or the unit list, if possible.
         * @param comp The list which imports data.
         * @param data The build queue to import.
         * @return Whether the import was successful.
         */
        public boolean importData(JComponent comp, Transferable data) {

            if (!canImport(comp, data.getTransferDataFlavors())) {
                return false;
            }

            JList target = null;
            List<BuildableType> buildQueue = new ArrayList<BuildableType>();
            DefaultListModel targetModel;

            try {
                target = (JList) comp;
                targetModel = (DefaultListModel) target.getModel();
                Object transferData = data.getTransferData(buildQueueFlavor);
                if (transferData instanceof List) {
                    for (Object object : (List) transferData) {
                        if (object instanceof BuildableType) {
                            if ((object instanceof BuildingType
                                 && target == unitList)
                                || (object instanceof UnitType
                                    && target == buildingList)) {
                                return false;
                            }
                            buildQueue.add((BuildableType) object);
                        }
                    }
                }                    
            } catch (Exception e) {
                logger.warning(e.toString());
                return false;
            }

            for (BuildableType type : buildQueue) {
                if (getMinimumIndex(type) < 0) {
                    return false;
                }
            }

            int preferredIndex = target.getSelectedIndex();

            if (source.equals(target)) {
                if (target == buildQueueList) {
                    // don't drop selection on itself
                    if (indices != null && 
                        preferredIndex >= indices[0] - 1 &&
                        preferredIndex <= indices[indices.length - 1]) {
                        indices = null;
                        return true;
                    }
                    numberOfItems = buildQueue.size();
                } else {
                    return false;
                }
            }

            int maxIndex = targetModel.size();
            if (preferredIndex < 0 || preferredIndex > maxIndex) {
                preferredIndex = maxIndex;
            }
            targetIndex = preferredIndex;

            for (int index = 0; index < buildQueue.size(); index++) {
                int minimumIndex = getMinimumIndex(buildQueue.get(index));
                if (minimumIndex < targetIndex + index) {
                    minimumIndex = targetIndex + index;
                }
                targetModel.insertElementAt(buildQueue.get(index), minimumIndex);
            }

            return true;
        }

        /**
         * Cleans up after a successful import.
         * @param source The component that has exported data.
         * @param data The data exported.
         * @param action The transfer action, e.g. MOVE.
         */
        protected void exportDone(JComponent source, Transferable data, int action) {

            if ((action == MOVE) && (indices != null)) {
                DefaultListModel model = (DefaultListModel) ((JList) source).getModel();

                // adjust indices if necessary
                if (numberOfItems > 0) {
                    for (int i = 0; i < indices.length; i++) {
                        if (indices[i] > targetIndex) {
                            indices[i] += numberOfItems;
                        }
                    }
                }
                // has to be done backwards
                for (int i = indices.length -1; i >= 0; i--) {
                    model.remove(indices[i]);
                }
            }
            // clean up
            indices = null;
            targetIndex = -1;
            numberOfItems = 0;
            updateAllLists();
        }

        /**
         * Returns <code>true</code> if the component can import this
         * data flavor.
         * @param comp The component to import data.
         * @param flavors An array of data flavors.
         */
        public boolean canImport(JComponent comp, DataFlavor[] flavors) {
            if (flavors == null) {
                return false;
            } else {
                for (DataFlavor flavor : flavors) {
                    if (flavor.equals(buildQueueFlavor)) {
                        return true;
                    }
                }
                return false;
            }
        }

        /**
         * Returns a <code>Transferable</code> suitable for wrapping
         * the build queue.
         * @param comp The source of the build queue.
         * @return A Transferable suitable for wrapping the build
         * queue. 
         */
        protected Transferable createTransferable(JComponent comp) {
            if (comp instanceof JList) {
                source = (JList) comp;
                indices = source.getSelectedIndices();
                List<BuildableType> buildQueue = getBuildableTypes(source.getSelectedValues());
                return new BuildQueueTransferable(buildQueue);
            } else {
                return null;
            }
        }

        /**
         * Returns the possible source actions of the component.
         * @param comp The source component.
         * @return The possible source actions of the component.
         */
        public int getSourceActions(JComponent comp) {
            if (comp == unitList) {
                return COPY;
            } else {
                return MOVE;
            }
        }

        /**
         * This class implements the <code>Transferable</code> interface.
         */
        public class BuildQueueTransferable implements Transferable {
            private List<BuildableType> buildQueue;
            private final DataFlavor[] supportedFlavors = new DataFlavor[] {
                buildQueueFlavor
            };

            /**
             * Default constructor.
             * @param buildQueue The build queue to transfer.
             */
            public BuildQueueTransferable(List<BuildableType> buildQueue) {
                this.buildQueue = buildQueue;
            }

            /**
             * Returns the build queue from the <code>Transferable</code>.
             * @param flavor The data flavor to use.
             * @return The build queue from the <code>Transferable</code>.
             */
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (isDataFlavorSupported(flavor)) {
                    return buildQueue;
                } else {
                    throw new UnsupportedFlavorException(flavor);
                }
            }

            /**
             * Returns the build queue from the <code>Transferable</code>.
             * @param flavor The data flavor to use.
             * @return The build queue from the <code>Transferable</code>.
             */
            public List<BuildableType> getBuildQueue() {
                return buildQueue;
            }

            /**
             * Returns an array of supported data flavors.
             * @return An array of supported data flavors.
             */
            public DataFlavor[] getTransferDataFlavors() {
                return supportedFlavors;
            }

            /**
             * Returns <code>true</code> if this data flavor is supported.
             * @param flavor The data flavor.
             * @return Whether this data flavor is supported.
             */
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                for (DataFlavor myFlavor : supportedFlavors) {
                    if (myFlavor.equals(flavor)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    /**
     * See FreeColComboBoxRenderer.
     */
    class SelectedPanel extends JPanel {

        public void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            Composite oldComposite = g2d.getComposite();
            Color oldColor = g2d.getColor();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setComposite(oldComposite);
            g2d.setColor(oldColor);
            
            super.paintComponent(g);
        }
    }

    class SimpleBuildQueueCellRenderer extends FreeColComboBoxRenderer {

        public void setLabelValues(JLabel c, Object value) {
            c.setText(((BuildableType) value).getName());
        }

    }

    class DefaultBuildQueueCellRenderer implements ListCellRenderer {

        JPanel itemPanel = new JPanel();
        JPanel selectedPanel = new SelectedPanel();
        JLabel imageLabel = new JLabel();
        JLabel nameLabel = new JLabel();
        JLabel lockLabel =
            new JLabel(new ImageIcon(ResourceManager.getImage("lock.image", 0.5)));

        Map<FreeColGameObjectType, ImageIcon> imageCache
            = new HashMap<FreeColGameObjectType, ImageIcon>();

        public DefaultBuildQueueCellRenderer() {
            itemPanel.setOpaque(false);
            itemPanel.setLayout(new MigLayout("", "", ""));
            selectedPanel.setOpaque(false);
            selectedPanel.setLayout(new MigLayout("", "", ""));
        }

        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            BuildableType item = (BuildableType) value;
            JPanel panel = isSelected ? selectedPanel : itemPanel;
            panel.removeAll();

            ImageIcon buildableIcon = imageCache.get(value);
            if (buildableIcon == null) {
                Image buildableImage = ResourceManager.getImage(item.getId() + ".image");
                if (buildableImage != null) {
                    buildableImage = buildableImage.getScaledInstance(-1, 48, Image.SCALE_SMOOTH);
                    buildableIcon = new ImageIcon(buildableImage);
                    imageCache.put(item, buildableIcon);
                }
            }
            imageLabel.setIcon(buildableIcon);

            nameLabel.setText(item.getName());
            panel.add(imageLabel, "span 1 2");
            if (lockedTypes.contains(item)) {
                panel.add(nameLabel, "split 2");
                panel.add(lockLabel, "wrap");
            } else {
                panel.add(nameLabel, "wrap");
            }

            List<AbstractGoods> goodsRequired = item.getGoodsRequired();
            int size = goodsRequired.size();
            for (int i = 0; i < size; i++) {
                AbstractGoods goods = goodsRequired.get(i);
                ImageIcon goodsIcon = imageCache.get(goods.getType());
                if (goodsIcon == null) {
                    goodsIcon = getLibrary().getScaledGoodsImageIcon(goods.getType(), 0.66f);
                    imageCache.put(goods.getType(), goodsIcon);
                }
                JLabel goodsLabel = new JLabel(Integer.toString(goods.getAmount()), 
                                               goodsIcon,
                                               SwingConstants.CENTER);
                if (i == 0 && size > 1) {
                    panel.add(goodsLabel, "split " + size);
                } else {
                    panel.add(goodsLabel);
                }
            }
            return panel;
        }
    }

    class BuildQueueMouseAdapter extends MouseAdapter {

        private boolean add = true;

        public BuildQueueMouseAdapter(boolean add) {
            this.add = add;
        }

        public void mousePressed(MouseEvent e) {
            JList source = (JList) e.getSource();
            if ((e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger())) {
                int index = source.locationToIndex(e.getPoint());
                BuildableType type = (BuildableType) source.getModel().getElementAt(index);
                if (type instanceof BuildingType) {
                    getCanvas().showColopediaPanel(ColopediaPanel.PanelType.BUILDINGS, type);
                } else if (type instanceof UnitType) {
                    getCanvas().showColopediaPanel(ColopediaPanel.PanelType.UNITS, type);
                }
            } else if ((e.getClickCount() > 1 && !e.isConsumed())) {
                DefaultListModel model = (DefaultListModel) buildQueueList.getModel();
                if (source.getSelectedIndex() == -1) {
                    source.setSelectedIndex(source.locationToIndex(e.getPoint()));
                }
                for (Object type : source.getSelectedValues()) {
                    if (add) {
                        model.addElement(type);
                    } else {
                        model.removeElement(type);
                    }
                }
                updateAllLists();
            }
        }
    }

}

