package test.java.uk.ac.soton.am26g21;

import static main.java.uk.ac.soton.am26g21.contract.PrivacyContract.genson;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import main.java.uk.ac.soton.am26g21.OrganizationAccess;
import main.java.uk.ac.soton.am26g21.state.LedgerState;
import main.java.uk.ac.soton.am26g21.state.LedgerState.StateType;
import main.java.uk.ac.soton.am26g21.state.State;
import main.java.uk.ac.soton.am26g21.state.UserState;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.junit.jupiter.api.Test;

public class AppTest {

    @Test
    public void testJsonUpdate() {
        String data = "{\"state\":{\"Org1MSP\":{\"read\":{\"_implicit_org_Org1MSP\":true},\"write\":{\"_implicit_org_Org1MSP\":true}}},\"type\":\"C\",\"user\":\"admin@owners.org\"}";
        var state = UserState.deserialize("u1", data);
        state.set("Org1MSP.write._implicit_org_Org2MSP", "true");

        System.out.println(state);
    }

    @Test
    public void testJsonClass() {
        Map<String, Object> patient = Map.of(
            "male", true,
            "treatments", List.of("chemotherapy", "antibiotics")
        );
        Map<String, Object> user = Map.of(
            "age", 50,
            "name", "John",
            "patient", patient
        );

        String input = genson.serialize(user);
        var userRecord = new UserState();

        userRecord.set("treatment", "{\"age\":50,\"gender\":false}");
        String serialized = genson.serialize(userRecord);

        System.out.println(serialized);

        var deserialized = genson.deserialize(serialized, UserState.class);
        System.out.println(deserialized.toString());
        System.out.println(deserialized.getUser());
    }

    @Test
    public void testRecordDeserialization() {

        var patient = Map.of(
            "age", 50,
            "name", "Arthur"
        );
        var json = Map.of(
            "user", "user1",
            "data", patient
        );
        String input = genson.serialize(json);
        System.out.println(input);
    }

    @Test
    public void testOrganizationAccess() {
        var access = new OrganizationAccess()
            .addRead("_implicit_org_Org1MSP")
            .addRead("_implicit_org_Org2MSP")
            .addWrite("_implicit_org_Org1MSP");


        var state = new LedgerState(StateType.CONSTRAINT, "u1");
        state.set("Org1MSP", genson.serialize(access));
        String json = genson.serialize(state);

        System.out.println(json);
        var deserialized = genson.deserialize(json, State.class);
        var deAccess = OrganizationAccess.deserialize(deserialized.get("Org1MSP"));

        System.out.println(deAccess.getRead());
        System.out.println(deAccess.getWrite());
    }


    private static Context mockContext(String compKey, String userAddress) {
        var key = mock(CompositeKey.class);
        when(key.toString())
            .thenReturn(compKey);
        var stub = mock(ChaincodeStub.class);
        when(stub.createCompositeKey(any(), any()))
            .thenReturn(key);
        var ctx = mock(Context.class);
        when(ctx.getStub())
            .thenReturn(stub);

        var id = mock(ClientIdentity.class);
        when(id.getId()).thenReturn("x509::CN=" + userAddress + ",");
        when(ctx.getClientIdentity()).thenReturn(id);

        return ctx;
    }
}
