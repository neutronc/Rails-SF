/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/UpgradesPanel.java,v 1.29 2010/06/25 20:47:45 evos Exp $*/
package rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import org.apache.log4j.Logger;

import rails.common.LocalText;
import rails.game.*;
import rails.game.action.*;
import rails.game.correct.MapCorrectionAction;
import rails.ui.swing.elements.ActionLabel;
import rails.ui.swing.elements.RailsIcon;
import rails.ui.swing.elements.RailsIconButton;
import rails.ui.swing.hexmap.GUIHex;
import rails.ui.swing.hexmap.GUITile;
import rails.ui.swing.hexmap.HexHighlightMouseListener;
import rails.ui.swing.hexmap.HexMap;

public class UpgradesPanel extends Box implements MouseListener, ActionListener {
    private static final long serialVersionUID = 1L;
    
    private static final int UPGRADE_TILE_ZOOM_STEP = 10;

    private ORUIManager orUIManager;
    private List<ActionLabel> tokenLabels;
    private List<CorrectionTokenLabel> correctionTokenLabels;
    private int selectedTokenIndex;
    private List<LayToken> possibleTokenLays = new ArrayList<LayToken>(3);

    static private Color defaultLabelBgColour = new JLabel("").getBackground();
    static private Color selectedLabelBgColour = new Color(255, 220, 150);
    private static final int defaultNbPanelElements = 15;

    private JPanel upgradePanel;
    private JScrollPane scrollPane;
    private Dimension preferredSize;
    private Border border = new EtchedBorder();
    private boolean tokenMode = false;
    private boolean correctionTokenMode = false;
    private RailsIconButton cancelButton = new RailsIconButton(RailsIcon.NO_TILE);
    private RailsIconButton doneButton = new RailsIconButton(RailsIcon.LAY_TILE);
    private HexMap hexMap;
    
    /**
     * If set, done/cancel buttons are not added to the pane. Instead, the
     * visibility property of these buttons are handled such that they are set to
     * visible if they normally would be added to the pane.
     */
    private boolean omitButtons;
    
    //list of tiles with an attached reason why it would represent an invalid upgrade
    private Map<TileI,String> invalidTileUpgrades = null;
    private static final String invalidUpgradeNoTilesLeft = "NoTilesLeft";
    private static final String invalidUpgradeNoValidOrientation = "NoValidOrientation";

    protected static Logger log =
        Logger.getLogger(UpgradesPanel.class.getPackage().getName());

    public UpgradesPanel(ORUIManager orUIManager,boolean omitButtons) {
        super(BoxLayout.Y_AXIS);

        this.orUIManager = orUIManager;
        this.omitButtons = omitButtons;

        preferredSize = new Dimension((int)Math.round(110 * (2 +  Scale.getFontScale())/3), 200);
        setSize(preferredSize);
        setVisible(true);

        upgradePanel = new JPanel();

        upgradePanel.setOpaque(true);
        upgradePanel.setBackground(Color.DARK_GRAY);
        upgradePanel.setBorder(border);
        upgradePanel.setLayout(new GridLayout(defaultNbPanelElements, 1));

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
        
        if (omitButtons) {
            doneButton.setVisible(false);
            cancelButton.setVisible(false);
        }

        add(scrollPane);
    }

    public void populate(PhaseI currentPhase) {
        if (hexMap == null) hexMap = orUIManager.getMapPanel().getMap();

        GUIHex uiHex = hexMap.getSelectedHex();
        MapHex hex = uiHex.getHexModel();
        orUIManager.tileUpgrades = new ArrayList<TileI>();
        List<TileI> tiles;
        Set<String> allowedColours = new HashSet<String> (currentPhase.getTileColours());

        for (LayTile layTile : hexMap.getTileAllowancesForHex(hex)) {
            tiles = layTile.getTiles();
            if (tiles == null) {
                for (TileI tile : uiHex.getCurrentTile().getValidUpgrades(hex,
                        orUIManager.gameUIManager.getCurrentPhase())) {
                    // Skip if not allowed in LayTile
                    //if (!layTile.isTileColourAllowed(tile.getColourName())) continue;

                    if (layTile.isTileColourAllowed(tile.getColourName()))
                        orUIManager.addTileUpgradeIfValid(uiHex,tile);
                }
            } else {
                for (TileI tile : tiles) {
                    // Skip if colour is not allowed yet
                    if (!allowedColours.contains(tile.getColourName())) continue;
                    orUIManager.addTileUpgradeIfValid(uiHex,tile);
                }
            }
        }
        
        //determine invalid upgrades
        //duplicates game engine logic to some degree
        //but this is indispensable since the game engine's services not sufficient here
        invalidTileUpgrades = new HashMap<TileI,String>();
        for (TileI tile : uiHex.getCurrentTile().getUpgrades(hex, currentPhase)) {
            if (!orUIManager.tileUpgrades.contains(tile)) {
                if (!currentPhase.isTileColourAllowed(tile.getColourName())) {
                    //current design decision: don't display tiles for invalid phases
                } else if (!orUIManager.isTileUpgradeValid(uiHex, tile)) {
                    invalidTileUpgrades.put(tile, invalidUpgradeNoValidOrientation);
                } else if (tile.countFreeTiles() == 0) {
                    invalidTileUpgrades.put(tile, invalidUpgradeNoTilesLeft);
                }
            }
        }
    }

    public void showUpgrades() {
        clearPanel();

        // reset to the number of elements
        GridLayout panelLayout = (GridLayout)upgradePanel.getLayout();
        panelLayout.setRows(defaultNbPanelElements);

        if (tokenMode && possibleTokenLays != null
                && possibleTokenLays.size() > 0) {

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
                    if (action.getSpecialProperty() != null) {
                        description += " (" + action.getSpecialProperty().getOriginalCompany().getName()+")";
                    }
                } else if (action instanceof LayBonusToken) {
                    fgColour = Color.BLACK;
                    bgColour = Color.WHITE;
                    BonusToken token =
                        (BonusToken) action.getSpecialProperty().getToken();
                    description = token.getName();
                    text = "+" + token.getValue();
                }
                icon = new TokenIcon(25, fgColour, bgColour, text);
                tokenLabel = new ActionLabel(icon);
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

            setSelectedToken();

        } else if (orUIManager.tileUpgrades == null) {
            ;
        } else { 
            if (orUIManager.tileUpgrades.size() == 0) {
                orUIManager.setMessage(LocalText.getText("NoTiles"));
            } else {
                for (TileI tile : orUIManager.tileUpgrades) {
                    HexLabel hexLabel = createHexLabel(tile,null);
                    hexLabel.addMouseListener(this);
                    upgradePanel.add(hexLabel);
                }
            }
            if (invalidTileUpgrades != null) {
                for (TileI tile : invalidTileUpgrades.keySet()) {
                    HexLabel hexLabel = createHexLabel(tile, 
                            LocalText.getText(invalidTileUpgrades.get(tile)));
                    hexLabel.setEnabled(false);
                    hexLabel.setToolTipText(hexLabel.getToolTip());
                    //highlight where tiles of this ID have been laid if no tiles left
                    if (invalidTileUpgrades.get(tile).equals(invalidUpgradeNoTilesLeft)) {
                        HexHighlightMouseListener.addMouseListener(hexLabel, 
                            orUIManager, tile.getId(), true);
                    }
                    upgradePanel.add(hexLabel);
                }
            }
        }
        
        addButtons();
        
        //repaint();
        revalidate();
    }
    
    private HexLabel createHexLabel(TileI tile,String toolTipHeaderLine) {
        BufferedImage hexImage = null;

        //get a buffered image of the tile in the first valid orientation
        GUIHex selectedGUIHex = hexMap.getSelectedHex();
        if (selectedGUIHex != null) {
            GUITile tempGUITile = selectedGUIHex.createUpgradeTileIfValid (
                    tile.getId(), 
                    orUIManager.getMustConnectRequirement(selectedGUIHex, tile));
            
            if (tempGUITile != null) {
                //tile has been rotated to valid orientation
                //get unscaled image for this orientation 
                hexImage = tempGUITile.getTileImage(getZoomStep());
            }
        }

        //fallback if no valid orientation exists: 
        //get the image in the standard orientation
        if (hexImage == null) {
            hexImage = getHexImage(tile.getPictureId());
        }
        
        ImageIcon hexIcon = new ImageIcon(hexImage);

        // Cheap n' Easy rescaling.
        hexIcon.setImage(hexIcon.getImage().getScaledInstance(
                (int) (hexIcon.getIconWidth() * GUIHex.NORMAL_SCALE * 0.8),
                (int) (hexIcon.getIconHeight() * GUIHex.NORMAL_SCALE * 0.8),
                Image.SCALE_SMOOTH));

        HexLabel hexLabel = new HexLabel(hexIcon, tile.getId(),toolTipHeaderLine);
        hexLabel.setName(tile.getName());
        hexLabel.setTextFromTile(tile);
        hexLabel.setOpaque(true);
        hexLabel.setVisible(true);
        hexLabel.setBorder(border);

        return hexLabel;
    }

    // populate version for corrections
    public void showCorrectionTileUpgrades() {
        // deactivate correctionTokenMode and tokenmode
        correctionTokenMode = false;
        tokenMode = false;

        // activate upgrade panel
        clearPanel();
        GridLayout panelLayout = (GridLayout)upgradePanel.getLayout();
        List<TileI> tiles = orUIManager.tileUpgrades;

        if (tiles == null || tiles.size() == 0) {
            // reset to the number of elements
            panelLayout.setRows(defaultNbPanelElements);
            // set to position 0
            scrollPane.getVerticalScrollBar().setValue(0);
        } else {
            // set to the max of available or the default number of elements
            panelLayout.setRows(Math.max(tiles.size() + 2, defaultNbPanelElements));
            for (TileI tile : tiles) {

                BufferedImage hexImage = getHexImage(tile.getId());
                ImageIcon hexIcon = new ImageIcon(hexImage);

                // Cheap n' Easy rescaling.
                hexIcon.setImage(hexIcon.getImage().getScaledInstance(
                        (int) (hexIcon.getIconWidth() * GUIHex.NORMAL_SCALE * 0.8),
                        (int) (hexIcon.getIconHeight() * GUIHex.NORMAL_SCALE * 0.8),
                        Image.SCALE_SMOOTH));

                HexLabel hexLabel = new HexLabel(hexIcon, tile.getId());
                hexLabel.setName(tile.getName());
                hexLabel.setTextFromTile(tile);
                hexLabel.setOpaque(true);
                hexLabel.setVisible(true);
                hexLabel.setBorder(border);
                hexLabel.addMouseListener(this);

                upgradePanel.add(hexLabel);
            }
        }

        addButtons();

        //      repaint();
        revalidate();
    }

    // populate version for corrections
    public void showCorrectionTokenUpgrades(MapCorrectionAction action) {
        // activate correctionTokenMode and deactivate standard tokenMode
        correctionTokenMode = true;
        tokenMode = false;

        // activate upgrade panel
        clearPanel();
        GridLayout panelLayout = (GridLayout)upgradePanel.getLayout();
        List<? extends TokenI> tokens = orUIManager.tokenLays;

        if (tokens == null || tokens.size() == 0) {
            // reset to the number of elements
            panelLayout.setRows(defaultNbPanelElements);
            // set to position 0
            scrollPane.getVerticalScrollBar().setValue(0);
        } else {
            Color fgColour = null;
            Color bgColour = null;
            String text = null;
            String description = null;
            TokenIcon icon;
            CorrectionTokenLabel tokenLabel;
            correctionTokenLabels = new ArrayList<CorrectionTokenLabel>();
            for (TokenI token:tokens) {
                if (token instanceof BaseToken) {
                    PublicCompanyI comp = ((BaseToken)token).getCompany();
                    fgColour = comp.getFgColour();
                    bgColour = comp.getBgColour();
                    description = text = comp.getName();
                }
                icon = new TokenIcon(25, fgColour, bgColour, text);
                tokenLabel = new CorrectionTokenLabel(icon, token);
                tokenLabel.setName(description);
                tokenLabel.setText(description);
                tokenLabel.setBackground(defaultLabelBgColour);
                tokenLabel.setOpaque(true);
                tokenLabel.setVisible(true);
                tokenLabel.setBorder(border);
                tokenLabel.addMouseListener(this);
                tokenLabel.addPossibleAction(action);
                correctionTokenLabels.add(tokenLabel);
                upgradePanel.add(tokenLabel);
            }

        }
        
        addButtons();

        //      repaint();
        revalidate();

    }

    public void clear() {
        clearPanel();
        addButtons();
        upgradePanel.repaint();
    }

    public void setSelectedTokenIndex(int index) {
        log.debug("Selected token index from " + selectedTokenIndex + " to "
                + index);
        selectedTokenIndex = index;
    }

    public void setSelectedToken() {
        if (tokenLabels == null || tokenLabels.isEmpty()) return;
        int index = -1;
        for (ActionLabel tokenLabel : tokenLabels) {
            tokenLabel.setBackground(++index == selectedTokenIndex
                    ? selectedLabelBgColour : defaultLabelBgColour);
        }
    }

    // NOTE: NOT USED
    private void setSelectedCorrectionToken() {
        if (correctionTokenLabels == null || correctionTokenLabels.isEmpty()) return;
        int index = -1;
        for (CorrectionTokenLabel tokenLabel : correctionTokenLabels) {
            tokenLabel.setBackground(++index == selectedTokenIndex
                    ? selectedLabelBgColour : defaultLabelBgColour);
        }
    }

    private BufferedImage getHexImage(int tileId) {
        return GameUIManager.getImageLoader().getTile(tileId, getZoomStep());
    }
    
    /**
     * @return Default zoom step for conventional panes or, for dockable panes,
     * the zoom step used in the map.
     * Map zoom step can only be used for dockable panes as user-based pane sizing
     * could be necessary when displaying tiles of an arbitrary size 
     */
    private int getZoomStep() {
        if (orUIManager.getORWindow().isDockingFrameworkEnabled()) {
            return hexMap.getZoomStep();
        } else {
            return UPGRADE_TILE_ZOOM_STEP;
        }
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

    public void addUpgrades(List<TileI> upgrades) {
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

    public <T extends LayToken> void setPossibleTokenLays(List<T> actions) {
        possibleTokenLays.clear();
        selectedTokenIndex = -1;
        if (actions != null) possibleTokenLays.addAll(actions);
    }

    public void setCancelText(String text) {
        cancelButton.setRailsIcon(RailsIcon.getByConfigKey(text));
    }

    public void setDoneText(String text) {
        doneButton.setRailsIcon(RailsIcon.getByConfigKey(text));
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
                orUIManager.tokenSelected((LayToken) ((ActionLabel) source).getPossibleActions().get(
                        0));
                setDoneEnabled(true);
            } else {
                orUIManager.tokenSelected(null);
            }
            setSelectedToken();
        } else if (correctionTokenMode) {
            int id = correctionTokenLabels.indexOf(source);
            selectedTokenIndex = id;
            log.info("Correction Token index = " + selectedTokenIndex + " selected");
        } else {

            int id = ((HexLabel) e.getSource()).getInternalId();

            orUIManager.tileSelected(id);
        }

    }

    public void mouseEntered(MouseEvent e) {
        Object source = e.getSource();
        if (!(source instanceof JLabel)) return;

        if (!tokenMode && !correctionTokenMode) {
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

        if (!tokenMode && !correctionTokenMode) {
            // tile mode
            HexLabel tile = (HexLabel) e.getSource();
            tile.setToolTipText(null);
        }
    }

    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

    public void finish() {
        setDoneEnabled(false);
        setCancelEnabled(false);
    }
    
    private void clearPanel() {
        upgradePanel.removeAll();
        if (omitButtons) {
            doneButton.setVisible(false);
            cancelButton.setVisible(false);
        }
    }
    
    private void addButtons() {
        if (omitButtons) {
            //only set externally managed buttons to visible if at least
            //one of them is enabled
            boolean isVisible = doneButton.isEnabled() || cancelButton.isEnabled();
            doneButton.setVisible(isVisible);
            cancelButton.setVisible(isVisible);
        } else {
            upgradePanel.add(doneButton);
            upgradePanel.add(cancelButton);
        }
    }
    
    public RailsIconButton[] getButtons() {
        return new RailsIconButton[] {doneButton, cancelButton};
    }

    /** ActionLabel extension that allows to attach the token */
    private class CorrectionTokenLabel extends ActionLabel {

        private static final long serialVersionUID = 1L;

        private TokenI token;

        CorrectionTokenLabel(Icon tokenIcon, TokenI token) {
            super(tokenIcon);
            this.token = token;
        }

    }

    /** JLabel extension to allow attaching the internal hex ID */
    private class HexLabel extends JLabel {

        private static final long serialVersionUID = 1L;

        String toolTip;
        int internalId;

        HexLabel(ImageIcon hexIcon, int internalId) {
            this(hexIcon,internalId,null);
        }
        
        HexLabel(ImageIcon hexIcon, int internalId, String toolTipHeaderLine) {
            super(hexIcon);
            this.internalId = internalId;
            this.setToolTip(toolTipHeaderLine);
        }
        
        int getInternalId() {
            return internalId;
        }

        public String getToolTip() {
            return toolTip;
        }

        void setTextFromTile(TileI tile) {
            StringBuffer text = new StringBuffer();
            if (rails.util.Util.hasValue(tile.getExternalId())) {
                text.append("<HTML><BODY>" + tile.getExternalId());
                if (tile.countFreeTiles() != -1) {
                    text.append("<BR> (" + tile.countFreeTiles() + ")");
                }
                text.append("</BODY></HTML>");
            }
            this.setText(text.toString());
        }

        protected void setToolTip() {
            setToolTip(null);
        }
        protected void setToolTip (String headerLine) {
            TileI currentTile = orUIManager.getGameUIManager().getGameManager().getTileManager().getTile(internalId);
            StringBuffer tt = new StringBuffer("<html>");
            if (headerLine != null && !headerLine.equals("")) {
                tt.append("<b><u>"+headerLine+"</u></b><br>");
            }
            tt.append("<b>Tile</b>: ").append(currentTile.getName()); // or
            // getId()
            if (currentTile.hasStations()) {
                // for (Station st : currentTile.getStations())
                int cityNumber = 0;
                // TileI has stations, but
                for (Station st : currentTile.getStations()) {
                    cityNumber++; // = city.getNumber();
                    tt.append("<br>  ").append(st.getType()).append(" ").append(
                            cityNumber) // .append("/").append(st.getNumber())
                            .append(": value ");
                    tt.append(st.getValue());
                    if (st.getBaseSlots() > 0) {
                        tt.append(", ").append(st.getBaseSlots()).append(
                        " slots");
                    }
                }
            }
            tt.append("</html>");
            toolTip = tt.toString();
        }

    }
}