package rails.ui.swing.gamespecific._18EU;

import java.awt.event.ActionEvent;
import java.util.List;

import rails.common.LocalText;
import rails.game.PublicCompany;
import rails.game.action.MergeCompanies;
import rails.game.action.PossibleAction;
import rails.ui.swing.GameStatus;
import rails.ui.swing.elements.NonModalDialog;
import rails.ui.swing.elements.RadioButtonDialog;

/**
 * This class is incorporated into StatusWindow and displays the bulk of
 * rails.game status information.
 */
public class GameStatus_18EU extends GameStatus {

    private static final long serialVersionUID = 1L;

    @Override
    protected void initGameSpecificActions() {

        PublicCompany mergingCompany;
        int index;

        List<MergeCompanies> mergers =
            possibleActions.getType(MergeCompanies.class);
        if (mergers != null) {
            for (MergeCompanies merger : mergers) {
                mergingCompany = merger.getMergingCompany();
                if (mergingCompany != null) {
                    index = mergingCompany.getPublicNumber();
                    setPlayerCertButton(index, merger.getPlayerIndex(), true,
                            merger);
                }
            }
        }

    }

    /** Start a company - specific procedure for 18EU */
    @Override
    protected PossibleAction processGameSpecificActions(ActionEvent actor,
            PossibleAction chosenAction) {

        if (chosenAction instanceof MergeCompanies) {

            log.debug("Merge action: " + chosenAction.toString());

            MergeCompanies action = (MergeCompanies) chosenAction;
            PublicCompany minor = action.getMergingCompany();
            List<PublicCompany> targets = action.getTargetCompanies();

            if (minor == null || targets == null || targets.isEmpty()) {
                log.error("Bad " + action.toString());
                return null;
            }

            String[] options = new String[targets.size()];
            int i = 0;
            for (PublicCompany target : targets) {
                if (target != null) {
                    options[i++] =
                            target.getId() + " " + target.getLongName();
                } else {
                    options[i++] =
                            LocalText.getText("CloseMinor", minor.getId());
                }
            }

            RadioButtonDialog dialog = new RadioButtonDialog (
                    NonModalDialog.Usage.SELECT_COMPANY,
                    gameUIManager,
                    parent,
                    LocalText.getText("PleaseSelect"),
                    LocalText.getText("SelectCompanyToMergeMinorInto",
                            minor.getId()),
                            options, -1);
            gameUIManager.setCurrentDialog(dialog, action);
            parent.disableButtons();

        }
        return null;
    }


}
