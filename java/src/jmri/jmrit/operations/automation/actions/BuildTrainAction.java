package jmri.jmrit.operations.automation.actions;

import jmri.jmrit.operations.trains.Train;

public class BuildTrainAction extends Action {

    private static final int _code = ActionCodes.BUILD_TRAIN;

    @Override
    public int getCode() {
        return _code;
    }

    @Override
    public String toString() {
        return Bundle.getMessage("BuildTrain");
    }

    @Override
    public void doAction() {
        if (getAutomationItem() != null) {
            Train train = getAutomationItem().getTrain();
            if (train != null && !train.isBuilt()) {
                train.build();
            }
            // now show message if there's one
            finishAction();
        }
    }

    @Override
    public void cancelAction() {
        // no cancel for this action     
    }

}