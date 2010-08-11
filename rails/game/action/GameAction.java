/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/GameAction.java,v 1.5 2010/04/04 22:02:53 stefanfrey Exp $*/
package rails.game.action;

/* THIS CLASS NEED NOT BE SERIALIZED */
public class GameAction extends PossibleAction {

    public static final int SAVE = 0;
    public static final int LOAD = 1;
    public static final int UNDO = 2;
    public static final int FORCED_UNDO = 3;
    public static final int REDO = 4;
    public static final int EXPORT = 5;
    public static final int MAX_MODE = 5;

    private String[] name =
            new String[] { "Save", "Load", "Undo", "Undo!", "Redo", "Export" };

    // Server-side settings
    protected int mode = -1;

    // Client-side settings
    protected String filepath; // Only applies to SAVE and LOAD
    protected int moveStackIndex = -1; // target moveStackIndex, only for FORCED_UNDO and REDO

    public static final long serialVersionUID = 1L;

    public GameAction(int mode) {
        super();
        if (mode < 0 || mode > MAX_MODE) mode = 0; // For safety
        this.mode = mode;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setmoveStackIndex(int moveStackIndex) {
        this.moveStackIndex = moveStackIndex;
    }

    public int getmoveStackIndex() {
        return moveStackIndex;
    }
    
    public int getMode() {
        return mode;
    }

    public boolean equals(PossibleAction action) {
        if (!(action instanceof GameAction)) return false;
        GameAction a = (GameAction) action;
        return a.mode == mode;
    }

    public String toString() {
        return name[mode];
    }
}
