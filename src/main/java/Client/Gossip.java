package Client;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Gossip extends Thread {

    public static   int gossipThreadStartingDelay=1000; //10s
    public static   int gossipPeriod =10000; //10s
    public static Node node;
    public static DatagramSocket ds;
    public static DatagramSocket socket;

    public Gossip(Node nodeRecieve){
        node = nodeRecieve;
        ds = node.ds;
        socket = node.socket;
    }

    @Override
    public void run(){
            sendGossip();
    }

    public static void sendGossip(){ //scheduler to schedule gossip sending interval
        Timer timer=new Timer();
        TimerTask task=new TimerTask() {
            @Override
            public void run() {
                sendGossipsToNeighbours();
            }
        };
        timer.schedule(task,gossipThreadStartingDelay, gossipPeriod);
    }

    public static void sendGossipsToNeighbours(){ //send gossip to neighbours asking for IPs
        if (node.myNeighbours.size() <3 ) {

            ArrayList<Node> allNeighbours = new ArrayList<>();
            allNeighbours.addAll(node.myNeighbours);


            for (Node node : allNeighbours) {
                sendNeighboursToNeighbourMessage(node);
            }

        }
    }

    public static void sendNeighboursToNeighbourMessage(Node nodeToBeSent){ //gossip request create
        InetAddress myip = null;
        try {
            ds = new DatagramSocket();
            myip = InetAddress.getByName(nodeToBeSent.getIp());
            int port = nodeToBeSent.getPort();
            String request="GOSSIP "+node.ip+" "+node.port;
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

}
