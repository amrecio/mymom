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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.Border;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitType;

/**
 * This panel displays a report.
 */
public class ReportPanel extends FreeColPanel implements ActionListener {

    protected static final Logger logger = Logger.getLogger(ReportPanel.class.getName());

    protected static final String OK = "OK";

    protected JPanel reportPanel;

    protected JLabel header;

    protected JScrollPane scrollPane;

    private JButton ok;

    private static final Comparator<Unit> unitTypeComparator = new Comparator<Unit>() {
        public int compare(Unit unit1, Unit unit2) {
            int deltaType = unit2.getType().getIndex() - unit1.getType().getIndex();
            if (deltaType == 0) {
                return unit2.getRole().ordinal() - unit1.getRole().ordinal();
            } else {
                return deltaType;
            }
        }
    };


    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     * @param title The title to display on the panel.
     */
    public ReportPanel(Canvas parent, String title) {
        super(parent, new FlowLayout(FlowLayout.CENTER, 1000, 10));

        setLayout(new BorderLayout());

        header = getDefaultHeader(title);
        add(header, BorderLayout.NORTH);

        reportPanel = new JPanel();
        reportPanel.setOpaque(true);
        reportPanel.setBorder(createBorder());

        scrollPane = new JScrollPane(reportPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );
        add(scrollPane, BorderLayout.CENTER);

        ok = new JButton(Messages.message("ok"));
        ok.setActionCommand(String.valueOf(OK));
        ok.addActionListener(this);
        enterPressesWhenFocused(ok);
        setCancelComponent(ok);
        add(ok, BorderLayout.SOUTH);

        setSize(850, 600);
    }
    
    protected Border createBorder() {
        return new EmptyBorder(20, 20, 20, 20);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(850, 600);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }


    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        reportPanel.removeAll();
        reportPanel.doLayout();
    }

    /**
     * 
     */
    public void requestFocus() {
        ok.requestFocus();
    }

    /**
     * Returns a unit type comparator.
     * 
     * @return A unit type comparator.
     */
    public static Comparator<Unit> getUnitTypeComparator() {
        return unitTypeComparator;
    }

    public JLabel createUnitTypeLabel(AbstractUnit unit) {
        return createUnitTypeLabel(unit.getUnitType(), unit.getRole(), unit.getNumber());
    }

    public JLabel createUnitTypeLabel(UnitType unitType, Role role, int count) {
        ImageIcon unitIcon = getLibrary().getUnitImageIcon(unitType, role, count == 0);
        JLabel unitLabel = new JLabel(getLibrary().getScaledImageIcon(unitIcon, 0.66f));
        unitLabel.setText(String.valueOf(count));
        if (count == 0) {
            unitLabel.setForeground(Color.GRAY);
        }
        unitLabel.setToolTipText(Unit.getName(unitType, role));
        return unitLabel;
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            getCanvas().remove(this);
        } else {
            FreeColGameObject object = getGame().getFreeColGameObject(command);
            if (object instanceof Colony) {
                getCanvas().showColonyPanel((Colony) object);
            } else if (object instanceof Europe) {
                getCanvas().showEuropePanel();
            } else if (object instanceof Tile) {
                getCanvas().getGUI().setFocus(((Tile) object).getPosition());
            }
        }
    }
}
