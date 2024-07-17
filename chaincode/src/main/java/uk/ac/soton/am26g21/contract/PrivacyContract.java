/*
 * SPDX-License-Identifier: Apache-2.0
 */
package main.java.uk.ac.soton.am26g21.contract;

import static main.java.uk.ac.soton.am26g21.state.LedgerState.StateType.CONSTRAINT;
import static main.java.uk.ac.soton.am26g21.state.LedgerState.StateType.EVENT;

import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import main.java.uk.ac.soton.am26g21.OrganizationAccess;
import main.java.uk.ac.soton.am26g21.state.ChaincodeEvent;
import main.java.uk.ac.soton.am26g21.state.ConstraintState;
import main.java.uk.ac.soton.am26g21.state.LedgerState;
import main.java.uk.ac.soton.am26g21.state.LedgerState.StateType;
import main.java.uk.ac.soton.am26g21.state.UserState;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.contract.annotation.Transaction.TYPE;

@Contract(info = @Info(
    title = "Privacy contract",
    description = "Enables endorsed data sharing with constraint application",
    version = "1.0",
    license = @License(
        name = "SPDX-License-Identifier: Apache-2.0"),
    contact = @Contact(
        email = "am26g21@soton.ac.uk",
        name = "Augustas Mirinas")))

@Log
@NoArgsConstructor
@Default
public class PrivacyContract implements ContractInterface {

  public static final String IMPLICIT = "_implicit_org_";
  public static final String AUTHORITY = "OwnersMSP";

  public static final SecureRandom random = new SecureRandom();
  public static final Genson genson = new GensonBuilder()
      .setSkipNull(true)
      .create();



  /**
   * Function to set a user constraint. Can be called by any client and endorsed by any peer
   * User is extracted from the client identity
   *
   * @return status message
   */
  @Transaction(intent = TYPE.SUBMIT)
  public String SetConstraints(Context ctx, String user, String descriptor, String value) {
    var stub = ctx.getStub();

    if(!ctx.getClientIdentity().getMSPID().equals(AUTHORITY)) {
      throw new ChaincodeError("Only users from " + AUTHORITY + " organization can set constraints");
    }

    // hash the secret
    byte[] secret = stub.getTransient().get("data");
    String salt = computeSalt();
    String hash = hashSecret(secret, salt);

    String key = LedgerState.key(ctx, CONSTRAINT, user);
    String state = stub.getStringState(key);

    // validate secret
    var constraint = ConstraintState.deserialize(user, hash, salt, state);
    constraint.validateSecret(secret);
    constraint.set(descriptor, value);

    String json = genson.serialize(constraint);
    stub.putStringState(key, json);

    return "Constraint set successfully: " + constraint;
  }

  @Transaction(intent = TYPE.SUBMIT)
  public String SetConstraintsImpl(Context ctx, String user, String descriptor, String orgMsp) {

    var access = new OrganizationAccess()
        .addRead(IMPLICIT + orgMsp)
        .addWrite(IMPLICIT + orgMsp);

    String value = genson.serialize(access);
    return SetConstraints(ctx, user, descriptor + "." + orgMsp, value);
  }

  @Transaction(intent = TYPE.SUBMIT)
  public String ClearConstraints(Context ctx) {
    if(!ctx.getClientIdentity().getMSPID().equals(AUTHORITY)) {
      throw new ChaincodeError("Only users from " + AUTHORITY + " organization can set constraints");
    }

    String key = LedgerState.key(ctx, CONSTRAINT, getUser(ctx));
    ctx.getStub().delState(key);

    return "Constraints cleared";
  }

  /**
   * Only endorsed by organization having access to uncensored data
   * CreatorMSPs should take care that uploaded data is not revealed via endorsement
   *
   * @param collection collection to which the information should be uploaded
   * @param dataSubject fabric identity on which to return data
   * @return status message
   */
  @Transaction(intent = TYPE.SUBMIT)
  public String Set(Context ctx, String dataSubject, String collection, String descriptor) {
    dataSubject = dataSubject.toLowerCase();

    var stub = ctx.getStub();
    String state = stub.getPrivateDataUTF8(collection, UserState.key(dataSubject));
    var record = UserState.deserialize(dataSubject, state);

    // check if client has write access
    var access = getAccess(ctx, dataSubject, descriptor);
    access.accessWrite(collection);

    // update existing data
    String input = new String(stub.getTransient().get("data"));
    record.set(descriptor, input);
    stub.putPrivateData(collection, record.getKey(), genson.serialize(record));

    // record upload event to a ledger
    recordEvent(ctx, StateType.UPLOAD, dataSubject, collection);
    return "Upload successful";
  }

  @Transaction(intent = TYPE.SUBMIT)
  public String SetImpl(Context ctx, String dataSubject, String descriptor) {
    String collection = IMPLICIT + ctx.getStub().getMspId();
    return Set(ctx, dataSubject, collection, descriptor);
  }



  /**
   * Function returning consented data based on constraints, client and peer MSPs, queried dataSubject
   * This function may be called on multiple peers to collect distributed data from different orgs
   *
   * @param collection which collection to query data from
   * @param dataSubject which data subject should the query return
   * @return consented data
   */
  @Transaction(intent = TYPE.SUBMIT)
  public String Get(Context ctx, String dataSubject, String collection) {
    var stub = ctx.getStub();
    dataSubject = dataSubject.toLowerCase();

    // check if client has full access to user record
    var access = getAccess(ctx, dataSubject, "");
    access.accessRead(collection);

    // parse data
    String data = stub.getPrivateDataUTF8(collection, UserState.key(dataSubject));

    // record query event to a ledger
    recordEvent(ctx, StateType.QUERY, dataSubject, collection);
    return data;
  }

  @Transaction(intent = TYPE.SUBMIT)
  public String GetImpl(Context ctx, String dataSubject) {
    String collection = IMPLICIT + ctx.getStub().getMspId();
    return Get(ctx, dataSubject, collection);
  }


  @Transaction(intent = TYPE.SUBMIT)
  public String Purge(Context ctx, String collection) {
    String dataSubject = getUser(ctx);

    // check if allowed to purge
    var stub = ctx.getStub();
    if(!ctx.getClientIdentity().getMSPID().equals(AUTHORITY)) {
      throw new ChaincodeError("Only users from " + AUTHORITY + " organization can purge private data");
    }

    // purge
    stub.purgePrivateData(collection, UserState.key(dataSubject));

    // record purge event to a ledger
    recordEvent(ctx, StateType.PURGE, dataSubject, collection);
    return "Data purged";
  }

  @Transaction(intent = TYPE.SUBMIT)
  public String PurgeImpl(Context ctx) {
    return Purge(ctx, IMPLICIT + ctx.getStub().getMspId());
  }


  @Transaction(intent = TYPE.EVALUATE)
  public String ReadLedger(Context ctx, String prefix) {
    var sb = new StringBuilder();

    var query = new CouchQuery()
        .term("selector", Map.of(
            "type", prefix
        ));

    var result = queryLedger(ctx, query);
    for(var e : result.entrySet()) {
      sb.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
    }
    return sb.toString();
  }


  @Transaction(intent = TYPE.EVALUATE)
  public String History(Context ctx, String dataSubject) {
    var stub = ctx.getStub();

    String key = LedgerState.key(ctx, EVENT, dataSubject);
    var sb = new StringBuilder();
    stub.getHistoryForKey(key).forEach(mod -> {
      sb.append("Tx: ").append(mod.getTxId()).append("\n")
          .append("[").append(mod.getTimestamp()).append("]: ")
          .append(mod.getStringValue()).append("\n");
    });
    return sb.toString();
  }


  @Transaction(intent = TYPE.SUBMIT)
  public String Query(Context ctx, String collection, String query) {
    var couchQuery = new CouchQuery(query);

    var res = queryCollection(ctx, collection, couchQuery);
    res.forEach((k, v) -> {

      // check if client has access to query
      var state = UserState.deserialize("", v);
      var access = getAccess(ctx, state.getUser(), "");
      access.accessRead(collection);
    });

    // record events for all data subjects that have been read
    return res.toString();
  }

  @Transaction(intent = TYPE.SUBMIT)
  public String QueryImpl(Context ctx, String query) {
    return Query(ctx, IMPLICIT + ctx.getStub().getMspId(), query);
  }

  @Transaction(intent = TYPE.EVALUATE)
  public String QueryLedger(Context ctx, String query) {
    return queryLedger(ctx, new CouchQuery(query)).toString();
  }


  public static Map<String, String> queryLedger(Context ctx, CouchQuery query) {
    var stub = ctx.getStub();
    var res = new HashMap<String, String>();

    stub.getQueryResult(query.toString()).forEach(kv -> {

      log.info("key: " + kv.getKey());
      log.info("val: " + kv.getStringValue());

      var keyList = new LinkedList<>(stub.splitCompositeKey(kv.getKey()).getAttributes());
      res.put(keyList.getLast(), kv.getStringValue());
    });
    return res;
  }

  public static Map<String, String> queryCollection(Context ctx, String collection, CouchQuery query) {
    var stub = ctx.getStub();

    var res = new HashMap<String, String>();
    stub.getPrivateDataQueryResult(collection, query.toString()).forEach(kv -> {

      log.info("key: " + kv.getKey());
      log.info("val: " + kv.getStringValue());

      res.put(kv.getKey(), kv.getStringValue());
    });
    return res;
  }

  public static void recordEvent(Context ctx, StateType type, String dataSubject, String collection) {
    var stub = ctx.getStub();
    String key = LedgerState.key(ctx, EVENT, dataSubject);
    String clientMsp = ctx.getClientIdentity().getMSPID();

    var event = new ChaincodeEvent(type, clientMsp, collection);
    stub.putStringState(key, genson.serialize(event));
  }

  public static String getUser(Context ctx) {
    var clientId = ctx.getClientIdentity().getId();
    var match = Pattern.compile("(?<=x509::CN=)(.+?)(?=,)").matcher(clientId);
    if(!match.find()) {
      throw new ChaincodeError("No client identity found");
    }
    return match.group().toLowerCase();
  }

  public static OrganizationAccess getAccess(Context ctx, String dataSubject, String descriptor) {
    var stub = ctx.getStub();
    String key = LedgerState.key(ctx, CONSTRAINT, dataSubject);
    String constraintState = stub.getStringState(key);
    var state = genson.deserialize(constraintState, LedgerState.class);

    if(!descriptor.isBlank()) descriptor += ".";
    return OrganizationAccess.deserialize(
        state.get(descriptor + stub.getMspId()));
  }

  public static String computeSalt() {
    byte[] salt = new byte[32];
    random.nextBytes(salt);
    return Hex.encodeHexString(salt);
  }

  public static String hashSecret(byte[] secret, String salt) {
    String stringSecret = Hex.encodeHexString(secret);
    return DigestUtils.sha256Hex(stringSecret + salt);
  }








//  /**
//   * Submit query to the network to receive responses from multiple organizations
//   * storing relevant information. Can be endorsed by any organization
//   *
//   * @param query data required from the response
//   * @return query with constraints applied
//   */
//  @Transaction(intent = TYPE.SUBMIT)
//  public String Query(Context ctx, String query) {
//
//    var stub = ctx.getStub();
//    String clientMsp = ctx.getClientIdentity().getMSPID();
//
//    //  apply consent constraints
//    //  re-write query based on client identity
//    //  endorsed by many orgs
//
//    String[] terms = query.split(",");
//    String[] constrained = new String[terms.length / 2 + terms.length % 2];
//
//    int j = 0;
//    for(int i = 0; i < terms.length; i += 2) {
//      constrained[j++] = terms[i];
//    }
//
//    //  set query event
//    //  listen for the event via gateway
//    //  Upload(q) queried filtered information
//
//    var newQuery = String.join(",", constrained);
//    var key = stub.createCompositeKey(QUERY_PREFIX, IMPLICIT + clientMsp);
//    stub.putStringState(key.toString(), newQuery);
//
//    return "Query " + newQuery + " submitted";
//  }



//  @Transaction(intent = TYPE.SUBMIT)
//  public String AddEndorserOrg(Context ctx, String collection, String term, String msp) {
//    var stub = ctx.getStub();
//    byte[] currentEp = stub.getPrivateDataValidationParameter(collection, term);
//    byte[] newEp = updateEndorsementPolicy(currentEp, msp);
//    stub.setPrivateDataValidationParameter(collection, term, newEp);
//
//    return "New ep: " + new String(newEp);
//  }
//
//  @Transaction(intent = TYPE.SUBMIT)
//  public String RemovePolicy(Context ctx, String collection, String term) {
//    var stub = ctx.getStub();
//    stub.setPrivateDataValidationParameter(collection, term, null);
//    return "Ep removed";
//  }
//
//
//
//  private byte[] updateEndorsementPolicy(byte[] oldEp, String... orgs) {
//    var ep = StateBasedEndorsementFactory.getInstance().newStateBasedEndorsement(oldEp);
//    ep.addOrgs(RoleType.RoleTypeMember, orgs);
//
//    return ep.policy();
//  }

}
