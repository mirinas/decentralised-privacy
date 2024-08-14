package main.java.uk.ac.soton.am26g21.state;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hyperledger.fabric.contract.Context;

import static main.java.uk.ac.soton.am26g21.contract.PrivacyContract.genson;

@Getter
@Setter
@NoArgsConstructor
public class LedgerState extends UserState {

  @Getter
  @AllArgsConstructor
  public enum StateType {
    CONSTRAINT("C"),
    EVENT("E"),
    UPLOAD("U"),
    QUERY("Q"),
    PURGE("P"),
    ACCOUNT("ACC"),
    REQUEST("RQ"),
    REFINE("REF");

    final String value;
  }

  protected String type;

  public LedgerState(StateType type, String user) {
    super(user);
    this.type = type.value;
  }

  public LedgerState(StateType type, String user, Object state) {
    super(user, state);
    this.type = type.value;
  }

  public static LedgerState deserialize(StateType type, String user, String state) {
    if(state == null || state.isBlank()) {
      return new LedgerState(type, user);
    }
    return genson.deserialize(state, LedgerState.class);
  }

  public static String key(Context ctx, StateType type, String... keys) {
//    keys = Arrays.stream(keys)
//        .map(DigestUtils::sha256Hex)
//        .toArray(String[]::new);

    return ctx.getStub().createCompositeKey(type.getValue(), keys).toString();
  }
}


