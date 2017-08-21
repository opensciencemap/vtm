/*
 * Copyright 2017 Longri
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.theme.comparator;

import org.oscim.theme.comparator.editor.EditorPane;
import org.oscim.theme.comparator.location.LocationPane;
import org.oscim.theme.comparator.logging.AllLoggingPane;
import org.oscim.theme.comparator.logging.MapsforgeLoggingPane;
import org.oscim.theme.comparator.logging.VtmLoggingPane;
import org.oscim.theme.comparator.mapsforge.MapsforgeMapPanel;
import org.oscim.theme.comparator.vtm.VtmPanel;

import java.awt.GridLayout;
import java.awt.event.KeyEvent;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

class InfoPanel extends JPanel {

    InfoPanel(final VtmPanel vtmPanel, final MapsforgeMapPanel mapsforgeMapPanel,
              final MainMenu mainMenu) {
        super(new GridLayout(1, 1));

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JTabbedPane tabbedPane = new JTabbedPane();
                tabbedPane.addTab("Theme Editor", Main.ICON_EDIT, new EditorPane(vtmPanel, mapsforgeMapPanel, mainMenu), "");
                tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

                tabbedPane.addTab("Log's ALL", Main.ICON_DEBUG, new AllLoggingPane(), "");
                tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);

                tabbedPane.addTab("Log's VTM", Main.ICON_DEBUG, new VtmLoggingPane(), "");
                tabbedPane.setMnemonicAt(1, KeyEvent.VK_3);

                tabbedPane.addTab("Log's MAPSFORGE", Main.ICON_DEBUG, new MapsforgeLoggingPane(), "");
                tabbedPane.setMnemonicAt(1, KeyEvent.VK_4);

                tabbedPane.addTab("Set Map position", Main.ICON_LOCATE, new LocationPane(), "");
                tabbedPane.setMnemonicAt(1, KeyEvent.VK_4);

                tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
                add(tabbedPane);
            }
        });
    }
}
