import java.io.*;
import java.net.*;

public class Main {


    public static void main(String args[]) throws Exception {
        BufferedReader inFromUser =
                new BufferedReader(new InputStreamReader(System.in));

        String accountsFileName = "accounts.txt";

        FileWriter fileWriter = new FileWriter(accountsFileName);
        fileWriter.write("[U0]admin\n");
        fileWriter.write("[U1]user\n");
        fileWriter.write("[A1]account1\n");
        fileWriter.write("[A1]account2\n");
        fileWriter.write("[P1]password\n");
        fileWriter.write("[U3]user2\n");
        fileWriter.write("[A3]account3\n");
        fileWriter.close();

        TCPServer camsTCPServer = new TCPServer();
        //camsTCPServer.sendCommand(inFromUser.readLine());

    }
}