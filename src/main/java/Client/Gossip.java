package Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

    public static   void sendGossip(){


        Timer timer=new Timer();

        TimerTask task=new TimerTask() {
            @Override
            public void run() {
                sendGossipsToNeighbours();
            }
        };

        timer.schedule(task,gossipThreadStartingDelay, gossipPeriod);



    }

    public static void sendGossipsToNeighbours(){


        if (node.myNeighbours.size() > 1) {

            if (node.hasRoutingTableIncreasedComparedToGossipStatus()) { //gossip only if routing table has added some nodes

                node.setGossipSendingStatusToRoutingTableStatus();

                System.out.println("Routing Table Changed : Start Sending Gossip Message");

            ArrayList<Node> allNeighbours = new ArrayList<>();
            allNeighbours.addAll(node.myNeighbours);

            for (Node node : allNeighbours) {
                String neighboursToBeSent = "";
                int count = 0;
                for (Node n : allNeighbours) {
                    if (!node.isEqual(n.getIp(), n.getPort())) {
                        neighboursToBeSent += n.getIp() + " " + n.getPort() + " ";
                        count++;
                    } else {
                        continue;
                    }
                }
                neighboursToBeSent.substring(0, neighboursToBeSent.length() - 1); //remove last space

                sendNeighboursToNeighbourMessage(node, count, neighboursToBeSent);

            }
            System.out.println("");
        }
           }

    }

    public static void sendNeighboursToNeighbourMessage(Node nodeToBeSent, int neighbourCount, String neighboursDetails){
        InetAddress myip = null;
        try {

            myip = InetAddress.getByName("localhost");
            int port = nodeToBeSent.getPort();
            String request="GOSSIP "+node.ip+" "+node.port+" "+neighbourCount+" "+neighboursDetails;
            String length = String.valueOf(request.length()+5);
            length = String.format("%4s", length).replace(' ', '0');
            request = length + " " + request;
            byte[] msg = request.getBytes();
            DatagramPacket packet = new DatagramPacket(msg, msg.length, myip, port);
            try {
                ds.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }




}
