package Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Pulse extends Thread {

    public static   int pulseThreadStartingDelay=1000; //10s
    public static   int pulsePeriod =10000; //10s
    public static Node node;
    public static DatagramSocket ds;
    public static DatagramSocket socket;

    public Pulse(Node nodeRecieve){
        node = nodeRecieve;
        ds = node.ds;
        socket = node.socket;
    }

    @Override
    public void run(){
        sendPulse();
    }

    public static void sendPulse(){ //scheduler to schedule pulse sending interval
        Timer timer=new Timer();
        TimerTask task=new TimerTask() {
            @Override
            public void run() {
                sendPulseToNeighbours();
            }
        };
        timer.schedule(task,pulseThreadStartingDelay, pulsePeriod);
    }

    public static void sendPulseToNeighbours(){ //send pulse to every neighbour
        if (node.myNeighbours.size() > 1) {

            ArrayList<Node> allNeighbours = new ArrayList<>();
            allNeighbours.addAll(node.myNeighbours);

            for (Node node : allNeighbours) {
                sendNeighboursToPulseMessage(node);
            }

           }

    }

    public static void sendNeighboursToPulseMessage(Node nodeToBeSent){ //creat pulse message
        InetAddress myip = null;
        try {
            ds = new DatagramSocket();
            myip = InetAddress.getByName(nodeToBeSent.getIp());
            int port = nodeToBeSent.getPort();
            String request="ISACTIVE "+node.ip+" "+node.port;
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
