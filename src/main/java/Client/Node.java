package Client;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContext;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Node implements Runnable{
    private String ip;
    private int port;
    private String username;
    public ArrayList<Node> myNeighbours = new ArrayList<>();
    @Autowired
    ServletContext context;

    private ArrayList<String> resources = new ArrayList<>();

    DatagramSocket ds;

    public  Node(String ip, int port, String username){
        this.ip = ip;
        this.port = port;
        this.username = username;
    }

    public  Node(String ip, int port){
        this.ip = ip;
        this.port = port;
    }

    public void addResource(String name, String url){
        this.resources.add(name);
    }

    @Override
    public void run() {
//        System.out.println(this.port+" port is listning...");
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(this.port);

            while (true){
                byte[] buffer = new byte[65536];
                DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
                try {

                    socket.receive(incoming);
                }catch (IOException e) {
                    e.printStackTrace();
                }

                byte[] data = incoming.getData();
                String received = new String(data, 0, incoming.getLength());
                System.out.println("received "+received);

                switch (received.split(" ")[1]){
                    case "JOIN":
                        System.out.println(this.port+": join request "+received);

                        String newNodeIp = received.split(" ")[2];
                        int newNodePort = Integer.parseInt(received.split(" ")[3]);

                        if(!isNeighbour(newNodeIp, newNodePort)){
                            myNeighbours.add(new Node(newNodeIp, newNodePort));
                        }

                        for(Node i: myNeighbours)
                            System.out.println(this.port+": neighbours "+i.toString());
                        break;
                    case "SER":
                        System.out.println(this.port+": search request "+received);
                        String[] splittedCommand = received.split("\"");
                        String command = splittedCommand[0];
                        String fileName = splittedCommand[1];
                        String hops = splittedCommand[2].trim();
                        int newHops = Integer.parseInt(hops)+1;

                        ArrayList<String> foundFiles=new ArrayList<>();

                        for (String file_Name:this.resources){
                            for (String word:file_Name.split(" ")){ //for space separated words in selected files
                                if (word.equalsIgnoreCase(fileName)){
                                    foundFiles.add(fileName);
                                    break;
                                }
                            }
                            if (file_Name.equalsIgnoreCase(fileName)){ //chek for hall file name in selected files
                                foundFiles.add(fileName);
                                break;
                            }
                        }

                        Set<String> searchResults = new HashSet<>(foundFiles);


                        if(foundFiles.isEmpty()) {
                            System.out.println(this.port + ": I dont have " + fileName);
                            try {
                                this.askNeighboursToSearch(fileName, command.split(" ")[2], command.split(" ")[3], String.valueOf(newHops));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            System.out.println(this.port + ": I have " + fileName);
                            try {
                                sendFilePathToRequester(searchResults, command.split(" ")[2], command.split(" ")[3], String.valueOf(newHops));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case "SEROK":
                        System.out.println("SEROK received by "+port);
                        break;
                    case "LEAVE":
                        String ip = received.split(" ")[2];
                        int port = Integer.parseInt(received.split(" ")[3]);
                        int removingIndex = -1;
                        for(int i =0; i<myNeighbours.size(); i++){
                            if(myNeighbours.get(i).getIp().equals(ip) && myNeighbours.get(i).getPort() == port){
                                removingIndex = i;
                            }
                        }
                        if(removingIndex>=0){
                            myNeighbours.remove(removingIndex);
                            System.out.println("removed node "+ip+":"+port);
                            String request = "LEAVEOK 0";
                            String length = String.valueOf(request.length()+5);
                            length = String.format("%4s", length).replace(' ', '0');
                            request = length + " " + request;
                            byte[] msg = request.getBytes();

                            InetAddress receiverIP = null;
                            try {
                                receiverIP = InetAddress.getByName("localhost");

                                DatagramPacket packet = new DatagramPacket(msg, msg.length, receiverIP, port);
                                ds.send(packet);
                            }catch (IOException e){
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("I dont have " + ip + ":" + port + " to remove");
                        }

                        //asking others to remove
//                        byte[] msg = ("0028 LEAVE "+ip+" "+String.valueOf(port)).getBytes();
//                        for(Node node:myNeighbours){
//                            InetAddress neighbourIP = null;
//                            try {
//                                neighbourIP = InetAddress.getByName("localhost");
//                                int neighbourPort = node.getPort();
//
//                                DatagramPacket packet = new DatagramPacket(msg, msg.length, neighbourIP, neighbourPort);
//                                ds.send(packet);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }

                        break;
                    case "LEAVEOK":
                        if (received.split(" ")[2].equals("0"))
                            System.out.println(this.ip+":"+this.port+" Leave succeful");
                        else
                            System.out.println("Leave failed!");
                        break;
                     default:
                         System.out.println("Ïn default");
                }
            }
        }catch (BindException ex){
            System.out.println("This is already registered! Try a different one or un-regiter first");
        }catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private Boolean isNeighbour(String ip, int port){
        boolean found = false;
        for(Node i:myNeighbours){
            if(i.getIp().equals(ip) && i.getPort()==port){
                System.out.println("neighbour found!");
                found = true;
                break;
            }
        }
        return found;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void register() throws IOException {
        ds = new DatagramSocket();
        String msg = "REG "+this.ip+" "+this.port+" "+this.username;
        String length = String.valueOf(msg.length()+5);
        length = String.format("%4s", length).replace(' ', '0');
        msg = length + " " + msg;
        byte b[] = msg.getBytes();     //request to register

        InetAddress ip = InetAddress.getByName("localhost");
        int port = 55555;

        DatagramPacket packet = new DatagramPacket(b, b.length, ip, port);
        ds.send(packet);
        System.out.println("reg sent");

        addNeighboursAfterRegister();
        showRoutingTable();

    }

    public void unregister() throws IOException{
        //length UNREG IP_address port_no username
       // this.myNeighbours.clear();
        ds = new DatagramSocket();
        String msg = "UNREG "+this.ip+" "+this.port+" "+this.username;
        String length = String.valueOf(msg.length()+5);
        length = String.format("%4s", length).replace(' ', '0');
        msg = length + " " + msg;
        byte b[] = msg.getBytes();     //request to register

        InetAddress ip = InetAddress.getByName("localhost");
        int port = 55555;

        DatagramPacket packet = new DatagramPacket(b, b.length, ip, port);
        ds.send(packet);


        byte[] buffer = new byte[512];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        ds.receive(response);      //get the server response
        String responseMsg = new String(buffer, 0, response.getLength());
        String responseMsgArr[] = responseMsg.split(" ");
//        System.out.println(responseMsg);

        if(responseMsgArr[1].equals("UNROK")){
            if (responseMsgArr[2].equals("0"))
                System.out.println(this.ip+":"+this.port+" UNREGISTER succeful");
            else
                System.out.println("UNREGISTER succesful!");
        }
    }

    public void search(String name) throws IOException {
//        ds = new DatagramSocket();
        String msg = "SER "+this.ip+" "+this.port+" \""+name+"\" 0";
        String length = String.valueOf(msg.length()+5);
        length = String.format("%4s", length).replace(' ', '0');
        msg = length + " " + msg;
        //0047 SER 129.82.62.142 5070 "Lord of the rings"
        byte b[] = msg.getBytes();

        InetAddress ip = InetAddress.getLocalHost();
        for(Node n: myNeighbours){
            int port = n.getPort();

            DatagramPacket packet = new DatagramPacket(b, b.length, ip, port);
            ds.send(packet);
        }
    }

    public void askNeighboursToSearch(String  fileName, String searcherIp, String searcherPort, String hops) throws IOException{
        String request = "SER "+searcherIp+" "+searcherPort+" \""+fileName+"\" "+hops;
        String length = String.valueOf(request.length()+5);
        length = String.format("%4s", length).replace(' ', '0');
        request = length + " " + request;
        System.out.println("aking neighbours:"+request);
        byte b[] = request.getBytes();
        String received = b.toString();
        System.out.println("asking neighbour received "+received);
        ds = new DatagramSocket();

        InetAddress ip = InetAddress.getByName("localhost");

        for(Node n: myNeighbours){
            int port = n.getPort();
            if(port!=Integer.parseInt(searcherPort) || !n.getIp().equals(searcherIp)) {
                System.out.println("asked neighbour:"+n.getPort());
                DatagramPacket packet = new DatagramPacket(b, b.length, ip, port);
                ds.send(packet);
            }
        }
    }

    public void sendFilePathToRequester(Set<String> fileName, String receiverIP, String receiverPort, String hops) throws IOException{
        String filesStr="";

        for (String result: fileName){
            filesStr+="\""+result+ "\" ";
        }

        System.out.println("filesStr:"+filesStr);

        String msg = "SEROK "+fileName.size()+" "+ip+" "+port+" "+hops+" "+filesStr;
        String length = String.valueOf(msg.length()+5);
        length = String.format("%4s", length).replace(' ', '0');
        msg = length + " " + msg;
        byte b[] = msg.getBytes();
        String received = b.toString();
        System.out.println("sending found results "+filesStr.trim());
        ds = new DatagramSocket();

        InetAddress ip = InetAddress.getByName("localhost");
        int port = Integer.parseInt(receiverPort);

        DatagramPacket packet = new DatagramPacket(b, b.length, ip, port);
        ds.send(packet);
    }

    public void addNeighboursAfterRegister() throws IOException {
        byte[] buffer = new byte[512];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        ds.receive(response);      //get the server response
        String responseMsg = new String(buffer, 0, response.getLength());
        System.out.println("REg response: "+responseMsg);
        String responseMsgArr[] = responseMsg.split(" ");
//        System.out.println(responseMsg);

        if(responseMsgArr[1].equals("REGOK")){
            int availableNodes = Integer.parseInt(responseMsgArr[2]);
            if(availableNodes == 9998){
                System.out.println("Failed, already registered to you, unregister first");
            } else if(availableNodes == 9999){
                System.out.println("Failed, there is some error in the command");
            }else if(availableNodes == 9997){
                System.out.println("Failed, registered to another user, try a different IP and port");
            }else if(availableNodes == 9996){
                System.out.println("Failed, can’t register. BS ful");
            }else if(availableNodes!=0){
                for(int i=3; i<responseMsgArr.length; i+=2){
                    String nodeIp = responseMsgArr[i];
                    int nodePort = Integer.parseInt(responseMsgArr[i+1]);
                    if(!isNeighbour(nodeIp, nodePort)) {
                        myNeighbours.add(new Node(nodeIp, nodePort));
                    }
                }
                for(Node i:myNeighbours)
                    System.out.println(this.port+": Neighbours"+i.toString());
            }else{
                System.out.println(this.port+": No neighbours yet");
            }
        }

    }

    public void join() throws IOException {
        String request = "JOIN "+this.ip+" "+this.port;
        String length = String.valueOf(request.length()+5);
        length = String.format("%4s", length).replace(' ', '0');
        request = length + " " + request;
        byte[] msg = request.getBytes();
        for(Node node:myNeighbours){
            InetAddress ip = InetAddress.getByName("localhost");
            int port = node.getPort();

            DatagramPacket packet = new DatagramPacket(msg, msg.length, ip, port);
            ds.send(packet);
        }
        showRoutingTable();
    }

    @Override
    public String toString() {
        return "Node{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }

    public void showRoutingTable(){
        System.out.println("Routing table of "+ip+":"+port);
        System.out.println("--------------------------------------");
        for(Node i:myNeighbours){
            System.out.println("IP: "+i.getIp()+"\t Post: "+i.getPort());
        }
    }

    public void showResources(){
        System.out.println("Stored files at "+ip+":"+port);
        System.out.println("---------------------------------------");
        resources.forEach((name) -> {
            System.out.println(name);
        });
    }

    public void leave() throws IOException{
        //0028 LEAVE 64.12.123.190 432
        String request = "LEAVE "+this.ip+" "+this.port;
        String length = String.valueOf(request.length()+5);
        length = String.format("%4s", length).replace(' ', '0');
        request = length + " " + request;
        System.out.println("leave:"+request);
        byte[] msg = request.getBytes();
        for(Node node:myNeighbours){
            InetAddress ip = InetAddress.getByName("localhost");
            int port = node.getPort();

            DatagramPacket packet = new DatagramPacket(msg, msg.length, ip, port);
            ds.send(packet);
        }
        myNeighbours.clear();
    }

    public void download(String ip, String port, String name) throws IOException, NoSuchAlgorithmException {
        try {
            System.out.println("Started downloading...");
            BufferedInputStream in = new BufferedInputStream(new URL("http://"+ip+":"+port+"/files/download?name="+name).openStream());
            if(in.available()>0) {
                String path = "static/downloaded";
                ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                path = classLoader.getResource(path).getPath().split("target")[0].substring(1)+"src/main/resources/static/downloaded/"+name.replace("%20", " ")+".txt";
                FileOutputStream fileOutputStream = new FileOutputStream(path);
                byte dataBuffer[] = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }
                Scanner scanner = new Scanner(new FileReader(path));
                StringBuilder sb = new StringBuilder();
                String outString;
                while (scanner.hasNext()) {
                    sb.append(scanner.next());
                }
                scanner.close();
                System.out.println("Download complete!");
                outString = sb.toString();
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(outString.getBytes(StandardCharsets.UTF_8));
                String encoded = Base64.getEncoder().encodeToString(hash);
                System.out.println("Downloaded file hash:" + encoded);
            } else
                System.out.println("Download Failed!");
        } catch (IOException e) {
            // handle exception
        }
    }
}
