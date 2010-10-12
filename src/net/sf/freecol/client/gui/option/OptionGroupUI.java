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

package net.sf.freecol.client.gui.option;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import net.sf.freecol.client.gui.action.FreeColAction;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.FileOption;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.LanguageOption;
import net.sf.freecol.common.option.ListOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.common.option.RangeOption;
import net.sf.freecol.common.option.SelectOption;
import net.sf.freecol.common.option.StringOption;

import net.miginfocom.swing.MigLayout;

/**
 * This class provides visualization for an {@link OptionMap}. In order to
 * enable values to be both seen and changed.
 */
public final class OptionGroupUI extends JPanel implements OptionUpdater {

    private static final Logger logger = Logger.getLogger(OptionGroupUI.class.getName());

    public static final int H_GAP = 10;

    private final List<OptionUpdater> optionUpdaters = new ArrayList<OptionUpdater>();
    
    private final HashMap<String, JComponent> optionUIs;

    private final JTabbedPane tb;


    /**
     * Creates a new <code>OptionGroupUI</code> for the given
     * <code>OptionMap</code>. This is the same as using
     * {@link #OptionGroupUI(OptionMap, boolean)} with
     * <code>editable == true</code>.
     * 
     * @param option The <code>OptionMap</code> to make a user interface for.
     */
    public OptionGroupUI(OptionGroup option) {
        this(option, true);
    }

    /**
     * Creates a new <code>OptionGroupUI</code> for the given
     * <code>OptionMap</code>.
     * 
     * @param option The <code>OptionMap</code> to make a user interface for.
     */
    public OptionGroupUI(OptionGroup option, boolean editable) {
        super(new BorderLayout());

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new MigLayout("wrap 4", "[fill]related[fill]unrelated[fill]related[fill]"));
        northPanel.setOpaque(false);
        
        optionUIs = new HashMap<String, JComponent>();

        tb = new JTabbedPane(JTabbedPane.TOP);
        tb.setOpaque(false);

        Iterator<Option> it = option.iterator();
        while (it.hasNext()) {
            Option o = it.next();

            if (o instanceof OptionGroup) {
                JPanel groupPanel = new JPanel();
                groupPanel.setLayout(new MigLayout("wrap 4", "[fill]related[fill]unrelated[fill]related[fill]"));
                groupPanel.setOpaque(true);
                addOptionGroupUI((OptionGroup) o, groupPanel, editable);
                JScrollPane scroll = new JScrollPane(groupPanel,
                                                     JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                scroll.getVerticalScrollBar().setUnitIncrement(16);
                scroll.setBorder(BorderFactory.createEmptyBorder());
                groupPanel.setBorder(BorderFactory.createEmptyBorder(H_GAP - 5, H_GAP, 0, H_GAP));
                tb.addTab(o.getName(), null, scroll, o.getShortDescription());
            } else {
                addOptionUI(o, northPanel, editable);
            }
        }
        if (tb.getTabCount() > 0) {
            if (northPanel.getComponentCount() > 0) {
                tb.addTab(" *** ", northPanel);
            }
            add(tb, BorderLayout.CENTER);
        } else {
            add(northPanel, BorderLayout.CENTER);
        }

        setOpaque(false);
    }


    private void addOptionGroupUI(OptionGroup group, JPanel panel, boolean editable) {
        Iterator<Option> iterator = group.iterator();
        while (iterator.hasNext()) {
            Option o = iterator.next();
            if (o instanceof OptionGroup) {
                panel.add(new JLabel(o.getName()), "newline 20, span, split 2");
                panel.add(new JSeparator(), "growx");
                addOptionGroupUI((OptionGroup) o, panel, editable);
            } else {
                addOptionUI(o, panel, editable);
            }
        }
    }

    private void addOptionUI(Option option, JPanel panel, boolean editable) {
        if (option instanceof BooleanOption) {
            BooleanOptionUI c = new BooleanOptionUI((BooleanOption) option, editable);
            if (c.getText().length() > 40) {
                panel.add(c, "newline, span");
            } else {
                panel.add(c, "span 2");
            }
            optionUpdaters.add(c);
            if (!option.getId().equals(Option.NO_ID)) {
                optionUIs.put(option.getId(), c);
            }
        } else if (option instanceof FileOption) {
            final FileOptionUI iou = new FileOptionUI((FileOption) option, editable);
            panel.add(iou, "newline, span");
            optionUpdaters.add(iou);
            if (!option.getId().equals(Option.NO_ID)) {
                optionUIs.put(option.getId(), iou);
            }
        } else if (option instanceof PercentageOption) {
            PercentageOptionUI c = new PercentageOptionUI((PercentageOption) option, editable);
            panel.add(c, "newline, span");
            optionUpdaters.add(c);
            if (!option.getId().equals(Option.NO_ID)) {
                optionUIs.put(option.getId(), c);
            }
        } else if (option instanceof ListOption<?>) {
            @SuppressWarnings("unchecked")
            ListOptionUI c = new ListOptionUI((ListOption) option, editable);
            panel.add(c);
            optionUpdaters.add(c);
            if (!option.getId().equals(Option.NO_ID)) {
                optionUIs.put(option.getId(), c);
            }
        } else if (option instanceof RangeOption) {
            RangeOptionUI c = new RangeOptionUI((RangeOption) option, editable);
            panel.add(c, "newline, span");
            optionUpdaters.add(c);
            if (!option.getId().equals(Option.NO_ID)) {
                optionUIs.put(option.getId(), c);
            }
        } else if (option instanceof SelectOption) {
            SelectOptionUI c = new SelectOptionUI((SelectOption) option, editable);
            if (c.getLabel().getText().length() > 30) {
                panel.add(c.getLabel(), "newline, span 3, right");
            } else {
                panel.add(c.getLabel(), "right");
            }
            panel.add(c);
            optionUpdaters.add(c);
            if (!option.getId().equals(Option.NO_ID)) {
                optionUIs.put(option.getId(), c);
            }
        } else if (option instanceof IntegerOption) {
            IntegerOptionUI c = new IntegerOptionUI((IntegerOption) option, editable);
            if (c.getLabel().getText().length() > 30) {
                panel.add(c.getLabel(), "newline, span 3, right");
            } else {
                panel.add(c.getLabel(), "right");
            }
            panel.add(c);
            optionUpdaters.add(c);
            if (!option.getId().equals(Option.NO_ID)) {
                optionUIs.put(option.getId(), c);
            }
        } else if (option instanceof StringOption) {
            final StringOptionUI soi = new StringOptionUI((StringOption) option, editable);
            if (soi.getLabel().getText().length() > 30) {
                panel.add(soi.getLabel(), "newline, span 3, right");
            } else {
                panel.add(soi.getLabel(), "right");
            }
            panel.add(soi);
            optionUpdaters.add(soi);
            if (!option.getId().equals(Option.NO_ID)) {
                optionUIs.put(option.getId(), soi);
            }
        } else if (option instanceof LanguageOption) {
            LanguageOptionUI c = new LanguageOptionUI((LanguageOption) option, editable);
            if (c.getLabel().getText().length() > 30) {
                panel.add(c.getLabel(), "newline, span 3");
            } else {
                panel.add(c.getLabel());
            }
            panel.add(c);
            optionUpdaters.add(c);
            if (!option.getId().equals(Option.NO_ID)) {
                optionUIs.put(option.getId(), c);
            }
        } else if (option instanceof AudioMixerOption) {
            AudioMixerOptionUI c = new AudioMixerOptionUI((AudioMixerOption) option, editable);
            if (c.getLabel().getText().length() > 30) {
                panel.add(c.getLabel(), "newline, span 3");
            } else {
                panel.add(c.getLabel());
            }
            panel.add(c);
            optionUpdaters.add(c);
            if (!option.getId().equals(Option.NO_ID)) {
                optionUIs.put(option.getId(), c);
            }
        } else if (option instanceof FreeColAction) {
            final FreeColActionUI fau = new FreeColActionUI((FreeColAction) option, this);
            optionUpdaters.add(fau);
            panel.add(fau, "newline, span");
            if (!option.getId().equals(Option.NO_ID)) {
                optionUIs.put(option.getId(), fau);
            }
        } else {
            logger.warning("Unknown option: " + option.getId() + " (" + option.getClass() + ")");
        }
    }


    /**
     * Rollback to the original value.
     * 
     * This method gets called so that changes made to options with
     * {@link Option#isPreviewEnabled()} is rolled back
     * when an option dialoag has been cancelled.
     */
    public void rollback() {
        for (OptionUpdater optionUpdater : optionUpdaters) {
            optionUpdater.rollback();
        }
    }
    
    /**
     * Unregister <code>PropertyChangeListener</code>s.
     */
    public void unregister() {
        for (OptionUpdater optionUpdater : optionUpdaters) {
            optionUpdater.unregister();
        }
    }

    /**
     * Updates the value of the {@link Option} this object keeps.
     */
    public void updateOption() {
        for (OptionUpdater optionUpdater : optionUpdaters) {
            optionUpdater.updateOption();
        }
    }
    
    public JComponent getOptionUI(String key) {
        return optionUIs.get(key);
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
}
