package net.sf.freecol.client.gui.plaf;

import         javax.swing.plaf.basic.*;
import         javax.swing.plaf.*;
import         javax.swing.*;
import         javax.swing.table.*;



public class FreeColTableHeaderUI extends BasicTableHeaderUI {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static ComponentUI createUI(JComponent c) {
        return new FreeColTableHeaderUI();
    }

    
    public void installUI(JComponent c) {
        super.installUI(c);

        JTableHeader j = (JTableHeader) c;
        j.setOpaque(false);

        DefaultTableCellRenderer dtcr = (DefaultTableCellRenderer) j.getDefaultRenderer();
        dtcr.setOpaque(false);
    }

}
