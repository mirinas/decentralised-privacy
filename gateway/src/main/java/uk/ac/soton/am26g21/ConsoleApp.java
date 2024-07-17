package uk.ac.soton.am26g21;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import lombok.extern.java.Log;
import org.hyperledger.fabric.client.GatewayException;
import uk.ac.soton.am26g21.gateway.Gateway;


@Log
public class ConsoleApp {

    public static void main(String[] args) {

        var scan = new Scanner(System.in);
        Gateway gateway;

        String[] endorsers = new String[0];
        byte[] trans = new byte[0];


        try {
            log.info("Enter user and peer:");

            gateway = new Gateway(scan.next(), scan.next());
            log.info("Connected to " + gateway.getConnection().getEndpoint());

        } catch (IOException e) {
            log.severe("Error reading peer config");
            return;
        } catch (Exception e) {
            log.severe("Error enrolling user");
            return;
        }


        scan.nextLine();
        while(true) {
            String line = scan.nextLine();

            try {

                var params = new ArrayList<>(Arrays.asList(line.split(" ")));
                String function = params.remove(0);

                switch (function) {
                    case "listen" -> {
                        log.info("Listening...");
                        gateway.listen();
                    }
                    case "user" -> {
                        if(params.isEmpty()) {
                            log.severe("No user specified");
                            break;
                        }
                        gateway.setUser(params.get(0));
                        log.info("Connected to " + gateway.getConnection().getEndpoint());
                    }
                    case "trans" -> {
                        if(params.isEmpty()) {
                            log.severe("No transient input specified");
                            break;
                        }
                        trans = params.get(0).getBytes();
                    }
                    case "endorse" -> endorsers = params.toArray(new String[0]);
                    case "quit", "q" -> {
                        log.info("Shutting down gateway");
                        gateway.close();
                        return;
                    }
                    default -> {
                        byte[][] input = params.stream()
                            .map(String::getBytes).toArray(byte[][]::new);

                        byte[] response;

                        // query if function starts with a dot
                        if(function.charAt(0) == '.') {
                            function = function.substring(1);
                            response = gateway.query(function, input, endorsers);

                        // otherwise invoke
                        } else {
                            response = gateway.invoke(function, input, trans, endorsers);
                            trans = new byte[0];
                        }

                        log.info("Response: \n" + new String(response));
                    }
                }


            } catch (GatewayException e) {
                log.severe("Chaincode exception: " + e.getMessage());
            } catch (IOException e) {
                log.severe("Error reading config files: " + e.getMessage());
            } catch (Exception e) {
                log.severe("Gateway error: " + e.getMessage());
            }
        }
    }
}
