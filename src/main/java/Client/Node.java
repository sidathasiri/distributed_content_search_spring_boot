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
    public ArrayList<String> blacklist = new ArrayList<>();
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

    /**
     * @desc  clean a given array by removing empty strings
     * @param arr
     * @return cleaned array
     */
    private ArrayList<String> cleanArray(String[] arr){
        ArrayList<String> cleanedList = new ArrayList<>();
        for(String i:arr){
            if(!i.trim().equals(""))
                cleanedList.add(i.trim());
        }

        return cleanedList;
    }

    /**
     * @desc check whether it is a neighbour or not
     * @param ip
     * @param port
     * @return
     */
    private Boolean isNeighbour(String ip, int port){
        boolean found = false;
        for(Node i:myNeighbours){
            if(i.getIp().equals(ip) && i.getPort()==port){
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

    /**
     * @desc continously running thread listing for messages from other nodes
     */
    @Override
    public void run() {
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

                    //join message received
                    case "JOIN":
                        String newNodeIp = received.split(" ")[2];
                        int newNodePort = Integer.parseInt(received.split(" ")[3]);

                        if(!isNeighbour(newNodeIp, newNodePort)){
                            addToRoutingTable(new Node(newNodeIp, newNodePort));
                        }
                        break;

                     //search message received
                    case "SER":
                        String[] splittedCommand = received.split("\"");
                        String command = splittedCommand[0];
                        String fileName = splittedCommand[1];
                        String hops = splittedCommand[2].trim();
                        int newHops = Integer.parseInt(hops)+1;

                        ArrayList<String> foundFiles=new ArrayList<>();
                        //search by single word as given
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

                        //if did find file in current node ask neighbours to search
                        if(foundFiles.isEmpty()) {
                            try {
                                if(newHops<=4)      //setting hop limit in flooding
                                    this.askNeighboursToSearch(fileName, command.split(" ")[2], command.split(" ")[3], String.valueOf(newHops));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        //if file found send it to requester
                        else {
                            try {
                                sendFilePathToRequester(searchResults, command.split(" ")[2], command.split(" ")[3], String.valueOf(newHops));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    //search OK message received
                    case "SEROK":
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

                    //leave message received
                    case "LEAVE":
                        String ip = received.split(" ")[2];
                        int port = Integer.parseInt(received.split(" ")[3]);
                        int removingIndex = -1;
                        //search if current node has the neighbour
                        for(int i =0; i<myNeighbours.size(); i++){
                            if(myNeighbours.get(i).getIp().equals(ip) && myNeighbours.get(i).getPort() == port){
                                removingIndex = i;
                            }
                        }
                        //if have, remove from routing table
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
                                ds.send(packet);        //send response
                            }catch (IOException e){
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("I dont have " + ip + ":" + port + " to remove");
                        }
                        break;

                    //leave OK message received
                    case "LEAVEOK":
                        if (received.split(" ")[2].equals("0"))
                            System.out.println(this.ip+":"+this.port+" Leave succeful");
                        else
                            System.out.println("Leave failed!");
                        break;

                     //gossip message received
                    case "GOSSIP":
                        sendNeighbours(st);
                        break;

                    //gossip ok meeagse received
                    case "GOSSIPOK":
                        handleGossip(st);
                        break;

                    //isactive message from active checker received
                    case "ISACTIVE":
                        checkHearBeat(st);
                        break;
                    //active message received
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

    /**
     * @desc node regstering method
     * @throws IOException
     */
    public void register() throws IOException {
        ds = new DatagramSocket();
        String msg = "REG "+this.ip+" "+this.port+" "+this.username;
        String length = String.valueOf(msg.length()+5);
        length = String.format("%4s", length).replace(' ', '0');
        msg = length + " " + msg;
        byte b[] = msg.getBytes();     //request to register

        InetAddress ip = InetAddress.getByName("192.168.43.157");
        int port = 55555;

        DatagramPacket packet = new DatagramPacket(b, b.length, ip, port);
        ds.send(packet);    //send REG msg to bootstrap setver

        //add the nodes provided by bootstrap server as neighbours
        addNeighboursAfterRegister();
        showRoutingTable();
        join();     //send join request

    }

    /**
     * @desc node unregister method
     * @throws IOException
     */
    public void unregister() throws IOException{
        ds = new DatagramSocket();
        String msg = "UNREG "+this.ip+" "+this.port+" "+this.username;
        String length = String.valueOf(msg.length()+5);
        length = String.format("%4s", length).replace(' ', '0');
        msg = length + " " + msg;
        byte b[] = msg.getBytes();     //request to register

        InetAddress ip = InetAddress.getByName("192.168.43.157");
        int port = 55555;

        DatagramPacket packet = new DatagramPacket(b, b.length, ip, port);
        ds.send(packet);        //send unregister msg to server

        //handle response by server
        byte[] buffer = new byte[512];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        ds.receive(response);      //get the server response
        String responseMsg = new String(buffer, 0, response.getLength());
        String responseMsgArr[] = responseMsg.split(" ");

        if(responseMsgArr[1].equals("UNROK")){
            if (responseMsgArr[2].equals("0"))
                System.out.println(this.ip+":"+this.port+" UNREGISTER succeful");
            else
                System.out.println("UNREGISTER succesful!");
        }
    }

    /**
     * @desc search a given file
     * @param name
     * @throws IOException
     */
    public void search(String name) throws IOException {
        String msg = "SER "+this.ip+" "+this.port+" \""+name+"\" 0";
        String length = String.valueOf(msg.length()+5);
        length = String.format("%4s", length).replace(' ', '0');
        msg = length + " " + msg;
        byte b[] = msg.getBytes();

        //send message to neighbours to seearch
        for(Node n: myNeighbours){
            int port = n.getPort();
            InetAddress ip = InetAddress.getByName(n.getIp());
            DatagramPacket packet = new DatagramPacket(b, b.length, ip, port);
            ds.send(packet);
        }
    }

    /**
     * @desc ask neighbours to search with flooding
     * @param fileName
     * @param searcherIp
     * @param searcherPort
     * @param hops
     * @throws IOException
     */
    public void askNeighboursToSearch(String  fileName, String searcherIp, String searcherPort, String hops) throws IOException{
        String request = "SER "+searcherIp+" "+searcherPort+" \""+fileName+"\" "+hops;
        String length = String.valueOf(request.length()+5);
        length = String.format("%4s", length).replace(' ', '0');
        request = length + " " + request;
        byte b[] = request.getBytes();
        String received = b.toString();
        ds = new DatagramSocket();


        for(Node n: myNeighbours){
            int port = n.getPort();
            InetAddress ip = InetAddress.getByName(n.getIp());
            if(port!=Integer.parseInt(searcherPort) || !n.getIp().equals(searcherIp)) {
                DatagramPacket packet = new DatagramPacket(b, b.length, ip, port);
                ds.send(packet);
            }
        }
    }

    /**
     * @desc send the found file to requester
     * @param fileName
     * @param receiverIP
     * @param receiverPort
     * @param hops
     * @throws IOException
     */
    public void sendFilePathToRequester(Set<String> fileName, String receiverIP, String receiverPort, String hops) throws IOException{
        String filesStr="";

        for (String result: fileName){
            filesStr+="\""+result+ "\" ";
        }

        String msg = "SEROK "+fileName.size()+" "+ip+" "+port+" "+hops+" "+filesStr;
        String length = String.valueOf(msg.length()+5);
        length = String.format("%4s", length).replace(' ', '0');
        msg = length + " " + msg;
        byte b[] = msg.getBytes();
        String received = b.toString();
//        System.out.println("sending found results "+filesStr.trim());
        ds = new DatagramSocket();

        InetAddress ip = InetAddress.getByName(receiverIP);
        int port = Integer.parseInt(receiverPort);

        DatagramPacket packet = new DatagramPacket(b, b.length, ip, port);
        ds.send(packet);    //send results
    }

    /**
     * @desc adding the found nodes from server as neighbours
     * @throws IOException
     */
    public void addNeighboursAfterRegister() throws IOException {
        byte[] buffer = new byte[512];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        ds.receive(response);      //get the server response
        String responseMsg = new String(buffer, 0, response.getLength());
        String responseMsgArr[] = responseMsg.split(" ");

        //handle possible response cases
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

    /**
     * @desc join the network
     * @throws IOException
     */
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

    /**
     * @desc display the routung table
     */
    public void showRoutingTable(){
        System.out.println("Routing table of "+ip+":"+port);
        System.out.println("--------------------------------------");
        for(Node i:myNeighbours){
            System.out.println("IP: "+i.getIp()+"\t Post: "+i.getPort());
        }
    }

    /**
     * @desc display the serving files in current node
     */
    public void showResources(){
        System.out.println("Stored files at "+ip+":"+port);
        System.out.println("---------------------------------------");
        resources.forEach((name) -> {
            System.out.println(name);
        });
    }

    /**
     * @desc leave the network
     * @throws IOException
     */
    public void leave() throws IOException{
        String request = "LEAVE "+this.ip+" "+this.port;
        String length = String.valueOf(request.length()+5);
        length = String.format("%4s", length).replace(' ', '0');
        request = length + " " + request;
        byte[] msg = request.getBytes();
        //send neighbours about leave
        for(Node node:myNeighbours){
            InetAddress ip = InetAddress.getByName(node.getIp());
            int port = node.getPort();

            DatagramPacket packet = new DatagramPacket(msg, msg.length, ip, port);
            ds.send(packet);
        }
        myNeighbours.clear();   //clear my neighbours
        unregister();           //unregister from server
        System.exit(1);  //kill current node
    }

    /**
     * @desc download file
     * @param ip
     * @param port
     * @param name
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public void download(String ip, String port, String name) throws IOException, NoSuchAlgorithmException {
        try {
            System.out.println("Startded downloading...");
            //construct URL to send files
            URL url = new URL("http://"+ip+":"+port+"/files/download?name="+name);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");        //creating a GET request

            //set timeouts
            con.setConnectTimeout(15000);
            con.setReadTimeout(15000);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);  //read the response
            }
            in.close();

            //if data received write to file
            if(content.toString().length()>0) {
                String path = "static/downloaded";
                ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                path = classLoader.getResource(path).getPath().split("target")[0].substring(1)+"src/main/resources/static/downloaded/"+name.replace("%20", " ")+".txt";
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

                //calculate the hash
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(outString.getBytes(StandardCharsets.UTF_8));
                String encoded = Base64.getEncoder().encodeToString(hash);
                System.out.println("Downloaded file hash:" + encoded);
            } else {
                System.out.println("No data retirevied! File not may exist at node");
            }

            //handle error cases
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

    /**
     * @desc remove a existing neighbour
     * @param ip
     * @param port
     */
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
                    if(!this.blacklist.contains(node.getKey())){
                        addToRoutingTable(node);
                        System.out.println("Node IP " + node.ip + " Port "+node.port+ " was added by Gossip");
                    }
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
