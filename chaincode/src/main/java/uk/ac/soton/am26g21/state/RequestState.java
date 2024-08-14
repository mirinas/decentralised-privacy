package main.java.uk.ac.soton.am26g21.state;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;
import main.java.uk.ac.soton.am26g21.contract.ChaincodeError;
import org.hyperledger.fabric.contract.Context;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static main.java.uk.ac.soton.am26g21.contract.PrivacyContract.genson;

@Log
@Getter
@Setter
@NoArgsConstructor
public class RequestState extends LedgerState {
    private Map<String, Object> assignees = new HashMap<>();
    private String date;
    private String name;

    public RequestState(String name, String date, String assigner) {
        super(StateType.REQUEST, assigner);
        this.name = name;
        this.date = date;
    }

    public void addAssignee(String username) {
        assignees.putIfAbsent(username, false);
    }

    public void update(String json) {
        setState(genson.deserialize(json, Map.class));
        assignees.replaceAll((user, signature) -> false);
    }

    public void checkAccess(String client, String controller, String descriptor, String action) {
        if(!getUser().equals(client)) {
            throw new ChaincodeError(client + " does not own this request");
        }

        if(!assignees.containsKey(controller) || !(boolean) assignees.get(controller)) {
            throw new ChaincodeError(controller + " did not sign this request");
        }

        // check if descriptor permission is granted
        if(!state.getJSONArray("permission")
                .getJSONObject(0)
                .getJSONArray("target").toString().contains(descriptor)) {
            throw new ChaincodeError("Access for " + descriptor + " is not granted");
        }

        // check if action is allowed
        if(!state.getJSONArray("permission")
                .getJSONObject(0)
                .getJSONArray("action").toString().contains(action)) {
            throw new ChaincodeError(action + " is not allowed");
        }

        // check if temporal access is given
        var endDate = LocalDate.parse(
                state.getJSONArray("permission")
                .getJSONObject(0)
                .getString("end_date"));
        var startDate = LocalDate.parse(
                state.getJSONArray("permission")
                        .getJSONObject(0)
                        .getString("start_date"));

        var now = LocalDate.now();
        if(     (!endDate.isEqual(now) && !endDate.isAfter(now)) ||
                (!startDate.isEqual(now) && !startDate.isBefore(now))) {
            throw new ChaincodeError("Current time is invalid for this request");
        }

        // TODO check for any refinements
    }

    public void sign(String username) {
        assignees.put(username, true);
    }

    public void revoke(String username) {
        assignees.put(username, false);
    }

    public static String key(Context ctx, String name) {
        return ctx.getStub().createCompositeKey(StateType.REQUEST.value, name).toString();
    }

    public static RequestState deserialize(String name, String date, String owner, String state) {
        if(state == null || state.isBlank()) {
            return new RequestState(name, date, owner);
        }
        return genson.deserialize(state, RequestState.class);
    }

    public static RequestState deserialize(String state) {
        return deserialize(null, null, null, state);
    }
}
