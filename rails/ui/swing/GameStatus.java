package rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.apache.log4j.Logger;

import rails.common.GuiDef;
import rails.common.LocalText;
import rails.game.*;
import rails.game.action.*;
import rails.game.correct.CashCorrectionAction;
import rails.sound.SoundManager;
import rails.ui.swing.elements.*;
import rails.ui.swing.hexmap.HexHighlightMouseListener;
import rails.util.Util;

/**
 * This class is incorporated into StatusWindow and displays the bulk of
 * rails.game status information.
 */
public class GameStatus extends GridPanel implements ActionListener {
    private static final long serialVersionUID = 1L;

    protected static final String BUY_FROM_IPO_CMD = "BuyFromIPO";
    protected static final String BUY_FROM_POOL_CMD = "BuyFromPool";
    protected static final String SELL_CMD = "Sell";
    protected static final String CASH_CORRECT_CMD = "CorrectCash";

    protected StatusWindow parent;

    // Grid elements per function
    protected Field certPerPlayer[][];
    protected ClickField certPerPlayerButton[][];
    protected int certPerPlayerXOffset, certPerPlayerYOffset;
    protected Field certInIPO[];
    protected ClickField certInIPOButton[];
    protected int certInIPOXOffset, certInIPOYOffset;
    protected Field certInPool[];
    protected ClickField certInPoolButton[];
    protected int certInPoolXOffset, certInPoolYOffset;
    protected Field certInTreasury[];
    protected ClickField certInTreasuryButton[];
    protected int certInTreasuryXOffset, certInTreasuryYOffset;
    protected Field parPrice[];
    protected int parPriceXOffset, parPriceYOffset;
    protected Field currPrice[];
    protected int currPriceXOffset, currPriceYOffset;
    protected Field compCash[];
    protected ClickField compCashButton[];
    protected int compCashXOffset, compCashYOffset;
    protected Field compRevenue[];
    protected int compRevenueXOffset, compRevenueYOffset;
    protected Field compTrains[];
    protected int compTrainsXOffset, compTrainsYOffset;
    protected Field compTokens[];
    protected int compTokensXOffset, compTokensYOffset;
    protected Field compPrivates[];
    protected int compPrivatesXOffset, compPrivatesYOffset;
    protected Field compLoans[];
    protected int compLoansXOffset, compLoansYOffset;
    protected int rightsXOffset, rightsYOffset;
    protected Field rights[];
    protected Field playerCash[];
    protected ClickField playerCashButton[];
    protected int playerCashXOffset, playerCashYOffset;
    protected Field playerPrivates[];
    protected int playerPrivatesXOffset, playerPrivatesYOffset;
    protected Field playerWorth[];
    protected int playerWorthXOffset, playerWorthYOffset;
    protected Field playerORWorthIncrease[];
    protected int playerORWorthIncreaseXOffset, playerORWorthIncreaseYOffset;
    protected Field playerCertCount[];
    protected int playerCertCountXOffset, playerCertCountYOffset;
    protected int certLimitXOffset, certLimitYOffset;
    protected int phaseXOffset, phaseYOffset;
    protected Field bankCash;
    protected int bankCashXOffset, bankCashYOffset;
    protected Field poolTrains;
    protected int poolTrainsXOffset, poolTrainsYOffset;
    protected Field newTrains;
    protected int newTrainsXOffset, newTrainsYOffset;
    protected Field futureTrains;
    protected int futureTrainsXOffset, futureTrainsYOffset, futureTrainsWidth;
    protected int rightCompCaptionXOffset;

    protected Cell[] upperPlayerCaption;
    protected Cell[] lowerPlayerCaption;
    protected Caption treasurySharesCaption;

    protected Portfolio ipo, pool;

    protected GameUIManager gameUIManager;
    protected Bank bank;

    protected PossibleActions possibleActions = PossibleActions.getInstance();

    protected boolean hasParPrices = false;
    protected boolean compCanBuyPrivates = false;
    protected boolean compCanHoldOwnShares = false;
    protected boolean compCanHoldForeignShares = false; // NOT YET USED
    protected boolean hasCompanyLoans = false;
    protected boolean hasRights;

    // Current actor.
    // Players: 0, 1, 2, ...
    // Company (from treasury): -1.
    protected int actorIndex = -2;

    protected ButtonGroup buySellGroup = new ButtonGroup();
    /** Invisible default radio button option */
    protected ClickField dummyButton = new ClickField("", "", "", this, buySellGroup);

    protected Map<PublicCompanyI, Integer> companyIndex =
        new HashMap<PublicCompanyI, Integer>();

    protected static Logger log =
        Logger.getLogger(GameStatus.class.getPackage().getName());

    public GameStatus() {
        super();
    }

    public void init(StatusWindow parent, GameUIManager gameUIManager) {

        this.parent = parent;
        this.gameUIManager = gameUIManager;
        bank = gameUIManager.getGameManager().getBank();

        gridPanel = this;
        parentFrame = parent;

        gb = new GridBagLayout();
        this.setLayout(gb);
        UIManager.put("ToggleButton.select", buttonHighlight);

        gbc = new GridBagConstraints();
        setSize(800, 300);
        setLocation(0, 450);
        setBorder(BorderFactory.createEtchedBorder());
        setOpaque(false);

        players = gameUIManager.getPlayers();
        np = gameUIManager.getNumberOfPlayers();
        companies = gameUIManager.getAllPublicCompanies().toArray(new PublicCompanyI[0]);
        nc = companies.length;

        hasParPrices = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.HAS_ANY_PAR_PRICE);
        compCanBuyPrivates = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.CAN_ANY_COMPANY_BUY_PRIVATES);
        compCanHoldOwnShares = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.CAN_ANY_COMPANY_HOLD_OWN_SHARES);
        hasCompanyLoans = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.HAS_ANY_COMPANY_LOANS);
        hasRights = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.HAS_ANY_RIGHTS);

        ipo = bank.getIpo();
        pool = bank.getPool();

        certPerPlayer = new Field[nc][np];
        certPerPlayerButton = new ClickField[nc][np];
        certInIPO = new Field[nc];
        certInIPOButton = new ClickField[nc];
        certInPool = new Field[nc];
        certInPoolButton = new ClickField[nc];
        if (compCanHoldOwnShares) {
            certInTreasury = new Field[nc];
            certInTreasuryButton = new ClickField[nc];
        }
        parPrice = new Field[nc];
        currPrice = new Field[nc];
        compCash = new Field[nc];
        compCashButton = new ClickField[nc];
        compRevenue = new Field[nc];
        compTrains = new Field[nc];
        compTokens = new Field[nc];
        compPrivates = new Field[nc];
        compLoans = new Field[nc];
        if (hasRights) rights = new Field[nc];

        playerCash = new Field[np];
        playerCashButton = new ClickField[np];
        playerPrivates = new Field[np];
        playerWorth = new Field[np];
        playerORWorthIncrease = new Field[np];
        playerCertCount = new Field[np];
        upperPlayerCaption = new Cell[np];
        lowerPlayerCaption = new Cell[np];

        int lastX = 0;
        int lastY = 1;
        certPerPlayerXOffset = ++lastX;
        certPerPlayerYOffset = ++lastY;
        certInIPOXOffset = (lastX += np);
        certInIPOYOffset = lastY;
        certInPoolXOffset = ++lastX;
        certInPoolYOffset = lastY;
        if (compCanHoldOwnShares) {
            certInTreasuryXOffset = ++lastX;
            certInTreasuryYOffset = lastY;
        }
        if (hasParPrices) {
            parPriceXOffset = ++lastX;
            parPriceYOffset = lastY;
        }
        currPriceXOffset = ++lastX;
        currPriceYOffset = lastY;
        compCashXOffset = ++lastX;
        compCashYOffset = lastY;
        compRevenueXOffset = ++lastX;
        compRevenueYOffset = lastY;
        compTrainsXOffset = ++lastX;
        compTrainsYOffset = lastY;
        compTokensXOffset = ++lastX;
        compTokensYOffset = lastY;
        if (compCanBuyPrivates) {
            compPrivatesXOffset = ++lastX;
            compPrivatesYOffset = lastY;
        }
        if (hasCompanyLoans) {
            compLoansXOffset = ++lastX;
            compLoansYOffset = lastY;
        }
        if (hasRights) {
            rightsXOffset = ++lastX;
            rightsYOffset = lastY;
        }
        rightCompCaptionXOffset = ++lastX;

        playerCashXOffset = certPerPlayerXOffset;
        playerCashYOffset = (lastY += nc);
        playerPrivatesXOffset = certPerPlayerXOffset;
        playerPrivatesYOffset = ++lastY;
        playerWorthXOffset = certPerPlayerXOffset;
        playerWorthYOffset = ++lastY;
        playerORWorthIncreaseXOffset = certPerPlayerXOffset;
        playerORWorthIncreaseYOffset = ++lastY;
        playerCertCountXOffset = certPerPlayerXOffset;
        playerCertCountYOffset = ++lastY;
        certLimitXOffset = certInPoolXOffset;
        certLimitYOffset = playerCertCountYOffset;
        phaseXOffset = certInPoolXOffset + 2;
        phaseYOffset = playerCertCountYOffset;
        bankCashXOffset = certInPoolXOffset;
        bankCashYOffset = playerPrivatesYOffset;
        poolTrainsXOffset = bankCashXOffset + 2;
        poolTrainsYOffset = playerPrivatesYOffset;
        newTrainsXOffset = poolTrainsXOffset + 1;
        newTrainsYOffset = playerPrivatesYOffset;
        futureTrainsXOffset = newTrainsXOffset + 1;
        futureTrainsYOffset = playerPrivatesYOffset;
        futureTrainsWidth = rightCompCaptionXOffset - futureTrainsXOffset;

        fields = new JComponent[1+lastX][2+lastY];
        rowVisibilityObservers = new RowVisibility[nc];

        initFields();

    }

    protected void initFields () {

        MouseListener companyCaptionMouseClickListener = gameUIManager.getORUIManager().getORPanel().getCompanyCaptionMouseClickListener();

        // Top captions
        addField(new Caption(LocalText.getText("COMPANY")), 0, 0, 1, 2,
                WIDE_BOTTOM, true);
        addField(new Caption(LocalText.getText("PLAYERS")),
                certPerPlayerXOffset, 0, np, 1, WIDE_LEFT + WIDE_RIGHT, true);
        for (int i = 0; i < np; i++) {
            //if (playerOrderCanVary) {
            //    f = upperPlayerCaption[i] = new Field(gameUIManager.getGameManager().getPlayerNameModel(i));
            //    upperPlayerCaption[i].setNormalBgColour(Cell.NORMAL_CAPTION_BG_COLOUR);
            //} else {
            f = upperPlayerCaption[i] = new Caption(players.get(i).getNameAndPriority());
            log.debug(" GS: new player "+i+" is "+players.get(i).getName());
            //}
            int wideGapPosition = WIDE_BOTTOM +
            ((i==0)? WIDE_LEFT : 0) + ((i==np-1)? WIDE_RIGHT : 0);
            addField(f, certPerPlayerXOffset + i, 1, 1, 1, wideGapPosition, true);
        }
        addField(new Caption(LocalText.getText("BANK_SHARES")),
                certInIPOXOffset, 0, 2, 1, WIDE_RIGHT, true);
        addField(new Caption(LocalText.getText("IPO")), certInIPOXOffset, 1, 1,
                1, WIDE_BOTTOM, true);
        addField(new Caption(LocalText.getText("POOL")), certInPoolXOffset, 1,
                1, 1, WIDE_RIGHT + WIDE_BOTTOM, true);

        if (compCanHoldOwnShares) {
            addField(treasurySharesCaption =
                new Caption(LocalText.getText("TREASURY_SHARES")),
                certInTreasuryXOffset, 0, 1, 2, WIDE_RIGHT + WIDE_BOTTOM, true);
        }

        if (this.hasParPrices) {
            addField(new Caption(LocalText.getText("PRICES")), parPriceXOffset,
                    0, 2, 1, WIDE_RIGHT, true);
            addField(new Caption(LocalText.getText("PAR")), parPriceXOffset, 1,
                    1, 1, WIDE_BOTTOM, true);
            addField(new Caption(LocalText.getText("CURRENT")),
                    currPriceXOffset, 1, 1, 1, WIDE_RIGHT + WIDE_BOTTOM, true);
        } else {
            addField(new Caption(LocalText.getText("CURRENT_PRICE")),
                    currPriceXOffset, 0, 1, 2, WIDE_RIGHT + WIDE_BOTTOM, true);

        }
        addField(new Caption(LocalText.getText("COMPANY_DETAILS")),
                compCashXOffset, 0, 4 + (compCanBuyPrivates ? 1 : 0)
                + (hasCompanyLoans ? 1 : 0), 1, 0, true);
        addField(new Caption(LocalText.getText("CASH")), compCashXOffset, 1, 1,
                1, WIDE_BOTTOM, true);
        addField(new Caption(LocalText.getText("REVENUE")), compRevenueXOffset,
                1, 1, 1, WIDE_BOTTOM, true);
        addField(new Caption(LocalText.getText("TRAINS")), compTrainsXOffset,
                1, 1, 1, WIDE_BOTTOM, true);
        addField(new Caption(LocalText.getText("TOKENS")), compTokensXOffset,
                1, 1, 1, WIDE_BOTTOM, true);
        if (compCanBuyPrivates) {
            addField(new Caption(LocalText.getText("PRIVATES")),
                    compPrivatesXOffset, 1, 1, 1, WIDE_BOTTOM, true);
        }
        if (hasCompanyLoans) {
            addField (new Caption (LocalText.getText("LOANS")),
                    compLoansXOffset, 1, 1, 1, WIDE_BOTTOM, true);
        }
        if (hasRights) {
            addField (new Caption(LocalText.getText("RIGHTS")),
                    rightsXOffset, 1, 1, 1, WIDE_BOTTOM, true);
        }

        addField(new Caption(LocalText.getText("COMPANY")),
                rightCompCaptionXOffset, 0, 1, 2, WIDE_LEFT + WIDE_BOTTOM, true);

        for (int i = 0; i < nc; i++) {
            c = companies[i];
            companyIndex.put(c, new Integer(i));
            rowVisibilityObservers[i]
                                   = new RowVisibility (this, certPerPlayerYOffset + i, c.getInGameModel(), false);
            boolean visible = rowVisibilityObservers[i].lastValue();

            f = new Caption(c.getName());
            f.setForeground(c.getFgColour());
            f.setBackground(c.getBgColour());
            HexHighlightMouseListener.addMouseListener(f,
                    gameUIManager.getORUIManager(),c,false);
            f.addMouseListener(companyCaptionMouseClickListener);
            f.setToolTipText(LocalText.getText("NetworkInfoDialogTitle",c.getName()));
            addField(f, 0, certPerPlayerYOffset + i, 1, 1, 0, visible);

            for (int j = 0; j < np; j++) {
                f =
                    certPerPlayer[i][j] =
                        new Field(
                                players.get(j).getPortfolio().getShareModel(
                                        c));
                int wideGapPosition = ((j==0)? WIDE_LEFT : 0) + ((j==np-1)? WIDE_RIGHT : 0);
                addField(f, certPerPlayerXOffset + j, certPerPlayerYOffset + i,
                        1, 1, wideGapPosition, visible);
                f =
                    certPerPlayerButton[i][j] =
                        new ClickField("", SELL_CMD,
                                LocalText.getText("ClickForSell"),
                                this, buySellGroup);
                addField(f, certPerPlayerXOffset + j, certPerPlayerYOffset + i,
                        1, 1, wideGapPosition, false);
            }
            f = certInIPO[i] = new Field(ipo.getShareModel(c));
            addField(f, certInIPOXOffset, certInIPOYOffset + i, 1, 1, 0, visible);
            f =
                certInIPOButton[i] =
                    new ClickField(
                            certInIPO[i].getText(),
                            BUY_FROM_IPO_CMD,
                            LocalText.getText("ClickToSelectForBuying"),
                            this, buySellGroup);
            addField(f, certInIPOXOffset, certInIPOYOffset + i, 1, 1, 0, false);

            //no size alignment as button size could also be smaller than the field's one
            //certInIPO[i].setPreferredSize(certInIPOButton[i].getPreferredSize());

            f = certInPool[i] = new Field(pool.getShareModel(c));
            addField(f, certInPoolXOffset, certInPoolYOffset + i, 1, 1,
                    WIDE_RIGHT, visible);
            f =
                certInPoolButton[i] =
                    new ClickField(
                            certInPool[i].getText(),
                            BUY_FROM_POOL_CMD,
                            LocalText.getText("ClickToSelectForBuying"),
                            this, buySellGroup);
            addField(f, certInPoolXOffset, certInPoolYOffset + i, 1, 1,
                    WIDE_RIGHT, false);
            //no size alignment as button size could also be smaller than the field's one
            //certInPool[i].setPreferredSize(certInIPOButton[i].getPreferredSize());/* sic */

            if (compCanHoldOwnShares) {
                f =
                    certInTreasury[i] =
                        new Field(c.getPortfolio().getShareModel(c));
                addField(f, certInTreasuryXOffset, certInTreasuryYOffset + i,
                        1, 1, WIDE_RIGHT, visible);
                f =
                    certInTreasuryButton[i] =
                        new ClickField(
                                certInTreasury[i].getText(),
                                BUY_FROM_POOL_CMD,
                                LocalText.getText("ClickForSell"),
                                this, buySellGroup);
                addField(f, certInTreasuryXOffset, certInTreasuryYOffset + i,
                        1, 1, WIDE_RIGHT, false);
                certInTreasury[i].setPreferredSize(certInTreasuryButton[i].getPreferredSize());/* sic */
            }

            if (this.hasParPrices) {
                f = parPrice[i] = new Field(c.getParPriceModel());
                addField(f, parPriceXOffset, parPriceYOffset + i, 1, 1, 0, visible);
            }

            f = currPrice[i] = new Field(c.getCurrentPriceModel());
            addField(f, currPriceXOffset, currPriceYOffset + i, 1, 1,
                    WIDE_RIGHT, visible);

            f = compCash[i] = new Field(c.getCashModel());
            addField(f, compCashXOffset, compCashYOffset + i, 1, 1, 0, visible);
            f =
                compCashButton[i] =
                    new ClickField(
                            compCash[i].getText(),
                            CASH_CORRECT_CMD,
                            LocalText.getText("CorrectCashToolTip"),
                            this, buySellGroup);
            addField(f, compCashXOffset, compCashYOffset + i, 1, 1,
                    WIDE_RIGHT, false);

            f = compRevenue[i] = new Field(c.getLastRevenueModel());
            addField(f, compRevenueXOffset, compRevenueYOffset + i, 1, 1, 0, visible);

            f = compTrains[i] = new Field(c.getPortfolio().getTrainsModel());
            addField(f, compTrainsXOffset, compTrainsYOffset + i, 1, 1, 0, visible);

            f = compTokens[i] = new Field(c.getBaseTokensModel());
            addField(f, compTokensXOffset, compTokensYOffset + i, 1, 1, 0, visible);

            if (this.compCanBuyPrivates) {
                f =
                    compPrivates[i] =
                        new Field(
                                c.getPortfolio().getPrivatesOwnedModel());
                HexHighlightMouseListener.addMouseListener(f,
                        gameUIManager.getORUIManager(),
                        c.getPortfolio());
                addField(f, compPrivatesXOffset, compPrivatesYOffset + i, 1, 1,
                        0, visible);
            }
            if (hasCompanyLoans) {
                if (c.getLoanValueModel() != null) {
                    f = compLoans[i] = new Field (c.getLoanValueModel());
                } else {
                    f = compLoans[i] = new Field ("");
                }
                addField (f, compLoansXOffset, compLoansYOffset+i, 1, 1, 0, visible);
            }

            if (hasRights) {
                f = rights[i] = new Field (c.getRightsModel());
                addField (f, rightsXOffset, rightsYOffset + i, 1, 1, 0, visible);
            }

            f = new Caption(c.getName());
            f.setForeground(c.getFgColour());
            f.setBackground(c.getBgColour());
            HexHighlightMouseListener.addMouseListener(f,
                    gameUIManager.getORUIManager(),c,false);
            f.addMouseListener(companyCaptionMouseClickListener);
            f.setToolTipText(LocalText.getText("NetworkInfoDialogTitle",c.getName()));
            addField(f, rightCompCaptionXOffset, certPerPlayerYOffset + i, 1,
                    1, WIDE_LEFT, visible);
        }

        // Player possessions
        addField(new Caption(LocalText.getText("CASH")), 0, playerCashYOffset,
                1, 1, WIDE_TOP , true);
        for (int i = 0; i < np; i++) {
            f = playerCash[i] = new Field(players.get(i).getCashModel());
            int wideGapPosition = WIDE_TOP +
            ((i==0)? WIDE_LEFT : 0) + ((i==np-1)? WIDE_RIGHT : 0);
            addField(f, playerCashXOffset + i, playerCashYOffset, 1, 1,
                    wideGapPosition, true);
            f =
                playerCashButton[i] =
                    new ClickField(
                            playerCash[i].getText(),
                            CASH_CORRECT_CMD,
                            LocalText.getText("CorrectCashToolTip"),
                            this, buySellGroup);
            addField(f, playerCashXOffset + i, playerCashYOffset, 1, 1,
                    wideGapPosition, false);
        }

        addField(new Caption(LocalText.getText("PRIVATES")), 0, playerPrivatesYOffset, 1, 1,
                0, true);
        for (int i = 0; i < np; i++) {
            f =
                playerPrivates[i] =
                    new Field(
                            players.get(i).getPortfolio().getPrivatesOwnedModel());
            HexHighlightMouseListener.addMouseListener(f,
                    gameUIManager.getORUIManager(),
                    players.get(i).getPortfolio());
            int wideGapPosition = ((i==0)? WIDE_LEFT : 0) + ((i==np-1)? WIDE_RIGHT : 0);
            addField(f, playerPrivatesXOffset + i, playerPrivatesYOffset, 1, 1,
                    wideGapPosition, true);
        }

        addField(new Caption(LocalText.getText("WORTH")), 0,
                playerWorthYOffset, 1, 1, 0, true);
        for (int i = 0; i < np; i++) {
            f = playerWorth[i] = new Field(players.get(i).getWorthModel());
            int wideGapPosition = ((i==0)? WIDE_LEFT : 0) + ((i==np-1)? WIDE_RIGHT : 0);
            addField(f, playerWorthXOffset + i, playerWorthYOffset, 1, 1, wideGapPosition, true);
        }

        addField(new Caption(LocalText.getText("ORWORTHINCR")), 0,
                playerORWorthIncreaseYOffset, 1, 1, 0, true);
        for (int i = 0; i < np; i++) {
            f = playerORWorthIncrease[i] = new Field(players.get(i).getLastORWorthIncrease());
            int wideGapPosition = ((i==0)? WIDE_LEFT : 0) + ((i==np-1)? WIDE_RIGHT : 0);
            addField(f, playerORWorthIncreaseXOffset + i, playerORWorthIncreaseYOffset, 1, 1, wideGapPosition, true);
        }

        addField(new Caption("Certs"), 0, playerCertCountYOffset, 1, 1,
                WIDE_TOP, true);
        for (int i = 0; i < np; i++) {
            f =
                playerCertCount[i] =
                    new Field(players.get(i).getCertCountModel(), false, true);
            int wideGapPosition = WIDE_TOP +
            ((i==0)? WIDE_LEFT : 0) + ((i==np-1)? WIDE_RIGHT : 0);
            addField(f, playerCertCountXOffset + i, playerCertCountYOffset, 1,
                    1, wideGapPosition, true);
        }

        // Bottom player captions
        for (int i = 0; i < np; i++) {
            //if (playerOrderCanVary) {
            //    f = lowerPlayerCaption[i] = new Field(gameUIManager.getGameManager().getPlayerNameModel(i));
            //    lowerPlayerCaption[i].setNormalBgColour(Cell.NORMAL_CAPTION_BG_COLOUR);
            //} else {
            f = lowerPlayerCaption[i] = new Caption(players.get(i).getNameAndPriority());
            //}
            int wideGapPosition = WIDE_TOP +
            ((i==0)? WIDE_LEFT : 0) + ((i==np-1)? WIDE_RIGHT : 0);
            addField(f, i + 1, playerCertCountYOffset + 1, 1, 1, wideGapPosition, true);
        }

        // Certificate Limit
        addField(new Caption(LocalText.getText("LIMIT")), certLimitXOffset - 1,
                certLimitYOffset, 1, 1, WIDE_TOP, true);
        addField(new Field(gameUIManager.getGameManager().getPlayerCertificateLimitModel()),
                certLimitXOffset,
                certLimitYOffset, 1, 1, WIDE_TOP + WIDE_RIGHT, true);

        // Phase
        addField(new Caption(LocalText.getText("PHASE")), phaseXOffset - 1,
                phaseYOffset, 1, 1, WIDE_TOP, true);
        addField(new Field(gameUIManager.getGameManager().getPhaseManager().getCurrentPhaseModel()),
                phaseXOffset,
                phaseYOffset, 1, 1, WIDE_TOP, true);

        // Bank
        addField(new Caption(LocalText.getText("BANK")), bankCashXOffset - 1,
                bankCashYOffset - 1, 1, 2, WIDE_TOP, true);
        addField(new Caption(LocalText.getText("CASH")), bankCashXOffset,
                bankCashYOffset - 1, 1, 1, WIDE_TOP + WIDE_RIGHT, true);
        bankCash = new Field(bank.getCashModel());
        addField(bankCash, bankCashXOffset, bankCashYOffset, 1, 1, WIDE_RIGHT, true);

        // Trains
        addField(new Caption(LocalText.getText("TRAINS")),
                poolTrainsXOffset - 1, poolTrainsYOffset - 1, 1, 2, WIDE_TOP, true);
        addField(new Caption(LocalText.getText("USED")), poolTrainsXOffset,
                poolTrainsYOffset - 1, 1, 1, WIDE_TOP, true);
        poolTrains = new Field(pool.getTrainsModel());
        addField(poolTrains, poolTrainsXOffset, poolTrainsYOffset, 1, 1, 0, true);

        // New trains
        addField(new Caption(LocalText.getText("NEW")), newTrainsXOffset,
                newTrainsYOffset - 1, 1, 1, WIDE_TOP, true);
        newTrains = new Field(ipo.getTrainsModel());
        addField(newTrains, newTrainsXOffset, newTrainsYOffset, 1, 1, 0, true);

        // Future trains
        addField(new Caption(LocalText.getText("Future")), futureTrainsXOffset,
                futureTrainsYOffset - 1, futureTrainsWidth, 1, WIDE_TOP, true);
        futureTrains = new Field(bank.getUnavailable().getTrainsModel(), true, false);
        futureTrains.setPreferredSize(new Dimension (1,1)); // To enable auto word wrap
        addField(futureTrains, futureTrainsXOffset, futureTrainsYOffset,
                futureTrainsWidth, 1, 0, true);

        // dummy field for nice rendering of table borders
        addField (new Caption(""), certInIPOXOffset, newTrainsYOffset + 1,
                poolTrainsXOffset - certInIPOXOffset, 2, 0, true);

        // Train cost overview
        String text = gameUIManager.getGameManager().getTrainManager().getTrainCostOverview();
        addField (f = new Caption("<html>" + text + "</html>"), poolTrainsXOffset, newTrainsYOffset + 1,
                futureTrainsWidth + 2, 2, 0, true);
        f.setPreferredSize(new Dimension (1,1));// To enable auto word wrap
    }

    public void recreate() {
        log.debug("GameStatus.recreate() called");

        // Remove old fields. Don't forget to deregister the Observers
        deRegisterObservers();
        removeAll();

        // Create new fields
        initFields();

        //repaint();
    }

    public void updatePlayerOrder (List<String> newPlayerNames) {

        List<String> oldPlayerNames = gameUIManager.getCurrentGuiPlayerNames();
        log.debug("GS: old player list: "+Util.joinWithDelimiter(oldPlayerNames.toArray(new String[0]), ","));
        log.debug("GS: new player list: "+Util.joinWithDelimiter(newPlayerNames.toArray(new String[0]), ","));

        /* Currently, the passed new player order is ignored.
         * A call to this method only serves as a signal to rebuild the player columns in the proper order
         * (in fact, the shortcut is taken to rebuild the whole GameStatus panel).
         * For simplicity reasons, the existing reference to the (updated)
         * players list in GameManager is used.
         * 
         * In the future (e.g. when implementing a client/server split),
         * newPlayerNames may actually become to be used to reorder the
         * (then internal) UI player list.
         */
        recreate();
        gameUIManager.packAndApplySizing(parent);
    }

    public void actionPerformed(ActionEvent actor) {
        JComponent source = (JComponent) actor.getSource();
        List<PossibleAction> actions;
        PossibleAction chosenAction = null;

        if (source instanceof ClickField) {
            gbc = gb.getConstraints(source);
            actions = ((ClickField) source).getPossibleActions();

            //notify sound manager that click field has been selected
            SoundManager.notifyOfClickFieldSelection(actions.get(0));

            // Assume that we will have either sell or buy actions
            // under one ClickField, not both. This seems guaranteed.
            log.debug("Action is " + actions.get(0).toString());

            if (actions == null || actions.size() == 0) {

                log.warn("No ClickField action found");

            } else if (actions.get(0) instanceof SellShares) {

                List<String> options = new ArrayList<String>();
                List<SellShares> sellActions = new ArrayList<SellShares>();
                List<Integer> sellAmounts = new ArrayList<Integer>();
                SellShares sale;
                for (PossibleAction action : actions) {
                    sale = (SellShares) action;

                    //for (int i = 1; i <= sale.getMaximumNumber(); i++) {
                    int i = sale.getNumber();
                    if (sale.getPresidentExchange() == 0) {
                        options.add(LocalText.getText("SellShares",
                                i,
                                sale.getShare(),
                                i * sale.getShare(),
                                sale.getCompanyName(),
                                Bank.format(i * sale.getShareUnits()
                                        * sale.getPrice()) ));
                    } else {
                        options.add(LocalText.getText("SellSharesWithSwap",
                                i,
                                sale.getShare(),
                                i * sale.getShare(),
                                sale.getCompanyName(),
                                Bank.format(i * sale.getShareUnits() * sale.getPrice()),
                                // disregard other than double pres.certs. This is for 1835 only.
                                3 - sale.getPresidentExchange(),
                                sale.getPresidentExchange() * sale.getShareUnit()));
                    }
                    sellActions.add(sale);
                    sellAmounts.add(i);
                }
                int index = 0;
                if (options.size() > 1) {
                    String message = LocalText.getText("PleaseSelect");
                    String sp =
                        (String) JOptionPane.showInputDialog(this, message,
                                message, JOptionPane.QUESTION_MESSAGE,
                                null, options.toArray(new String[0]),
                                options.get(0));
                    index = options.indexOf(sp);
                } else if (options.size() == 1) {
                    String message = LocalText.getText("PleaseConfirm");
                    int result =
                        JOptionPane.showConfirmDialog(this, options.get(0),
                                message, JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                    index = (result == JOptionPane.OK_OPTION ? 0 : -1);
                }
                if (index < 0) {
                    // cancelled
                } else {
                    chosenAction = sellActions.get(index);
                    //((SellShares) chosenAction).setNumberSold(sellAmounts.get(index));
                }
            } else if (actions.get(0) instanceof BuyCertificate) {
                boolean startCompany = false;

                List<String> options = new ArrayList<String>();
                List<BuyCertificate> buyActions =
                    new ArrayList<BuyCertificate>();
                List<Integer> buyAmounts = new ArrayList<Integer>();
                BuyCertificate buy;
                PublicCertificateI cert;
                String companyName = "";
                String playerName = "";
                int sharePerCert;
                int sharesPerCert;
                int shareUnit;

                for (PossibleAction action : actions) {
                    buy = (BuyCertificate) action;
                    //cert = buy.getCertificate();
                    playerName = buy.getPlayerName ();
                    PublicCompanyI company = buy.getCompany();
                    companyName = company.getName();
                    sharePerCert = buy.getSharePerCertificate();
                    shareUnit = company.getShareUnit();
                    sharesPerCert = sharePerCert / shareUnit;

                    if (buy instanceof StartCompany) {

                        startCompany = true;
                        int[] startPrices;
                        if (((StartCompany) buy).mustSelectAPrice()) {
                            startPrices =
                                ((StartCompany) buy).getStartPrices();
                            Arrays.sort(startPrices);
                            if (startPrices.length > 1) {
                                for (int i = 0; i < startPrices.length; i++) {
                                    options.add(LocalText.getText("StartCompany",
                                            Bank.format(startPrices[i]),
                                            sharePerCert,
                                            Bank.format(sharesPerCert * startPrices[i]) ));
                                    buyActions.add(buy);
                                    buyAmounts.add(startPrices[i]);
                                }
                            } else {
                                options.add (LocalText.getText("StartACompany",
                                        companyName,
                                        company.getPresidentsShare().getShare(),
                                        Bank.format(company.getPresidentsShare().getShares() * startPrices[0])));
                                buyActions.add(buy);
                                buyAmounts.add(startPrices[0]);
                            }
                        } else {
                            startPrices = new int[] {((StartCompany) buy).getPrice()};
                            options.add(LocalText.getText("StartCompanyFixed",
                                    companyName,
                                    sharePerCert,
                                    Bank.format(startPrices[0]) ));
                            buyActions.add(buy);
                            buyAmounts.add(startPrices[0]);
                        }

                    } else {

                        options.add(LocalText.getText("BuyCertificate",
                                sharePerCert,
                                companyName,
                                buy.getFromPortfolio().getName(),
                                Bank.format(sharesPerCert * buy.getPrice()) ));
                        buyActions.add(buy);
                        buyAmounts.add(1);
                        for (int i = 2; i <= buy.getMaximumNumber(); i++) {
                            options.add(LocalText.getText("BuyCertificates",
                                    i,
                                    sharePerCert,
                                    companyName,
                                    buy.getFromPortfolio().getName(),
                                    Bank.format(i * sharesPerCert
                                            * buy.getPrice()) ));
                            buyActions.add(buy);
                            buyAmounts.add(i);
                        }
                    }
                }
                int index = 0;
                if (options.size() > 1) {
                    if (startCompany) {
                        RadioButtonDialog dialog = new RadioButtonDialog (
                                GameUIManager.COMPANY_START_PRICE_DIALOG,
                                gameUIManager,
                                parent,
                                LocalText.getText("PleaseSelect"),
                                LocalText.getText("WHICH_START_PRICE",
                                        playerName,
                                        companyName),
                                        options.toArray(new String[0]), -1);
                        gameUIManager.setCurrentDialog(dialog, actions.get(0));
                        parent.disableButtons();
                        return;
                    } else {
                        String sp =
                            (String) JOptionPane.showInputDialog(this,
                                    LocalText.getText(startCompany
                                            ? "WHICH_PRICE"
                                                    : "HOW_MANY_SHARES"),
                                                    LocalText.getText("PleaseSelect"),
                                                    JOptionPane.QUESTION_MESSAGE, null,
                                                    options.toArray(new String[0]),
                                                    options.get(0));
                        index = options.indexOf(sp);
                    }
                } else if (options.size() == 1) {
                    int result =
                        JOptionPane.showConfirmDialog(this, options.get(0),
                                LocalText.getText("PleaseConfirm"),
                                JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                    index = (result == JOptionPane.OK_OPTION ? 0 : -1);
                }
                if (index < 0) {
                    // cancelled
                } else if (startCompany) {
                    chosenAction = buyActions.get(index);
                    ((StartCompany) chosenAction).setStartPrice(buyAmounts.get(index));
                    ((StartCompany) chosenAction).setNumberBought(((StartCompany) chosenAction).getSharesPerCertificate());
                } else {
                    chosenAction = buyActions.get(index);
                    ((BuyCertificate) chosenAction).setNumberBought(buyAmounts.get(index));
                }
            } else if (actions.get(0) instanceof CashCorrectionAction) {
                CashCorrectionAction cca = (CashCorrectionAction)actions.get(0);
                String amountString = (String) JOptionPane.showInputDialog(this,
                        LocalText.getText("CorrectCashDialogMessage", cca.getCashHolderName()),
                        LocalText.getText("CorrectCashDialogTitle"),
                        JOptionPane.QUESTION_MESSAGE, null, null, 0);
                if (amountString.substring(0,1).equals("+"))
                    amountString = amountString.substring(1);
                int amount;
                try {
                    amount = Integer.parseInt(amountString);
                } catch (NumberFormatException e) {
                    amount = 0;
                }
                cca.setAmount(amount);
                chosenAction = cca;
            } else {

                chosenAction =
                    processGameSpecificActions(actor, actions.get(0));

            }
        } else {
            log.warn("Action from unknown source: " + source.toString());
        }

        chosenAction = processGameSpecificFollowUpActions(actor, chosenAction);

        if (chosenAction != null)
            (parent).process(chosenAction);

        repaint();

    }

    /** Stub allowing game-specific extensions */
    protected PossibleAction processGameSpecificActions(ActionEvent actor,
            PossibleAction chosenAction) {
        return chosenAction;
    }

    protected PossibleAction processGameSpecificFollowUpActions(
            ActionEvent actor, PossibleAction chosenAction) {
        return chosenAction;
    }

    public void initTurn(int actorIndex, boolean myTurn) {
        int i, j;

        dummyButton.setSelected(true);

        for (i = 0; i < nc; i++) {
            setIPOCertButton(i, false);
            setPoolCertButton(i, false);
            for (j=0; j < np; j++) setPlayerCertButton (i, j, false);
            if (compCanHoldOwnShares) setTreasuryCertButton(i, false);
        }

        this.actorIndex = actorIndex;

        highlightCurrentPlayer(this.actorIndex);
        if (treasurySharesCaption != null) treasurySharesCaption.setHighlight(actorIndex == -1);

        // Set new highlights
        if ((j = this.actorIndex) >= -1) {

            if (myTurn) {

                PublicCompanyI company;
                Portfolio holder;
                int index;
                CashHolder owner;

                List<BuyCertificate> buyableCerts =
                    possibleActions.getType(BuyCertificate.class);
                if (buyableCerts != null) {
                    for (BuyCertificate bCert : buyableCerts) {
                        company = bCert.getCompany();
                        index = company.getPublicNumber();
                        holder = bCert.getFromPortfolio();
                        owner = holder.getOwner();
                        if (holder == ipo) {
                            setIPOCertButton(index, true, bCert);
                        } else if (holder == pool) {
                            setPoolCertButton(index, true, bCert);
                        } else if (owner instanceof Player) {
                            setPlayerCertButton(index, ((Player)owner).getIndex(), true, bCert);
                        } else if (owner instanceof PublicCompanyI && compCanHoldOwnShares) {
                            setTreasuryCertButton(index, true, bCert);
                        }
                    }
                }

                List<SellShares> sellableShares =
                    possibleActions.getType(SellShares.class);
                if (sellableShares != null) {
                    for (SellShares share : sellableShares) {
                        company = share.getCompany();
                        index = company.getPublicNumber();
                        if (j >= 0) {
                            setPlayerCertButton(index, j, true, share);
                        } else if (j == -1 && compCanHoldOwnShares) {
                            setTreasuryCertButton(index, true, share);
                        }
                    }
                }

                initGameSpecificActions();

                List<NullAction> nullActions =
                    possibleActions.getType(NullAction.class);
                if (nullActions != null) {
                    for (NullAction na : nullActions) {
                        (parent).setPassButton(na);
                    }
                }
            }
        }

        repaint();
    }

    /** Stub, can be overridden by game-specific subclasses */
    protected void initGameSpecificActions() {

    }

    /**
     * Initializes the CashCorrectionActions
     */
    public boolean initCashCorrectionActions() {

        // Clear all buttons
        for (int i = 0; i < nc; i++) {
            setCompanyCashButton(i, false, null);
        }
        for (int j = 0; j < np; j++) {
            setPlayerCashButton(j, false, null);
        }

        List<CashCorrectionAction> actions =
            possibleActions.getType(CashCorrectionAction.class);

        if (actions != null) {
            for (CashCorrectionAction a : actions) {
                CashHolder ch = a.getCashHolder();
                if (ch instanceof PublicCompanyI) {
                    PublicCompanyI pc = (PublicCompanyI)ch;
                    int i = pc.getPublicNumber();
                    setCompanyCashButton(i, true, a);
                }
                if (ch instanceof Player) {
                    Player p = (Player)ch;
                    int i = players.indexOf(p);
                    setPlayerCashButton(i, true, a);
                }
            }
        }

        return (actions != null && !actions.isEmpty());

    }

    public void setPriorityPlayer(int index) {

        for (int j = 0; j < np; j++) {
            String playerNameAndPriority = players.get(j).getName() + (j == index ? " PD" : "");
            upperPlayerCaption[j].setText(playerNameAndPriority);
            lowerPlayerCaption[j].setText(playerNameAndPriority);
        }
    }

    public void highlightCurrentPlayer (int index) {
        for (int j = 0; j < np; j++) {
            upperPlayerCaption[j].setHighlight(j == index);
            lowerPlayerCaption[j].setHighlight(j == index);
        }
    }

    public void highlightLocalPlayer (int index) {
        for (int j = 0; j < np; j++) {
            upperPlayerCaption[j].setLocalPlayer(j == index);
            lowerPlayerCaption[j].setLocalPlayer(j == index);
        }
    }

    public String getSRPlayer() {
        if (actorIndex >= 0)
            return players.get(actorIndex).getName();
        else
            return "";
    }

    protected void setPlayerCertButton(int i, int j, boolean clickable, Object o) {

        if (j < 0) return;
        setPlayerCertButton(i, j, clickable);
        if (clickable) syncToolTipText (certPerPlayer[i][j], certPerPlayerButton[i][j]);
        if (clickable && o != null) {
            if (o instanceof PossibleAction) {
                certPerPlayerButton[i][j].addPossibleAction((PossibleAction) o);
                if (o instanceof SellShares) {
                    addToolTipText (certPerPlayerButton[i][j], LocalText.getText("ClickForSell"));
                } else if (o instanceof BuyCertificate) {
                    addToolTipText (certPerPlayerButton[i][j], LocalText.getText("ClickToSelectForBuying"));
                }
            }
        }
    }

    protected void setPlayerCertButton(int i, int j, boolean clickable) {
        if (j < 0) return;
        boolean visible = rowVisibilityObservers[i].lastValue();

        if (clickable) {
            certPerPlayerButton[i][j].setText(certPerPlayer[i][j].getText());
            syncToolTipText (certPerPlayer[i][j], certPerPlayerButton[i][j]);
        } else {
            certPerPlayerButton[i][j].clearPossibleActions();
        }
        certPerPlayer[i][j].setVisible(visible && !clickable);
        certPerPlayerButton[i][j].setVisible(visible && clickable);
    }

    protected void setIPOCertButton(int i, boolean clickable, Object o) {

        setIPOCertButton(i, clickable);
        if (clickable) syncToolTipText (certInIPO[i], certInIPOButton[i]);
        if (clickable && o != null) {
            if (o instanceof PossibleAction)
                certInIPOButton[i].addPossibleAction((PossibleAction) o);
        }
    }

    protected void setIPOCertButton(int i, boolean clickable) {
        boolean visible = rowVisibilityObservers[i].lastValue();
        if (clickable) {
            certInIPOButton[i].setText(certInIPO[i].getText());
            syncToolTipText (certInIPO[i], certInIPOButton[i]);
        } else {
            certInIPOButton[i].clearPossibleActions();
        }
        certInIPO[i].setVisible(visible && !clickable);
        certInIPOButton[i].setVisible(visible && clickable);
    }

    protected void setPoolCertButton(int i, boolean clickable, Object o) {

        setPoolCertButton(i, clickable);
        if (clickable) syncToolTipText (certInPool[i], certInPoolButton[i]);
        if (clickable && o != null) {
            if (o instanceof PossibleAction)
                certInPoolButton[i].addPossibleAction((PossibleAction) o);
        }
    }

    protected void setPoolCertButton(int i, boolean clickable) {
        boolean visible = rowVisibilityObservers[i].lastValue();
        if (clickable) {
            certInPoolButton[i].setText(certInPool[i].getText());
            syncToolTipText (certInIPO[i], certInIPOButton[i]);
        } else {
            certInPoolButton[i].clearPossibleActions();
        }
        certInPool[i].setVisible(visible && !clickable);
        certInPoolButton[i].setVisible(visible && clickable);
    }

    protected void setTreasuryCertButton(int i, boolean clickable, Object o) {

        setTreasuryCertButton(i, clickable);
        if (clickable && o != null) {
            if (clickable) syncToolTipText (certInTreasury[i], certInTreasuryButton[i]);
            if (o instanceof PossibleAction)
                certInTreasuryButton[i].addPossibleAction((PossibleAction) o);
        }
    }

    protected void setTreasuryCertButton(int i, boolean clickable) {
        boolean visible = rowVisibilityObservers[i].lastValue();
        if (clickable) {
            certInTreasuryButton[i].setText(certInTreasury[i].getText());
            syncToolTipText (certInTreasury[i], certInTreasuryButton[i]);
        } else {
            certInTreasuryButton[i].clearPossibleActions();
        }
        certInTreasury[i].setVisible(visible && !clickable);
        certInTreasuryButton[i].setVisible(clickable);
    }

    protected void setCompanyCashButton(int i, boolean clickable, PossibleAction action){
        boolean visible = rowVisibilityObservers[i].lastValue();

        if (clickable) {
            compCashButton[i].setText(compCash[i].getText());
        } else {
            compCashButton[i].clearPossibleActions();
        }
        compCash[i].setVisible(visible && !clickable);
        compCashButton[i].setVisible(visible && clickable);
        if (action != null)
            compCashButton[i].addPossibleAction(action);
    }

    protected void setPlayerCashButton(int i, boolean clickable, PossibleAction action){

        if (clickable) {
            playerCashButton[i].setText(playerCash[i].getText());
        } else {
            playerCashButton[i].clearPossibleActions();
        }
        playerCash[i].setVisible(!clickable);
        playerCashButton[i].setVisible(clickable);

        if (action != null)
            playerCashButton[i].addPossibleAction(action);
    }

    protected void syncToolTipText (Field field, ClickField clickField) {
        String baseText = field.getToolTipText();
        clickField.setToolTipText(Util.hasValue(baseText) ? baseText : null);
    }

    protected void addToolTipText (ClickField clickField, String addText) {
        if (!Util.hasValue(addText)) return;
        String baseText = clickField.getToolTipText();
        clickField.setToolTipText(Util.hasValue(baseText) ? baseText+"<br>"+addText : addText);
    }

}
