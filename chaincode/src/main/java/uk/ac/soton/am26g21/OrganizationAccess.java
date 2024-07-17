package main.java.uk.ac.soton.am26g21;

import static main.java.uk.ac.soton.am26g21.contract.PrivacyContract.genson;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import main.java.uk.ac.soton.am26g21.contract.ChaincodeError;

/**
 * Operations using constraints:
 * - checking if read/write is allowed
 * - TODO filtering returned values
 */
@Data
@NoArgsConstructor
public class OrganizationAccess {
  Map<String, Boolean> read = new HashMap<>();
  Map<String, Boolean> write = new HashMap<>();

  public void accessWrite(String collection) {
    Boolean access = write.get(collection);
    if(access == null || !access) {
      throw new ChaincodeError("No write access at " + collection);
    }
  }

  public void accessRead(String collection) {
    Boolean access = read.get(collection);
    if(access == null || !access) {
      throw new ChaincodeError("No read access at " + collection);
    }
  }

  public OrganizationAccess addRead(String coll) {
    read.put(coll, true);
    return this;
  }

  public OrganizationAccess removeRead(String coll) {
    read.put(coll, false);
    return this;
  }

  public OrganizationAccess addWrite(String coll) {
    write.put(coll, true);
    return this;
  }

  public OrganizationAccess removeWrite(String coll) {
    write.put(coll, false);
    return this;
  }

  public static OrganizationAccess deserialize(Object stringState) {
    if(stringState == null) {
      return new OrganizationAccess();
    }
    return genson.deserialize(stringState.toString(), OrganizationAccess.class);
  }
}
