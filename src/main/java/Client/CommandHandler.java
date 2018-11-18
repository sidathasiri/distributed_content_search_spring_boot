package Client;

import java.io.IOException;

public class CommandHandler {
    private Node node;

    public CommandHandler(Node node){
        this.node = node;
    }

    public void execute(String command) throws IOException {
        switch (command.split(" ")[0]){
            case "show":
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
            default:
                System.out.println("False command!");
        }
    }
}
