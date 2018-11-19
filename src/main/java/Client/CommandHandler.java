package Client;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class CommandHandler {
    private Node node;

    public CommandHandler(Node node){
        this.node = node;
    }

    public void execute(String command) throws IOException, NoSuchAlgorithmException {
        switch (command.split(" ")[0]){
            case "routing":
                node.showRoutingTable();
                break;
            case "unregister":
                try {
                    node.unregister();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "register":
                try {
                    node.register();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "join":
                try {
                    node.join();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "search":
                try {
                    String[] commandArr = command.split(" ");
                    String fileName = "";
                    for(int i=1; i<commandArr.length; i++)
                        fileName += " "+ commandArr[i];
                    System.out.println("Searching:"+fileName.trim());
                    node.search(fileName.trim());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "files":
                node.showResources();
                break;
            case "leave":
                node.leave();
                break;
            case "download":
                String[] commandArr = command.split(" ");
                String ip = commandArr[1];
                String port = commandArr[2];
                String fileName = "";
                for(int i=3; i<commandArr.length; i++)
                    fileName += " "+ commandArr[i];
                System.out.println(fileName.trim());
                node.download(ip, port, fileName.trim());
            default:
                //System.out.println("False command!");
        }
    }
}
