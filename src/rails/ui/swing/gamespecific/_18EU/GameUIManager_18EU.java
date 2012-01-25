package rails.ui.swing.gamespecific._18EU;

import java.util.List;

import javax.swing.JOptionPane;

import rails.common.LocalText;
import rails.game.PublicCompany;
import rails.game.Stop;
import rails.game.action.MergeCompanies;
import rails.game.specific._18EU.StartCompany_18EU;
import rails.ui.swing.GameUIManager;
import rails.ui.swing.elements.NonModalDialog;
import rails.ui.swing.elements.RadioButtonDialog;

public class GameUIManager_18EU extends GameUIManager {

    @Override
    public void dialogActionPerformed () {

        if (currentDialog instanceof RadioButtonDialog
                && currentDialogAction instanceof MergeCompanies) {

            RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
            MergeCompanies action = (MergeCompanies) currentDialogAction;
            PublicCompany minor = action.getMergingCompany();

            if (action.getSelectedTargetCompany() == null) {
                // Step 1: selection of the major company to merge into
                int choice = dialog.getSelectedOption();
                if (choice < 0) return;

                PublicCompany major = action.getTargetCompanies().get(choice);
                action.setSelectedTargetCompany(major);

                if (major != null && action.canReplaceToken(choice)) {

                    boolean replaceToken =
                        JOptionPane.showConfirmDialog(statusWindow, LocalText.getText(
                                "WantToReplaceToken",
                                minor.getId(),
                                major.getId() ),
                                LocalText.getText("PleaseSelect"),
                                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
                    action.setReplaceToken(replaceToken);
                }
            } else {
                // To be added later when ReplaceToken dialog is modeless
            }

        } else if (currentDialog instanceof RadioButtonDialog
                && currentDialogAction instanceof StartCompany_18EU) {

            RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
            StartCompany_18EU action = (StartCompany_18EU) currentDialogAction;

            if (action.getPrice() == 0) {

                // The price will be set first
                int index = dialog.getSelectedOption();
                if (index < 0) return;
                action.setStartPrice(action.getStartPrices()[index]);

                // Set up another dialog for the next step
                   List<PublicCompany> minors = action.getMinorsToMerge();

                if (minors != null && !minors.isEmpty()) {
                    // Up to phase 6, a minor must be exchanged
                    String[] options = new String[minors.size()];
                    int i = 0;
                       for (PublicCompany minor : minors) {
                        options[i++] =
                                   "Minor " + minor.getId() + " "
                            + minor.getLongName();
                    }
                    dialog = new RadioButtonDialog (NonModalDialog.Usage.SELECT_FOLDING_COMPANIES,
                            this,
                            statusWindow,
                            LocalText.getText("PleaseSelect"),
                            LocalText.getText(
                                    "SelectMinorToMerge",
                                    action.getCompanyName()),
                                    options, -1);
                    setCurrentDialog(dialog, action);
                    statusWindow.disableButtons();
                    return;
                } else {

                    // From phase 6, no minors are involved, but a home station must be chosen
                    List<Stop> cities = action.getAvailableHomeStations();
                    if (cities != null && !cities.isEmpty()) {
                        String[] options = new String[cities.size()];
                        for (int i = 0; i < options.length; i++) {
                            options[i] = cities.get(i).toString();
                        }
                        dialog = new RadioButtonDialog (NonModalDialog.Usage.SELECT_HOME_STATION,
                                this,
                                statusWindow,
                                LocalText.getText("PleaseSelect"),
                                LocalText.getText(
                                        "SelectHomeStation",
                                        action.getCompanyName()),
                                        options, -1);
                        setCurrentDialog(dialog, action);
                        statusWindow.disableButtons();
                        return;

                    }
                }
            } else if (action.getMinorsToMerge() != null) {
                // Up to phase 5: a minor to merge has been selected (or not)
                int choice = dialog.getSelectedOption();
                if (choice < 0) {
                    // Also reset price
                    action.setStartPrice(0);
                    return;
                }
                action.setChosenMinor(action.getMinorsToMerge().get(choice));

            } else if (action.getAvailableHomeStations() != null) {
                // From phase 6: a home station has been selected (or not)

                int index = dialog.getSelectedOption();
                if (index < 0) {
                    // Also reset price
                    action.setStartPrice(0);
                    return;
                }
                action.setHomeStation(action.getAvailableHomeStations().get(index));
            }
        } else {
            super.dialogActionPerformed(false);
        }

        super.dialogActionPerformed(true);
    }

}
