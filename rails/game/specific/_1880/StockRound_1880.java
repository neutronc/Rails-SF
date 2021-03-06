/**
 * This class implements the 1880 rules for making new companies
 * being available in the IPO after buying shares of another company.
 */
package rails.game.specific._1880;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import rails.common.LocalText;
import rails.game.*;
import rails.game.action.BuyCertificate;
import rails.game.action.PossibleAction;
import rails.game.action.SellShares;
import rails.game.action.StartCompany;
import rails.game.move.CashMove;
import rails.game.specific._1880.PublicCompany_1880;


public class StockRound_1880 extends StockRound {

    /**
     * Constructor with the GameManager, will call super class (StockRound's)
     * Constructor to initialize
     * 
     * @param aGameManager The GameManager Object needed to initialize the Stock
     * Round
     * 
     */
    public StockRound_1880(GameManagerI aGameManager) {
        super(aGameManager);
    }

    @Override
    // The sell-in-same-turn-at-decreasing-price option does not apply here
    protected int getCurrentSellPrice(PublicCompanyI company) {

        String companyName = company.getName();
        int price;

        if (sellPrices.containsKey(companyName)) {
            price = (sellPrices.get(companyName)).getPrice();
        } else {
            price = company.getCurrentSpace().getPrice();
        }
        if (!((PublicCompany_1880) company).isCommunistPhase()) {
            // stored price is the previous unadjusted price
            price = price / company.getShareUnitsForSharePrice();
            // Price adjusted by -5 per share for selling but only if we are not
            // in CommunistPhase...
            price = price - 5;
        }
        return price;
    }

    /**
     * Share price goes down 1 space for any number of shares sold.
     */
    @Override
    protected void adjustSharePrice(PublicCompanyI company, int numberSold,
            boolean soldBefore) {
        // No more changes if it has already dropped
        // Or we are in the CommunistTakeOverPhase after the 4T has been bought
        // and the 6T has not yet been bought
        if ((!soldBefore)
            || (!((PublicCompany_1880) company).isCommunistPhase())) {
            super.adjustSharePrice(company, 1, soldBefore);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * rails.game.StockRound#mayPlayerSellShareOfCompany(rails.game.PublicCompanyI
     * )
     */
    @Override
    public boolean mayPlayerSellShareOfCompany(PublicCompanyI company) {
        if ((company.getPresident() == gameManager.getCurrentPlayer())
            && (((PublicCompany_1880) company).isCommunistPhase())) {
            return false;
        }
        return super.mayPlayerSellShareOfCompany(company);
    }

    /* (non-Javadoc)
     * @see rails.game.StockRound#gameSpecificChecks(rails.game.Portfolio, rails.game.PublicCompanyI)
     */
    @Override
      protected void gameSpecificChecks(Portfolio boughtFrom,
            PublicCompanyI company) {
        boolean fullCap = false;
        
        if (boughtFrom != ipo) return;
         
        fullCap =  ((PublicCompany_1880) company).isFullyCapitalised();
        if  (fullCap == true) return; // If the company is already fully capitalized do nothing
       
        if ((((PublicCompany_1880) company).hasFloated()) && (!((PublicCompany_1880) company).isFullyCapitalised()) && ((getSoldPercentage(company)>= 50) && 
                ( ((PublicCompany_1880) company).shouldBeCapitalisedFull()))) {
                    company.setCapitalisation(0); //CAPITALISATION_FULL
                    int additionalOperatingCapital;
                    additionalOperatingCapital=company.getIPOPrice()*5;
                    company.addCash(additionalOperatingCapital);
                    ((PublicCompany_1880) company).setFullyCapitalised(true);
                    // Can be used as 1880 has no game end on bank break or should CashMove() be used ?
        }// TODO: Do we need to add money to the companies wallet somewhere ?
        
        // how to find out which certificates have been bought ?
        //check the current player
         PublicCertificateI cert= null;
         cert = getCurrentPlayer().getPortfolio().findCertificate(company, true);
         if (cert !=null) { //the current player has the president certificate of this PublicCompany..
             //check if the investor of the current player has a share of this PublicCompany
             CompanyManagerI compMgr= gameManager.getCompanyManager();
             List<PublicCompanyI>allComp=compMgr.getAllPublicCompanies();
             for (PublicCompanyI privComp:  allComp) {
                 if ((privComp.getTypeName().equals ("Minor")) && (privComp.getPresident()==getCurrentPlayer())) {
                     // We have an Investor and the President is the current Player
                     //now we need to find out if the Portfolio of that Minor holds a share (only one is allowed !) of any Company...
                     if (privComp.getPortfolio().ownsCertificates(company, 1, false) == 0) {
                         //need to check if the Private Company owns any other certificate of a company...
                         for (PublicCompanyI comp2 : allComp) {
                             if (privComp.getPortfolio().ownsCertificates(comp2,1,false) > 0) {
                                return;
                             } // if clause end to be left if no certificate has been found otherwise early 
                         } // we have a Private thats in Possession of our current Player  that doesnt have a certificate of the company in possession where the player is president of...
                         PublicCertificateI cert2;
                         cert2 = boughtFrom.findCertificate(company, 1, false);
                         if (cert2 == null) {
                                 log.error("Cannot find " + company.getName() + " " + 1*10
                                         + "% share in " + boughtFrom.getName());
                             }
                             cert2.moveTo(privComp.getPortfolio());
                             ((PublicCompany_1880) privComp).setDestinationHex(company.getHomeHexes().get(0));
                             privComp.setInfoText(privComp.getInfoText() + "<br>Destination: "+privComp.getDestinationHex().getInfo());
                             // Check if the company has floated
                             if (!company.hasFloated()) checkFlotation(company);
                             return;
                     } else {
                         return;
                     }
                 }
             }
         }

        
        super.gameSpecificChecks(boughtFrom, company);
    }
    /**
     * Float a company, including a default implementation of moving cash and
     * shares as a result of flotation. <p>Fifty Percent capitalisation is implemented
     * as in 1880. 
     */
    @Override
    protected void floatCompany(PublicCompanyI company) {
        // Move cash and shares where required
        int cash = 0;
        int price = company.getIPOPrice();
        
        
        // For all Companies who float before the first 6T is purchased the Full Capitalisation will happen on purchase of 
        // 50 percent of the shares.
        // The exception of the rule of course are the late starting companies after the first 6 has been bought when the 
        // flotation will happen after 60 percent have been bought.
        if (((PublicCompany_1880) company).shanghaiExchangeIsOperational()) {
            cash = 10 * price;
        } else {
            cash = 5 * price;
        }
       
        company.setFloated(); 

        if (cash > 0) {
            new CashMove(bank, company, cash);
            ReportBuffer.add(LocalText.getText("FloatsWithCash",
                    company.getName(),
                    Bank.format(cash) ));
        } else {
            ReportBuffer.add(LocalText.getText("Floats",
                    company.getName()));
        }

    }

    /* (non-Javadoc)
     * @see rails.game.StockRound#processGameSpecificAction(rails.game.action.PossibleAction)
     */
    @Override
    protected boolean processGameSpecificAction(PossibleAction action) {
        // TODO Auto-generated method stub
        return super.processGameSpecificAction(action);
    }

    /* (non-Javadoc)
     * @see rails.game.StockRound#setBuyableCerts()
     */
    @Override
    public void setBuyableCerts() {
        if (!mayCurrentPlayerBuyAnything()) return;

        List<PublicCertificateI> certs;
        PublicCertificateI cert;
        PublicCompanyI comp;
        StockSpaceI stockSpace;
        Portfolio from;
        int price;
        int number;
        int unitsForPrice;

        int playerCash = currentPlayer.getCash();

        /* Get the next available IPO certificates */
        // Never buy more than one from the IPO
        PublicCompanyI companyBoughtThisTurn =
            (PublicCompanyI) companyBoughtThisTurnWrapper.get();
        if (companyBoughtThisTurn == null) {
            from = ipo;
            Map<String, List<PublicCertificateI>> map =
                from.getCertsPerCompanyMap();
            int shares;

            for (String compName : map.keySet()) {
                certs = map.get(compName);
                if (certs == null || certs.isEmpty()) continue;

                /* Only the top certificate is buyable from the IPO */
                int lowestIndex = 99;
                int ipoShares = 0;
                cert = null;
                int index;
                for (PublicCertificateI c : certs) {
                    index = c.getIndexInCompany();
                    if (index < lowestIndex) {
                        lowestIndex = index;
                        cert = c;
                    }
                    if (c.getPortfolio().getOwner() == bank ) {
                        ipoShares += cert.getShares();
                    }
                }

                comp = cert.getCompany();
                unitsForPrice = comp.getShareUnitsForSharePrice();
                if (isSaleRecorded(currentPlayer, comp)) continue;
                if (maxAllowedNumberOfSharesToBuy(currentPlayer, comp,
                        cert.getShare()) < 1 ) continue;
                //Make sure that only 50  percent of shares are sold until all Certs are to become avail upon Phase change
                //after the first 3 Train has been sold from IPO.
                                      
                //find the President Certificate Check the Percentage; sharesOwnedByPlayers returns the number of Certificates and not the share percentages.. 
                if ((ipoShares == 5) && (!((PublicCompany_1880) comp).getAllCertsAvail())) continue;

                /* Would the player exceed the total certificate limit? */
                stockSpace = comp.getCurrentSpace();
                if ((stockSpace == null || !stockSpace.isNoCertLimit()) && !mayPlayerBuyCertificate(
                        currentPlayer, comp, cert.getCertificateCount())) continue;

                shares = cert.getShares();

                if (!cert.isPresidentShare()) {
                    price = comp.getIPOPrice() / unitsForPrice;
                    if (price <= playerCash) {
                        possibleActions.add(new BuyCertificate(comp, cert.getShare(),
                                from, price));
                    }
                } else if (!comp.hasStarted()) {
                    List<Integer> startPrices = new ArrayList<Integer>();
                        for (int startPrice : stockMarket.getStartPrices()) {
                            if ((startPrice * shares <= playerCash) && (((StockMarket_1880) gameManager.getStockMarket()).getParSlot(startPrice))) {
                                startPrices.add(startPrice);
                            }
                        }
                        if (startPrices.size() > 0) {
                            int[] prices = new int[startPrices.size()];
                            for (int i = 0; i < prices.length; i++) {
                                prices[i] = startPrices.get(i);
                            }
                            Arrays.sort(prices);
                            StartCompany_1880 action =
                                    new StartCompany_1880(comp, prices);
                            possibleActions.add(action);
                        }
                }
             }
        }

        /* Get the unique Pool certificates and check which ones can be bought */
        from = pool;
        Map<String, List<PublicCertificateI>> map =
            from.getCertsPerCompanyMap();
        /* Allow for multiple share unit certificates (e.g. 1835) */
        PublicCertificateI[] uniqueCerts;
        int[] numberOfCerts;
        int shares;
        int shareUnit;
        int maxNumberOfSharesToBuy;

        for (String compName : map.keySet()) {
            certs = map.get(compName);
            if (certs == null || certs.isEmpty()) continue;

            comp = certs.get(0).getCompany();
            stockSpace = comp.getCurrentSpace();
            unitsForPrice = comp.getShareUnitsForSharePrice();
            price = stockSpace.getPrice() / unitsForPrice;
            shareUnit = comp.getShareUnit();
            maxNumberOfSharesToBuy
            = maxAllowedNumberOfSharesToBuy(currentPlayer, comp, shareUnit);

            /* Checks if the player can buy any shares of this company */
            if (maxNumberOfSharesToBuy < 1) continue;
            if (isSaleRecorded(currentPlayer, comp)) continue;
            if ((comp.sharesOwnedByPlayers() ==50) && (!((PublicCompany_1880) comp).getAllCertsAvail())) continue;
            if (companyBoughtThisTurn != null) {
                // If a cert was bought before, only brown zone ones can be
                // bought again in the same turn
                if (comp != companyBoughtThisTurn) continue;
                if (!stockSpace.isNoBuyLimit()) continue;
            }

            /* Check what share multiples are available
             * Normally only 1, but 1 and 2 in 1835. Allow up to 4.
             */
            uniqueCerts = new PublicCertificateI[5];
            numberOfCerts = new int[5];
            for (PublicCertificateI cert2 : certs) {
                shares = cert2.getShares();
                if (maxNumberOfSharesToBuy < shares) continue;
                numberOfCerts[shares]++;
                if (uniqueCerts[shares] != null) continue;
                uniqueCerts[shares] = cert2;
            }

            /* Create a BuyCertificate action per share size */
            for (shares = 1; shares < 5; shares++) {
                /* Only certs in the brown zone may be bought all at once */
                number = numberOfCerts[shares];
                if (number == 0) continue;

                if (!stockSpace.isNoBuyLimit()) {
                    number = 1;
                    /* Would the player exceed the per-company share hold limit? */
                    if (!checkAgainstHoldLimit(currentPlayer, comp, number)) continue;

                    /* Would the player exceed the total certificate limit? */
                    if (!stockSpace.isNoCertLimit()
                            && !mayPlayerBuyCertificate(currentPlayer, comp,
                                    number * uniqueCerts[shares].getCertificateCount()))
                        continue;
                }

                // Does the player have enough cash?
                while (number > 0 && playerCash < number * price * shares) {
                    number--;
                }

                if (number > 0) {
                    possibleActions.add(new BuyCertificate(comp,
                            uniqueCerts[shares].getShare(),
                            from, price,
                            number));
                }
            }
        }

        // Get any shares in company treasuries that can be bought
        if (gameManager.canAnyCompanyHoldShares()) {

            for (PublicCompanyI company : companyManager.getAllPublicCompanies()) {
                certs =
                    company.getPortfolio().getCertificatesPerCompany(
                            company.getName());
                if (certs == null || certs.isEmpty()) continue;
                cert = certs.get(0);
                if (isSaleRecorded(currentPlayer, company)) continue;
                if (!checkAgainstHoldLimit(currentPlayer, company, 1)) continue;
                if (maxAllowedNumberOfSharesToBuy(currentPlayer, company,
                        certs.get(0).getShare()) < 1) continue;
                stockSpace = company.getCurrentSpace();
                if (!stockSpace.isNoCertLimit()
                        && !mayPlayerBuyCertificate(currentPlayer, company, 1)) continue;
                if (company.getMarketPrice() <= playerCash) {
                    possibleActions.add(new BuyCertificate(company, cert.getShare(),
                            company.getPortfolio(), company.getMarketPrice()));
                }
            }
        }
        }

    /* (non-Javadoc)
     * @see rails.game.StockRound#startCompany(java.lang.String, rails.game.action.StartCompany)
     */
    @Override
    public boolean startCompany(String playerName, StartCompany action) {
        // TODO Auto-generated method stub
        return super.startCompany(playerName, action);
    }

    /* (non-Javadoc)
     * @see rails.game.StockRound#finishRound()
     */
    @Override
    protected void finishRound() {
        ReportBuffer.add(" ");
        ReportBuffer.add(LocalText.getText("END_SR",
                String.valueOf(getStockRoundNumber())));

        if (raiseIfSoldOut) {
            /* Check if any companies are sold out. */
            for (PublicCompanyI company : gameManager.getCompaniesInRunningOrder()) {
                if (company.hasStockPrice() && company.isSoldOut() && (!((PublicCompany_1880) company).isCommunistPhase())) {
                    StockSpaceI oldSpace = company.getCurrentSpace();
                    stockMarket.soldOut(company);
                    StockSpaceI newSpace = company.getCurrentSpace();
                    if (newSpace != oldSpace) {
                        ReportBuffer.add(LocalText.getText("SoldOut",
                                company.getName(),
                                Bank.format(oldSpace.getPrice()),
                                oldSpace.getName(),
                                Bank.format(newSpace.getPrice()),
                                newSpace.getName()));
                    } else {
                        ReportBuffer.add(LocalText.getText("SoldOutNoRaise",
                                company.getName(),
                                Bank.format(newSpace.getPrice()),
                                newSpace.getName()));
                    }
                }
            }
        }
        /** At the end of each Stockround the current amount of negative cash is subject to a fine of 50 percent
         * 
         */
        for (Player p : playerManager.getPlayers()) {
            if (p.getCash() <0 ) {
                int fine = p.getCash()/2;
                p.addCash(-fine);
            }
        }
        
        // Report financials
        ReportBuffer.add("");
        for (PublicCompanyI c : companyManager.getAllPublicCompanies()) {
            if (c.hasFloated() && !c.isClosed()) {
                ReportBuffer.add(LocalText.getText("Has", c.getName(),
                        Bank.format(c.getCash())));
            }
        }
        for (Player p : playerManager.getPlayers()) {
            ReportBuffer.add(LocalText.getText("Has", p.getName(),
                    Bank.format(p.getCash())));
        }
        // Inform GameManager
        gameManager.nextRound(this);
    }

    /* (non-Javadoc)
     * @see rails.game.StockRound#sellShares(rails.game.action.SellShares)
     */
    @Override
    public boolean sellShares(SellShares action) {
        // TODO Auto-generated method stub
        if(super.sellShares(action)) {
            int numberSold=action.getNumber();
            gameManager.getCurrentPlayer().addCash(-5*numberSold); //Deduct the Money for selling those Shares !
            return true;
        }
        else
        {
            return false;
        }
    }


}
