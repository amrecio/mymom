/**
 *  Copyright (C) 2002-2010  The FreeCol Team
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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.OptionMapUI;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.AbstractOption;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.resources.ResourceManager;

import net.miginfocom.swing.MigLayout;


/**
* Dialog for changing the {@link net.sf.freecol.common.model.DifficultyLevel}.
*/
public final class DifficultyDialog extends FreeColDialog<OptionGroup> implements ItemListener {

    private static final Logger logger = Logger.getLogger(DifficultyDialog.class.getName());

    private static final String RESET = "RESET";

    private OptionMapUI ui;
    private OptionGroup level;
    private JPanel optionPanel;

    private String DEFAULT_LEVEL = "model.difficulty.medium";
    private String CUSTOM_LEVEL = "model.difficulty.custom";

    /**
     * We need our own copy of the specification, as the dialog is
     * used before the game has been started.
     */
    private Specification specification;

    private final JComboBox difficultyBox = new JComboBox();


    public DifficultyDialog(Canvas parent, OptionGroup level) {
        super(parent);
        specification = getSpecification();
        List<OptionGroup> levels = new ArrayList<OptionGroup>(1);
        levels.add(level);
        initialize(levels);
    }

    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public DifficultyDialog(Canvas parent, Specification specification) {
        super(parent);
        this.specification = specification;
        initialize(specification.getDifficultyLevels());
    }

    private void initialize(List<OptionGroup> levels) {
        setLayout(new MigLayout("wrap 1, fill"));

        // Header:
        JLabel header = localizedLabel("gameOptions.difficultySettings.name");
        header.setFont(ResourceManager.getFont("HeaderFont", 48f));
        add(header, "center, wrap 20");

        for (OptionGroup dLevel : levels) {
            String id = dLevel.getId();
            difficultyBox.addItem(id);
            if (DEFAULT_LEVEL.equals(id)) {
                level = dLevel;
                difficultyBox.setSelectedIndex(difficultyBox.getItemCount() - 1);
            }
        }

        if (levels.size() == 1) {
            difficultyBox.setEnabled(false);
        } else {
            difficultyBox.addItemListener(this);
        }
        add(difficultyBox);

        // Options:
        ui = new OptionMapUI(level, false);
        ui.setOpaque(false);
        optionPanel = new JPanel() {
            @Override
            public String getUIClassID() {
                return "ReportPanelUI";
            }
        };
        optionPanel.setOpaque(true);
        optionPanel.add(ui);
        JScrollPane scrollPane = new JScrollPane(optionPanel,
                                                 JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );
        add(scrollPane, "height 100%, width 100%");

        // Buttons:
        if (levels.size() == 1) {
            add(okButton, "newline 20, tag ok");
        } else {
            add(okButton, "newline 20, split 3, tag ok");

            JButton reset = new JButton(Messages.message("reset"));
            reset.setActionCommand(RESET);
            reset.addActionListener(this);
            reset.setMnemonic('R');
            add(reset);
        
            add(cancelButton, "tag cancel");
        }

        setSize(780, 540);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(780, 540);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    public void initialize() {
        removeAll();

    }

    /**
     * This function analyses an event and calls the right methods to take
     * care of the user's requests.
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            ui.unregister();
            ui.updateOption();
            getCanvas().remove(this);
            setResponse(level);
        } else if (CANCEL.equals(command)) {
            ui.rollback();
            ui.unregister();
            getCanvas().remove(this);
            setResponse(specification.getOptionGroup(DEFAULT_LEVEL));
        } else if (RESET.equals(command)) {
            ui.reset();
        } else {
            logger.warning("Invalid ActionCommand: " + command);
        }
    }

    public void itemStateChanged(ItemEvent event) {
        String id = (String) difficultyBox.getSelectedItem();
        Specification spec = specification;
        level = spec.getOptionGroup(id);
        ui = new OptionMapUI(level, (CUSTOM_LEVEL.equals(id)));
        optionPanel.removeAll();
        optionPanel.add(ui);
        revalidate();
        repaint();
    }
}
