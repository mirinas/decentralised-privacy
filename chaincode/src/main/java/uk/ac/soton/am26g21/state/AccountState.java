package main.java.uk.ac.soton.am26g21.state;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;
import main.java.uk.ac.soton.am26g21.contract.ChaincodeError;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.hyperledger.fabric.contract.Context;

import java.security.SecureRandom;

import static main.java.uk.ac.soton.am26g21.contract.PrivacyContract.genson;
import static main.java.uk.ac.soton.am26g21.contract.PrivacyContract.hashSecret;

@Log
@Getter
@Setter
@NoArgsConstructor
public class AccountState extends LedgerState {

    private String hashedSecret;
    private String salt;

    public AccountState(String username, byte[] secret) {
        super(StateType.ACCOUNT, username);
        state = null;

        var random = new SecureRandom();
        byte[] saltBytes = new byte[32];
        random.nextBytes(saltBytes);

        this.salt = Hex.encodeHexString(saltBytes);
        this.hashedSecret = DigestUtils.sha256Hex(Hex.encodeHexString(secret) + salt);
    }

    public void validateSecret(byte[] secret) {
        String hash = hashSecret(secret, salt);
        if(!this.hashedSecret.equals(hash)) {
            throw new ChaincodeError("Invalid user secret!");
        }
    }

    public static AccountState deserialize(String state) {
        if(state == null || state.isBlank()) {
            throw new ChaincodeError("User does not exist");
        }
        return genson.deserialize(state, AccountState.class);
    }

    public static String key(Context ctx, String username) {
        return key(ctx, StateType.ACCOUNT, username);
    }
}
