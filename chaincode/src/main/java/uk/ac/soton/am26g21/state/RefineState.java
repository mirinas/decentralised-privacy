package main.java.uk.ac.soton.am26g21.state;

import org.hyperledger.fabric.contract.Context;

import static main.java.uk.ac.soton.am26g21.contract.PrivacyContract.genson;

public class RefineState {

    public static RequestState deserialize(String name, String date, String owner, String state) {
        if(state == null || state.isBlank()) {
            var refine = new RequestState(name, date, owner);
            refine.setType(LedgerState.StateType.REFINE.value);
            return refine;
        }
        return genson.deserialize(state, RequestState.class);
    }

    public static RequestState deserialize(String state) {
        return deserialize(null, null, null, state);
    }

    public static String key(Context ctx, String name, String user) {
        return ctx.getStub().createCompositeKey(LedgerState.StateType.REFINE.value, name, user).toString();
    }
}
