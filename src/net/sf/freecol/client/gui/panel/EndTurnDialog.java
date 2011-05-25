/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.PanelUI;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColSelectedPanelUI;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;

import net.miginfocom.swing.MigLayout;


/**
 * Centers the map on a known settlement or colony. Pressing ENTER
 * opens a panel if appropriate.
 */
public final class EndTurnDialog extends FreeColDialog<Boolean> implements ListSelectionListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FindSettlementDialog.class.getName());

    private JList unitList;

    /**
     * We need to wrap the Unit class in order to make the JList
     * support keystroke navigation. JList.getNextMatch uses the
     * toString() method, but the toString() method of FreeCol objects
     * provides debugging information rather than a searchable name.
     */
    public class UnitWrapper {
        public Unit unit;
        public String name;
        public String location;

        public UnitWrapper(Unit unit) {
            this.unit = unit;
            name = Messages.message(Messages.getLabel(unit));
            location = Messages.message(unit.getLocation().getLocationName());
        }

        public String toString() {
            return name;
        }
    }



    /**
     * The constructor to use.
     */
    public EndTurnDialog(Canvas parent, List<Unit> units) {
        super(parent);

        setLayout(new MigLayout("wrap 1", "[align center]"));

        JLabel header = new JLabel(Messages.message("endTurnDialog.name"));
        header.setFont(smallHeaderFont);

        StringTemplate t = StringTemplate.template("endTurnDialog.areYouSure")
            .addAmount("%number%", units.size());

        DefaultListModel model = new DefaultListModel();
        for (Unit unit : units) {
            model.addElement(new UnitWrapper(unit));
        }
        unitList = new JList(model);
        unitList.setCellRenderer(new UnitCellRenderer());
        unitList.setFixedCellHeight(48);
        JScrollPane listScroller = new JScrollPane(unitList);
        listScroller.setPreferredSize(new Dimension(250, 250));
        unitList.addListSelectionListener(this);

        Action selectAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    selectUnit();
                }
            };

        Action quitAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    EndTurnDialog.this.setResponse(Boolean.FALSE);
                }
            };

        unitList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "select");
        unitList.getActionMap().put("select", selectAction);
        unitList.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "quit");
        unitList.getActionMap().put("quit", quitAction);

        MouseListener mouseListener = new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        selectUnit();
                    }
                }
            };
        unitList.addMouseListener(mouseListener);

        add(header);
        add(getDefaultTextArea(Messages.message(t)), "newline 30");
        add(listScroller, "width max(200, 100%), height max(300, 100%), newline 20");

        add(cancelButton, "newline 20, span, split 2, tag cancel");
        add(okButton, "tag ok");

        restoreSavedSize(getPreferredSize());
    }

    private void selectUnit() {
        Unit unit = (Unit) unitList.getSelectedValue();
        if (unit != null) {
            if (unit.getColony() != null) {
                getCanvas().showColonyPanel(unit.getColony());
            } else if (unit.isInEurope()) {
                getCanvas().showEuropePanel();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestFocus() {
        unitList.requestFocus();
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     *
     * @param e a <code>ListSelectionEvent</code> value
     */
    public void valueChanged(ListSelectionEvent e) {
        Unit unit = ((UnitWrapper) unitList.getSelectedValue()).unit;
        if (unit != null && unit.getTile() != null) {
            getCanvas().getGUI().setFocus(unit.getTile());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            setResponse(Boolean.TRUE);
        } else if (CANCEL.equals(command)) {
            setResponse(Boolean.FALSE);
        } else {
            super.actionPerformed(event);
        }
    }

    private class UnitCellRenderer implements ListCellRenderer {

        JPanel itemPanel = new JPanel();
        JPanel selectedPanel = new JPanel();
        JLabel imageLabel = new JLabel();
        JLabel nameLabel = new JLabel();
        JLabel locationLabel = new JLabel();

        public UnitCellRenderer() {
            itemPanel.setOpaque(false);
            itemPanel.setLayout(new MigLayout("", "[60]"));
            selectedPanel.setOpaque(false);
            selectedPanel.setLayout(new MigLayout("", "[60]"));
            selectedPanel.setUI((PanelUI) FreeColSelectedPanelUI.createUI(selectedPanel));
            locationLabel.setFont(locationLabel.getFont().deriveFont(Font.ITALIC));
        }

        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            UnitWrapper unit = (UnitWrapper) value;
            JPanel panel = isSelected ? selectedPanel : itemPanel;
            panel.removeAll();

            imageLabel.setIcon(getLibrary().getUnitImageIcon(unit.unit, 0.5));
            nameLabel.setText(unit.name);
            locationLabel.setText(unit.location);

            panel.add(imageLabel, "center");
            panel.add(nameLabel, "split 2, flowy");
            panel.add(locationLabel);
            return panel;
        }
    }

}

