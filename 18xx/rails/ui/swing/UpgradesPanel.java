/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/UpgradesPanel.java,v 1.14 2008/06/03 21:25:43 wakko666 Exp $*/
package rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;

import org.apache.log4j.Logger;

import rails.game.*;
import rails.game.action.*;
import rails.ui.swing.elements.ActionLabel;
import rails.ui.swing.hexmap.*;
import rails.util.LocalText;

public class UpgradesPanel extends Box implements MouseListener, ActionListener {
    private static final long serialVersionUID = 1L;
    private ORUIManager orUIManager;
    private List<ActionLabel> tokenLabels;
    private int selectedTokenIndex;
    private List<LayToken> possibleTokenLays = new ArrayList<LayToken>(3);

    static private Color defaultLabelBgColour = new JLabel("").getBackground();
    static private Color selectedLabelBgColour = new Color(255, 220, 150);

    private JPanel upgradePanel;
    private JScrollPane scrollPane;
    private Dimension preferredSize = new Dimension(100, 200);
    private Border border = new EtchedBorder();
    private final String INIT_CANCEL_TEXT = "NoTile";
    private final String INIT_DONE_TEXT = "LayTile";
    private boolean tokenMode = false;
    private JButton cancelButton = new JButton(
            LocalText.getText(INIT_CANCEL_TEXT));
    private JButton doneButton = new JButton(LocalText.getText(INIT_DONE_TEXT));
    private HexMap hexMap;

    protected static Logger log = Logger.getLogger(UpgradesPanel.class.getPackage().getName());

    public UpgradesPanel(ORUIManager orUIManager) {
        super(BoxLayout.Y_AXIS);

        this.orUIManager = orUIManager;

        setSize(preferredSize);
        setVisible(true);

        upgradePanel = new JPanel();

        upgradePanel.setOpaque(true);
        upgradePanel.setBackground(Color.DARK_GRAY);
        upgradePanel.setBorder(border);
        upgradePanel.setLayout(new GridLayout(15, 1));

        scrollPane = new JScrollPane(upgradePanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setSize(getPreferredSize());

        doneButton.setActionCommand("Done");
        doneButton.setMnemonic(KeyEvent.VK_D);
        doneButton.addActionListener(this);
        cancelButton.setActionCommand("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);
        cancelButton.addActionListener(this);

        add(scrollPane);
    }

    public void populate() {
        if (hexMap == null)
            hexMap = orUIManager.getMapPanel().getMap();

        GUIHex uiHex = hexMap.getSelectedHex();
        MapHex hex = uiHex.getHexModel();
        orUIManager.tileUpgrades = new ArrayList<TileI>();
        List<TileI> tiles;

        for (LayTile layTile: hexMap.getTileAllowancesForHex(hex)) {
        	tiles = layTile.getTiles();
        	if (tiles == null) {
        		for (TileI tile : uiHex.getCurrentTile().getValidUpgrades(
                    hex,
                    GameManager.getCurrentPhase())) {
        			if (!orUIManager.tileUpgrades.contains(tile)) orUIManager.tileUpgrades.add (tile);
        		}
        	} else {
        		for (TileI tile : tiles) {
        			if (!orUIManager.tileUpgrades.contains(tile)) orUIManager.tileUpgrades.add (tile);
        		}
        	}
        }
    }

    public void showUpgrades() {
        upgradePanel.removeAll();
        if (tokenMode && possibleTokenLays != null && possibleTokenLays.size() > 0) {

        	Color fgColour = null;
        	Color bgColour = null;
        	String text = null;
        	String description = null;
            TokenIcon icon;
            ActionLabel tokenLabel;
            tokenLabels = new ArrayList<ActionLabel>();
            for (LayToken action : possibleTokenLays) {
            	if (action instanceof LayBaseToken) {
            		PublicCompanyI comp = ((LayBaseToken) action).getCompany();
            		fgColour = comp.getFgColour();
            		bgColour = comp.getBgColour();
            		description = text = comp.getName();
            	} else if (action instanceof LayBonusToken) {
            		fgColour = Color.BLACK;
            		bgColour = Color.WHITE;
            		BonusToken token = (BonusToken) action.getSpecialProperty().getToken();
            		description = token.getName();
            		text = "+"+token.getValue();
            	}
                icon = new TokenIcon (25, fgColour, bgColour, text);
                tokenLabel = new ActionLabel (icon);
                tokenLabel.setName(description);
                tokenLabel.setText(description);
                tokenLabel.setBackground(defaultLabelBgColour);
                tokenLabel.setOpaque(true);
                tokenLabel.setVisible(true);
                tokenLabel.setBorder(border);
                tokenLabel.addMouseListener(this);
                tokenLabel.addPossibleAction(action);
                tokenLabels.add(tokenLabel);

                upgradePanel.add(tokenLabel);
            }

            setSelectedToken ();

        } else if (orUIManager.tileUpgrades == null) {
        } else if (orUIManager.tileUpgrades.size() == 0) {
            orUIManager.setMessage(LocalText.getText("NoTiles"));
        } else {
            for (TileI tile : orUIManager.tileUpgrades) {
                BufferedImage hexImage = getHexImage(tile.getId());
                ImageIcon hexIcon = new ImageIcon(hexImage);

                // Cheap n' Easy rescaling.
                hexIcon.setImage(hexIcon.getImage().getScaledInstance(
                        (int) (hexIcon.getIconWidth() * GUIHex.NORMAL_SCALE*0.8),
                        (int) (hexIcon.getIconHeight() * GUIHex.NORMAL_SCALE*0.8),
                        Image.SCALE_SMOOTH));

                HexLabel hexLabel = new HexLabel(hexIcon, tile.getId());
                hexLabel.setName(tile.getName());
                hexLabel.setText("" + tile.getExternalId());
                hexLabel.setOpaque(true);
                hexLabel.setVisible(true);
                hexLabel.setBorder(border);
                hexLabel.addMouseListener(this);

                upgradePanel.add(hexLabel);
            }
        }

        upgradePanel.add(doneButton);
        upgradePanel.add(cancelButton);

        repaint();
    }

    public void clear () {
    	upgradePanel.removeAll();
        upgradePanel.add(doneButton);
        upgradePanel.add(cancelButton);
    	upgradePanel.repaint();
    }

    public void setSelectedTokenIndex (int index) {
    	log.debug("Selected token index from "+selectedTokenIndex+" to "+index);
    	selectedTokenIndex = index;
    }

    public void setSelectedToken () {
        if (tokenLabels == null || tokenLabels.isEmpty()) return;
        int index = -1;
        for (ActionLabel tokenLabel : tokenLabels) {
            tokenLabel.setBackground(++index == selectedTokenIndex
                    ? selectedLabelBgColour
                    : defaultLabelBgColour);
        }
    }
    private BufferedImage getHexImage(int tileId) {
        return GameUIManager.getImageLoader().getTile(tileId);
    }

    @Override
    public Dimension getPreferredSize() {
        return preferredSize;
    }

    @Override
    public void setPreferredSize(Dimension preferredSize) {
        this.preferredSize = preferredSize;
    }

    public void setTileUpgrades(List<TileI> upgrades) {
        this.orUIManager.tileUpgrades = upgrades;
    }

    public void addUpgrades (List<TileI> upgrades) {
        this.orUIManager.tileUpgrades.addAll(upgrades);
    }

    public void setTileMode(boolean tileMode) {
        setTileUpgrades(null);
    }

    public void setTokenMode(boolean tokenMode) {
        this.tokenMode = tokenMode;
        setTileUpgrades(null);
        possibleTokenLays.clear();
        selectedTokenIndex = -1;
    }

    public <T extends LayToken> void setPossibleTokenLays (List<T> actions) {
        possibleTokenLays.clear();
        selectedTokenIndex = -1;
        if (actions != null) possibleTokenLays.addAll(actions);
    }

    public void setCancelText(String text) {
        cancelButton.setText(LocalText.getText(text));
    }

    public void setDoneText(String text) {
        doneButton.setText(LocalText.getText(text));
    }

    public void setDoneEnabled(boolean enabled) {
        doneButton.setEnabled(enabled);
    }

    public void setCancelEnabled(boolean enabled) {
        cancelButton.setEnabled(enabled);
    }

    public void actionPerformed(ActionEvent e) {

        Object source = e.getSource();

        if (source == cancelButton) {
        	orUIManager.cancelUpgrade();
        } else if (source == doneButton) {
            orUIManager.executeUpgrade();
        }
    }

    public void mouseClicked(MouseEvent e) {

    	Object source = e.getSource();
        if (!(source instanceof JLabel)) return;

        if (tokenMode) {
        	if (tokenLabels.contains(source)) {
        		orUIManager.tokenSelected((LayToken)((ActionLabel)source).getPossibleActions().get(0));
                setDoneEnabled(true);
        	} else {
        		orUIManager.tokenSelected(null);
        	}
            setSelectedToken();
        } else {

	        int id = ((HexLabel) e.getSource()).getInternalId();

	        orUIManager.tileSelected(id);
        }

    }

    public void mouseEntered(MouseEvent e) {
        Object source = e.getSource();
        if (!(source instanceof JLabel)) return;
        
        if (!tokenMode) {
            // tile mode
            HexLabel tile = (HexLabel) e.getSource();
            String tooltip = tile.getToolTip();
            if (tooltip != "") {
                tile.setToolTipText(tooltip);
            }
        }
    }

    public void mouseExited(MouseEvent e) {
        Object source = e.getSource();
        if (!(source instanceof JLabel)) return;
        
        if (!tokenMode) {
            // tile mode
            HexLabel tile = (HexLabel) e.getSource();
            tile.setToolTipText(null);
        }
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void finish() {
        setDoneEnabled(false);
        setCancelEnabled(false);
    }

    /** JLabel extension to allow attaching the internal hex ID */
    private class HexLabel extends JLabel {

        String toolTip;
        int internalId;

        HexLabel (ImageIcon hexIcon, int internalId) {
            super (hexIcon);
            this.internalId = internalId;
            this.setToolTip();
        }

        int getInternalId () {
            return internalId;
        }
        
        public String getToolTip()
        {
            return toolTip;
        }
        
        protected void setToolTip()
        {
            TileI currentTile = TileManager.get().getTile(internalId);
            StringBuffer tt = new StringBuffer("<html>");
            tt.append("<b>Tile</b>: ").append(currentTile.getName()); // or getId()
            if (currentTile.hasStations())
            {
                //for (Station st : currentTile.getStations())
                int cityNumber = 0;
                // TileI has stations, but 
                for (Station st: currentTile.getStations())
                {
                    cityNumber++; // = city.getNumber();
                    tt.append("<br>  ").append(st.getType())
                    .append(" ").append(cityNumber) //.append("/").append(st.getNumber())
                    .append(": value ");
                    tt.append(st.getValue());
                    if (st.getBaseSlots() > 0)
                    {
                        tt.append(", ").append(st.getBaseSlots()).append(" slots");
                    }
                }
            }
            tt.append("</html>");
            toolTip = tt.toString();
        }

    }
}