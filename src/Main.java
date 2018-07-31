import java.io.*;

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

        String testDelete = "deleteThis.txt";

        FileWriter fileWriter2 = new FileWriter(testDelete);
        fileWriter2.write("Please please oh just please lord delete me\n");
        fileWriter2.close();

        TCPServer camsTCPServer = new TCPServer();

    }
}