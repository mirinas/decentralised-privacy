package main.java.uk.ac.soton.am26g21.state;

import static main.java.uk.ac.soton.am26g21.contract.PrivacyContract.genson;
import static main.java.uk.ac.soton.am26g21.contract.PrivacyContract.hashSecret;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import main.java.uk.ac.soton.am26g21.contract.ChaincodeError;

@Getter
@Setter
@NoArgsConstructor
public class ConstraintState extends LedgerState {
  private String salt;
  private String hashedSecret;

  public ConstraintState(String user, String secret, String salt) {
    super(StateType.CONSTRAINT, user);
    this.hashedSecret = secret;
    this.salt = salt;
  }

  public static ConstraintState deserialize(String user, String hash, String salt, String state) {
    if(state == null || state.isBlank()) {
      return new ConstraintState(user, hash, salt);
    }
    return genson.deserialize(state, ConstraintState.class);
  }

  public void validateSecret(byte[] secret) {
    String hash = hashSecret(secret, salt);
    if(!this.hashedSecret.equals(hash)) {
      throw new ChaincodeError("Invalid constraint secret!");
    }
  }
}

