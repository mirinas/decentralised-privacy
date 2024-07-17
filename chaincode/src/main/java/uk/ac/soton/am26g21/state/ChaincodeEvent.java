package main.java.uk.ac.soton.am26g21.state;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChaincodeEvent extends LedgerState {
  String collection;

  public ChaincodeEvent(StateType type, String user, String collection) {
    super(type, user);
    this.collection = collection;
  }
}
