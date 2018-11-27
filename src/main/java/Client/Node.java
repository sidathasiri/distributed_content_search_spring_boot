package Client;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContext;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Node implements Runnable{

    public String ip;
    public int port;
    public String username;
    public ArrayList<Node> myNeighbours = new ArrayList<>();
    public HashMap<String,Node> availableNeighbours = new HashMap<>();
    private ArrayList<String> resources = new ArrayList<>();
    DatagramSocket ds;
    public  int routingTableStatus=0;
    public  int gossipSendingStatus=0;
    public DatagramSocket socket = null;

    @Autowired
    ServletContext context;


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

    public  boolean isEqual(String ip,int port){
        if(this.port==port && this.ip.equals(ip)){
            return true;
        }return false;
    }

    public String getKey(){
        return ip+":"+port;
    }


    private ArrayList<String> cleanArray(String[] arr){
        ArrayList<String> cleanedList = new ArrayList<>();
        for(String i:arr){
            if(!i.trim().equals(""))
                cleanedList.add(i.trim());
        }

        return cleanedList;
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

    @Override
    public String toString() {
        return "Node{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                '}';
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
                StringTokenizer st = new StringTokenizer(received, " ");
                String encodeLength= st.nextToken();

                switch (st.nextToken()){

                    case "JOIN":
                        System.out.println(this.port+": join request "+received);

                        String newNodeIp = received.split(" ")[2];
                        int newNodePort = Integer.parseInt(received.split(" ")[3]);

                        if(!isNeighbour(newNodeIp, newNodePort)){
                            addToRoutingTable(new Node(newNodeIp, newNodePort));
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
                                    foundFiles.add(file_Name);
                                    break;
                                }
                            }
                            if (file_Name.equalsIgnoreCase(fileName)){ //chek for hall file name in selected files
                                foundFiles.add(file_Name);
                                break;
                            }
                        }

                        Set<String> searchResults = new HashSet<>(foundFiles);


                        if(foundFiles.isEmpty()) {
                            System.out.println(this.port + ": I dont have " + fileName);
                            try {
                                if(newHops<=4)
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
//                      0114 SEROK 3 129.82.128.1 2301 baby_go_home.mp3 baby_come_back.mp3 baby.mpeg
                        String[] result = received.split("\"");
                        ArrayList<String> cleanedArr = cleanArray(result);
                        String resultCommand = cleanedArr.get(0);
                        int resultFiles = Integer.parseInt(resultCommand.split(" ")[2]);
                        String foundIp = resultCommand.split(" ")[3];
                        String foundPort = resultCommand.split(" ")[4];
                        String foundHops = resultCommand.split(" ")[5];
                        System.out.println(resultFiles+" results found from "+foundIp+":"+foundPort+" in "+foundHops+" hops");
                        System.out.print("File Names:");
                        for(int i=1; i<cleanedArr.size(); i++){
                            System.out.print(cleanedArr.get(i)+" ");
                        }
                        System.out.println("\n----------------------------------------------------");
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
                                receiverIP = InetAddress.getByName(ip);

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

                    case "GOSSIP":
                        sendNeighbours(st);
                        break;

                    case "GOSSIPOK":
                        handleGossip(st);
                        break;

                    case "ISACTIVE":
                        checkHearBeat(st);
                        break;

                    case "ACTIVE":
                        addPulseNeighbours(st);
                        break;


                    default:
                        System.out.println("Invalid Command");
                        break;
                }
            }
        }catch (BindException ex){
            System.out.println("This is already registered! Try a different one or un-regiter first");
        }catch (SocketException e) {
            e.printStackTrace();
        }
    }


    public void register() throws IOException {

        ds = new DatagramSocket();
        String msg = "REG "+this.ip+" "+this.port+" "+this.username;
        String length = String.valueOf(msg.length()+5);
        length = String.format("%4s", length).replace(' ', '0');
        msg = length + " " + msg;
        byte b[] = msg.getBytes();     //request to register

        InetAddress ip = InetAddress.getByName("192.168.43.102");
        int port = 55555;

        DatagramPacket packet = new DatagramPacket(b, b.length, ip, port);
        ds.send(packet);
        System.out.println("reg sent");

        addNeighboursAfterRegister();
        showRoutingTable();
        join();

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

        InetAddress ip = InetAddress.getByName("192.168.43.102");
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


        for(Node n: myNeighbours){
            int port = n.getPort();
            InetAddress ip = InetAddress.getByName(n.getIp());
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


        for(Node n: myNeighbours){
            int port = n.getPort();
            InetAddress ip = InetAddress.getByName(n.getIp());
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

        InetAddress ip = InetAddress.getByName(receiverIP);
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
                        addToRoutingTable(new Node(nodeIp, nodePort));
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
            InetAddress ip = InetAddress.getByName(node.getIp());
            int port = node.getPort();

            DatagramPacket packet = new DatagramPacket(msg, msg.length, ip, port);
            ds.send(packet);
        }
        showRoutingTable();
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
            InetAddress ip = InetAddress.getByName(node.getIp());
            int port = node.getPort();

            DatagramPacket packet = new DatagramPacket(msg, msg.length, ip, port);
            ds.send(packet);
        }
        myNeighbours.clear();
        unregister();
    }

    public void download(String ip, String port, String name) throws IOException, NoSuchAlgorithmException {
        try {
            System.out.println("Startded downloading...");
            URL url = new URL("http://"+ip+":"+port+"/files/download?name="+name);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(15000);
            con.setReadTimeout(15000);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            if(content.toString().length()>0) {
                String path = "static/downloaded";
                ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                path = "C:\\Users\\YD\\Desktop\\distributed_content_search_spring_boot\\src\\main\\resources\\static\\created_files\\"+name.replace("%20", " ")+".txt";
                FileOutputStream fileOutputStream = new FileOutputStream(path);
                byte dataBuffer[] = new byte[1024];
                int bytesRead;
                fileOutputStream.write(content.toString().getBytes());

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
            } else {
                System.out.println("No data retirevied! File not may exist at node");
            }

        } catch (java.net.SocketTimeoutException e) {
            System.out.println("Connection timeout! Node may have down. Try another");
            removeNeighbour(ip, Integer.parseInt(port));
        } catch (ConnectException e){
            System.out.println("Node is down. Try another!");
            removeNeighbour(ip, Integer.parseInt(port));
        } catch (MalformedURLException ex){
            System.out.println("Error in the command");
        } catch (SocketException ex){
            System.out.println("Connection lost! Node may down");
        }catch (UnknownHostException ex){
            System.out.println("Error in IP or PORT");
        }
    }

    private void removeNeighbour(String ip, int port){
        int removingIndex = -1;
        for(int i =0; i<myNeighbours.size(); i++){
            if(myNeighbours.get(i).getIp().equals(ip) && myNeighbours.get(i).getPort() == port){
                removingIndex = i;
            }
        }

        if(removingIndex>=0){
            myNeighbours.remove(removingIndex);
        }
    }

    public void sendNeighbours(StringTokenizer st){

        String ip_of_sender=st.nextToken();
        int port_of_sender= Integer.parseInt(st.nextToken());
        Node senderNode=new Node(ip_of_sender,port_of_sender,"");
        String neighboursToBeSent = "";
        int count = 0;
        ArrayList<String> nodeKeys = new ArrayList<>();

        for (Node node:this.myNeighbours) {
            nodeKeys.add(node.ip+":"+node.port);
        }

        if (!nodeKeys.contains(senderNode.getKey())){
            addToRoutingTable(senderNode);
            System.out.println("Node IP " + senderNode.ip + " Port "+senderNode.port+ " was added by Request");
        }


        if(this.myNeighbours.size()>1){
                for (Node n : this.myNeighbours) {
                    if (!senderNode.isEqual(n.getIp(), n.getPort())) {
                        neighboursToBeSent += n.getIp() + " " + n.getPort() + " ";
                        count++;
                    } else {
                        continue;
                    }
                }
                neighboursToBeSent.substring(0, neighboursToBeSent.length() - 1); //remove last space
                sendNeighboursToNeighbourMessage(senderNode,count,neighboursToBeSent);


        }
    }


    public void sendNeighboursToNeighbourMessage(Node nodeToBeSent, int neighbourCount, String neighboursDetails){
        InetAddress myip = null;
        try {

            myip = InetAddress.getByName(nodeToBeSent.getIp());
            int port = nodeToBeSent.getPort();
            String request="GOSSIPOK "+this.ip+" "+this.port+" "+neighbourCount+" "+neighboursDetails;
            String length = String.valueOf(request.length()+5);
            length = String.format("%4s", length).replace(' ', '0');
            request = length + " " + request;
            byte[] msg = request.getBytes();
            DatagramPacket packet = new DatagramPacket(msg, msg.length, myip, port);
            ds.send(packet);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public  void handleGossip(StringTokenizer st) {


        if(this.myNeighbours.size()<3){
            String ip_of_sender=st.nextToken();
            int port_of_sender= Integer.parseInt(st.nextToken());
            int no_of_nodes_received=Integer.parseInt(st.nextToken());
            ArrayList<String> nodeKeys = new ArrayList<>();

            Node senderNode=new Node(ip_of_sender,port_of_sender,"");

            for (Node node:this.myNeighbours) {
                nodeKeys.add(node.ip+":"+node.port);
            }

            if (!nodeKeys.contains(senderNode.getKey())){
                System.out.println("Node IP " + senderNode.ip + " Port "+senderNode.port+ " was added by Gossip");
                addToRoutingTable(senderNode);
            }

            for (int i=0;i<no_of_nodes_received;i++){
                Node node=new Node(st.nextToken(),Integer.parseInt(st.nextToken()),"");

                if (nodeKeys.contains(node.getKey())){
                    continue;
                }else {
                    addToRoutingTable(node);
                    System.out.println("Node IP " + node.ip + " Port "+node.port+ " was added by Gossip");
                }

            }

        }
    }

    public void checkHearBeat(StringTokenizer st){

        String ip_of_sender=st.nextToken();
        int port_of_sender= Integer.parseInt(st.nextToken());
        Node senderNode=new Node(ip_of_sender,port_of_sender,"");
        sendNeighboursToPulseMessage(senderNode);
//        availableNeighbours.put(senderNode.ip+":"+senderNode.port,senderNode);
    }

    public void sendNeighboursToPulseMessage(Node nodeToBeSent){
        InetAddress myip = null;
        try {

            myip = InetAddress.getByName(nodeToBeSent.getIp());
            int port = nodeToBeSent.getPort();
            String request="ACTIVE "+this.ip+" "+this.port;
            String length = String.valueOf(request.length()+5);
            length = String.format("%4s", length).replace(' ', '0');
            request = length + " " + request;
            byte[] msg = request.getBytes();
            DatagramPacket packet = new DatagramPacket(msg, msg.length, myip, port);
            ds.send(packet);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addPulseNeighbours(StringTokenizer st){

        String ip_of_sender=st.nextToken();
        int port_of_sender= Integer.parseInt(st.nextToken());
        Node senderNode=new Node(ip_of_sender,port_of_sender,"");
//        sendNeighboursToPulseMessage(senderNode);
        availableNeighbours.put(senderNode.ip+":"+senderNode.port,senderNode);

    }


    public void addToRoutingTable(Node node){
        ArrayList<String> nodeKeys = new ArrayList<>();
        for (Node nodeVal:this.myNeighbours) {
            nodeKeys.add(nodeVal.ip+":"+nodeVal.port);
        }
        if (nodeKeys.contains(node.getKey())){
            return;
        }else {
            this.myNeighbours.add(node);
            routingTableStatus_plus1();
        }
    }


    public void routingTableStatus_plus1(){
        routingTableStatus++;
    }

    public  void setGossipSendingStatusToRoutingTableStatus(){
        gossipSendingStatus=routingTableStatus;
    }


    public  boolean hasRoutingTableIncreasedComparedToGossipStatus(){
        if (routingTableStatus>gossipSendingStatus){
            return true;
        }else{
            return false;
        }
    }


}
