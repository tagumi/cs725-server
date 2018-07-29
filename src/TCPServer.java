/**
 * Code has been adapted from Computer Networking: A Top-Down Approach Featuring
 * the Internet, second edition, copyright 1996-2002 J.F Kurose and K.W. Ross, 
 * All Rights Reserved.
 **/

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

class TCPServer {

    public boolean userAllowed = false;
    private ServerSocket welcomeSocket;
    private Socket connectionSocket;
    private DataOutputStream outToClient;
    private BufferedReader inFromClient;
    private boolean serverRunning = true;
    private String accountsFileName = "accounts.txt";
    private String usersNoPasswordName = "nopass.txt";
    private boolean requireUserID = true;
    private boolean prePassworded = false;
    private String currentUser = "";
    private String currentAccount = "";
    private boolean loggedIn = false;
    private char transferType = 'B';
    private char listFormat = 'F';
    private String currentDirectory = ".";
    private String currentWorkingDirectory = ".";

    public enum STATE{WAIT_CONN, WAIT_USER, WAIT_ACC, WAIT_PW, WAIT_COMMAND};

    private STATE serverState = STATE.WAIT_CONN;

    public TCPServer() throws Exception
    {
        String capitalizedSentence;
        String[] parsedCommand;
        String positiveGreeting = "+MIT-XX SFTP Service";
        String negativeGreeting = "-MIT-XX Out to Lunch";

        welcomeSocket = new ServerSocket(42069);

        //busy waits for connection
        acceptConnection();
        //sends greeting
        if (serverRunning){
            sendCommand(positiveGreeting);
        } else {
            sendCommand(negativeGreeting);
        }


        serverState = STATE.WAIT_USER;

        while(true) {

            //wait and parse command from client, first command should be login
            String clientCommand = getClientCommand(inFromClient);

            //split command into string array
            parsedCommand = parseString(clientCommand);

            switch(serverState){
                case WAIT_USER:
                    if (tryLogin(parsedCommand)){
                        //determine next state
                        if (userNoPassword(parsedCommand)){
                            //Send message saying logged in
                            currentUser = getPrimaryArg(parsedCommand);
                            sendCommand("!" + getPrimaryArg(parsedCommand) + " logged in, you don't need password");
                            //set state to await command
                            serverState = STATE.WAIT_COMMAND;
                        } else {
                            //Send message asking for acc & pw
                            currentUser = getPrimaryArg(parsedCommand);
                            sendCommand("+" + getPrimaryArg(parsedCommand) + " ok, specify account or send password");
                            //set state to await acc selection
                            serverState = STATE.WAIT_ACC;
                        }
                    }
                    break;
                case WAIT_ACC:
                    tryAccount(parsedCommand);
                    break;
                case WAIT_PW:
                    tryPassword(parsedCommand);
                    break;
                case WAIT_COMMAND:
                    doCommand(parsedCommand);
                    break;
                default:
                    break;
            }

            System.out.println("Current State is " + serverState.toString() + "\n");
        }
    }

    private void doCommand(String[] parsedCommand) throws IOException {
        if(!validCommand(parsedCommand[0])){
            sendCommand("-5 Bad Message");
        }

        switch (parsedCommand[0]){
            case "TYPE":
                runTypeCommand(parsedCommand);
                break;
            case "LIST":
                runListCommand(parsedCommand);
                break;
        }
    }

    private void runListCommand(String[] parsedCommand) throws IOException {
        switch(getPrimaryArg(parsedCommand)){
            case "F":
                sendListStandard(parsedCommand);
                break;
            case "V":
                sendListVerbose(parsedCommand);
                break;
            default:
                sendCommand("-5 incorrect use of LIST, missing format setting");
                break;
        }
    }

    private void sendListVerbose(String[] parsedCommand) throws IOException {
        try{
            File directoryPath = new File(getDirectory(parsedCommand));
            File[] filesList = directoryPath.listFiles();
            sendDirectory("+" + getDirectory(parsedCommand));
            for(int i = 0; i < (filesList.length - 1); i++){
                String verboseFileMessage = "Name: " + filesList[i].getName();
                verboseFileMessage += filesList[i].getName() + " Last Modified: ";
                verboseFileMessage += String.valueOf(filesList[i].lastModified()) + " File Size: ";
                verboseFileMessage += String.valueOf(filesList[i].length());
                sendDirectory(verboseFileMessage);
            }
            String verboseFileMessage = "Name: " + filesList[(filesList.length-1)].getName();
            verboseFileMessage += filesList[(filesList.length-1)].getName() + " Last Modified: ";
            verboseFileMessage += String.valueOf(filesList[(filesList.length-1)].lastModified()) + " File Size: ";
            verboseFileMessage += String.valueOf(filesList[(filesList.length-1)].length());
            sendCommand(verboseFileMessage + "\r\n");
        } catch (Exception e){
            sendCommand("-" + e.toString());
        }
    }

    private void sendListStandard(String[] parsedCommand) throws IOException {
        try{
            File directoryPath = new File(getDirectory(parsedCommand));
            File[] filesList = directoryPath.listFiles();
            sendDirectory("+" + getDirectory(parsedCommand));
            for(int i = 0; i < (filesList.length - 1); i++){
                sendDirectory(filesList[i].getName());
            }
            sendCommand(filesList[(filesList.length-1)].getName() + "\r\n");
        } catch (Exception e){
            sendCommand("-" + e.toString());
        }
    }

    private String getDirectory(String[] parsedCommand){
        if (parsedCommand.length < 3){
            return currentDirectory;
        } else {
            currentDirectory = parsedCommand[2];
            return parsedCommand[2];
        }

    }

    private void runTypeCommand(String[] parsedCommand) throws IOException {
        switch(getPrimaryArg(parsedCommand)){
            case "A":
                transferType = 'A';
                sendCommand("+Using Ascii mode");
                break;
            case "B":
                transferType = 'B';
                sendCommand("+Using Binary mode");
                break;
            case "C":
                transferType = 'C';
                sendCommand("+Using Continuous mode");
                break;
            default:
                sendCommand("-Type not valid");
                break;
        }
    }

    private boolean validCommand(String command){
        List<String> goodCommands = Arrays.asList("USER", "ACCT", "PASS", "TYPE",
                "LIST", "CDIR", "KILL", "NAME", "DONE", "RETR", "STOR");
        return goodCommands.contains(command);
    }



    private void acceptConnection() throws IOException {
        //busy waits for a connection, creates connection socket
        connectionSocket = welcomeSocket.accept();

        inFromClient =
                new BufferedReader(new
                        InputStreamReader(connectionSocket.getInputStream()));

        outToClient =
                new DataOutputStream(connectionSocket.getOutputStream());
    }

    private void sendCommand(String command) throws IOException {
        if (this.outToClient != null){
            outToClient.writeBytes(command + "\0");
        }
    }

    private void sendDirectory(String directory) throws IOException {
        if (this.outToClient != null){
            outToClient.writeBytes(directory + "\r\n");
        }
    }

    private boolean tryLogin(String[] parsedCommand) throws IOException {
        if(!requireUserID){
            return true;
        } else if (!parsedCommand[0].equals("USER")){
            sendCommand("-5 Bad Message");
            return false;
        } else {
            ArrayList<String> list = getFileList(accountsFileName);
            for(String field : list){
                if(userEntry(field)){
                    if(valueFromField(field).equals(getPrimaryArg(parsedCommand))){
                        return true;
                    }
                }
            }
            System.out.println("not a listed user \n");
            sendCommand("-Invalid user-id, try again");
            return false;
        }

    }

    private boolean checkUserTagField(String field, String currentUser, char var) throws FileNotFoundException {
        if (field.charAt(1) == var){
            return getTagNum(field).equals(getTagNum(currentUser));
        } else {
            return false;
        }


    }

    private String getTagNum(String value) throws FileNotFoundException {
        ArrayList<String> list = getFileList(accountsFileName);
        for(String field : list){
            if(valueFromField(field).equals(value)){
                return field.substring(2,field.indexOf(']')-1);
            }
        }
        return "";
    }

    private boolean tryAccount(String[] parsedCommand) throws IOException {
        ArrayList<String> list = getFileList(accountsFileName);
        if (!(parsedCommand[0].equals("ACCT") || (parsedCommand[0].equals("PASS")))) {
            sendCommand("-5 Bad Message");
            return false;
        } else if (parsedCommand[0].equals("ACCT")){
            if(prePassworded){
                //check account name against users accounts
                for(String field : list){
                    if(checkUserTagField(field, currentUser, 'A')){
                        //login
                        currentAccount = getPrimaryArg(parsedCommand);
                        serverState = STATE.WAIT_COMMAND;
                        sendCommand("!Account valid, logged-in");
                        return true;
                    }
                }
                //account doesn't exist
                sendCommand("-Invalid account, try again");
                return false;
            } else {
                //check account against currentUser
                for(String field : list){
                    if(checkUserTagField(field, currentUser, 'A')){
                        //account exists get pw
                        currentAccount = getPrimaryArg(parsedCommand);
                        serverState = STATE.WAIT_PW;
                        sendCommand("+Account valid, send password");
                        return true;
                    }
                }
                //account doesn't exist
                sendCommand("-Invalid account, try again");
                return false;
            }
        } else {
            //if they've sent the password only, check pw against current user
            //check password against currentUser
            for(String field : list){
                if(checkUserTagField(field, currentUser, 'P')){
                    //account exists get acc
                    prePassworded = true;
                    sendCommand("+Send account");
                    return false;
                }
            }
            sendCommand("-Wrong password, try again");
            return false;
        }
    }

    private boolean tryPassword(String[] parsedCommand) throws IOException {
        ArrayList<String> list = getFileList(accountsFileName);
        if (!(parsedCommand[0].equals("PASS"))) {
            sendCommand("-5 Bad Message");
            return false;
        } else if (parsedCommand[0].equals("PASS")){
            //check password against currentUser
            for(String field : list){
                if(checkUserTagField(field, currentUser, 'P')){
                    //pw correct and for user
                    serverState = STATE.WAIT_COMMAND;
                    sendCommand("! Logged in");
                    return true;
                }
            }
        }
        sendCommand("-Wrong password, try again");
        return false;
    }

    private String getPrimaryArg(String[] parsedCommand){
        String arg = "";
        try {
            arg = parsedCommand[1];
            return arg;
        } catch(java.lang.ArrayIndexOutOfBoundsException e) {
            arg = "";
            return arg;
        }
    }

    private String valueFromField(String field){
        int x = field.indexOf("]");
        return field.substring(x+1);
    }

    private ArrayList<String> getFileList(String fileName) throws FileNotFoundException {
        Scanner s = new Scanner(new File(fileName));
        ArrayList<String> list = new ArrayList<String>();
        while (s.hasNext()){
            list.add(s.next());
        }
        s.close();
        return list;
    }

    private boolean userEntry(String entry){
        return (entry.substring(0,2).equals("[U"));
    }

    private String getClientCommand(BufferedReader inFromClient) throws IOException {
        String clientCommand = "";
        int charCount = 0;
        while(true){
            char ch = (char) inFromClient.read();
            //check for terminating null or too many chars
            if ((ch == '\0') || (charCount >= Integer.MAX_VALUE)){
                break;
            } else {
                clientCommand += ch;
                charCount++;
            }
        }
        return clientCommand;
    }

    private String[] parseString(String string){
        return string.split(" ");
    }

    private boolean userNoPassword(String[] parsedCommand) throws FileNotFoundException {
        ArrayList<String> list = getFileList(usersNoPasswordName);
        for(String field : list){
            if(userEntry(field)){
                if(valueFromField(field).equals(getPrimaryArg(parsedCommand))){
                    return true;
                }
            }
        }
        return false;
    }

    private void appendStringAccounts(String string){
        try(FileWriter fw = new FileWriter(accountsFileName, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            out.println(string);
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }

    }
}