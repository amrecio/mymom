/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

package net.sf.freecol.client.gui.option;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;


/**
 * This panel displays an OptionGroup using a JTree.
 */
public final class OptionGroupUI extends JPanel
    implements OptionUpdater, TreeSelectionListener {

    private static final Logger logger = Logger.getLogger(OptionGroupUI.class.getName());

    private final List<OptionUpdater> optionUpdaters = new ArrayList<OptionUpdater>();

    private final HashMap<String, OptionUI> optionUIs = new HashMap<String, OptionUI>();


    private JPanel detailPanel;

    private JTree tree;

    private GUI gui;

    private OptionGroup group;

    private boolean editable;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param gui
     * @param group 
     * @param editable 
     */
    public OptionGroupUI(GUI gui, OptionGroup group, boolean editable) {
        this.gui = gui;
        this.group = group;
        this.editable = editable;

        setLayout(new MigLayout("fill", "[200:]unrelated[550:, grow, fill]", "[top]"));

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(group);
        buildTree(group, root);

        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel) {
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(200, super.getPreferredSize().height);
                }
                @Override
                public String convertValueToText(Object value, boolean selected, boolean expanded,
                                                 boolean leaf, int row, boolean hasFocus) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                    Option option = (Option) node.getUserObject();
                    return Messages.message(option.getId() + ".name");
                }
            };

        tree.setOpaque(false);
        tree.addTreeSelectionListener(this);
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree.getCellRenderer();
        renderer.setBackgroundNonSelectionColor(new Color(0,0,0,1));

        add(tree);
        detailPanel = new JPanel(new MigLayout("wrap 2", "[fill]related[fill]"));
        detailPanel.setOpaque(false);
        add(detailPanel, "grow");

    }

    public JTree getTree() {
        return tree;
    }

    /**
     * Builds the JTree which represents the navigation menu and then returns it
     *
     */
    private void buildTree(OptionGroup group, DefaultMutableTreeNode parent) {

        for (Option option : group.getOptions()) {
            if (option instanceof OptionGroup) {
                DefaultMutableTreeNode branch = new DefaultMutableTreeNode(option);
                parent.add(branch);
                buildTree((OptionGroup) option, branch);
            }
        }
    }

    /**
     * This function analyses a tree selection event and calls the right methods to take care
     * of building the requested unit's details.
     *
     * @param event The incoming TreeSelectionEvent.
     */
    public void valueChanged(TreeSelectionEvent event) {
        detailPanel.removeAll();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node != null) {
            if (node.isLeaf()) {
                OptionGroup group = (OptionGroup) node.getUserObject();
                for (Option option : group.getOptions()) {
                    addOptionUI(option, editable && group.isEditable());
                }
            } else {
                tree.expandPath(event.getPath());
            }
        }
        detailPanel.revalidate();
        detailPanel.repaint();
    }

    /**
     * Updates the value of the {@link net.sf.freecol.common.option.Option} this object keeps.
     */
    public void updateOption() {
        for (OptionUpdater optionUpdater : optionUpdaters) {
            optionUpdater.updateOption();
        }
    }

    public OptionUI getOptionUI(String key) {
        return optionUIs.get(key);
    }

    private void addOptionUI(Option option, boolean editable) {
        OptionUI ui = OptionUI.getOptionUI(gui, option, editable);
        if (ui == null) {
            logger.warning("Unknown option type: " + option.toString());
        } else if (ui instanceof FreeColActionUI) {
            ((FreeColActionUI) ui).setOptionGroupUI(this);
        }
        JLabel label = ui.getLabel();
        if (label == null) {
            detailPanel.add(ui.getComponent(), "newline, span");
        } else {
            detailPanel.add(label);
            detailPanel.add(ui.getComponent());
        }
        if (group.isEditable()) {
            optionUpdaters.add((OptionUpdater) ui);
        }
        if (!option.getId().equals(FreeColObject.NO_ID)) {
            optionUIs.put(option.getId(), ui);
        }
    }


    /**
     * Removes the given <code>KeyStroke</code> from all of this
     * <code>OptionGroupUI</code>'s children.
     *
     * @param keyStroke The <code>KeyStroke</code> to be removed.
     */
    public void removeKeyStroke(KeyStroke keyStroke) {
        for (OptionUpdater optionUpdater : optionUpdaters) {
            if (optionUpdater instanceof FreeColActionUI) {
                ((FreeColActionUI) optionUpdater).removeKeyStroke(keyStroke);
            }
        }
    }

    /**
     * Reset with the value from the option.
     */
    public void reset() {
        for (OptionUpdater optionUpdater : optionUpdaters) {
            optionUpdater.reset();
        }
    }

    @Override
    public String getUIClassID() {
        return "ReportPanelUI";
    }

}
