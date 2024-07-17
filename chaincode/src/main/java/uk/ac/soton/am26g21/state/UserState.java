package main.java.uk.ac.soton.am26g21.state;

import static main.java.uk.ac.soton.am26g21.contract.PrivacyContract.genson;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

import com.owlike.genson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@Getter
@Setter
public class UserState extends State {
  private String user;

  public UserState(String user) {
    super();
    this.user = user;
  }

  public UserState(String user, Object state) {
    super(state);
    this.user = user;
  }

  @JsonIgnore
  public String getKey() {
    return key(user);
  }

  public static UserState deserialize(String user, String state) {
    if(state == null || state.isBlank()) {
      return new UserState(user);
    }
    return genson.deserialize(state, UserState.class);
  }

  public static String key(String data) {
    return sha256Hex(data);
  }
}
