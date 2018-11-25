package Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class ActiveChecker extends Thread {

    public static int gossipThreadStartingDelay=20000; //10s
    public static int gossipPeriod =30000; //10s
    public static Node node;
    public static DatagramSocket ds;
    public static DatagramSocket socket;

    public ActiveChecker(Node nodeRecieve){
        node = nodeRecieve;
        ds = node.ds;
        socket = node.socket;
    }

    @Override
    public void run(){
        checkAvailability();
    }

    public static void checkAvailability(){
        Timer timer=new Timer();
        TimerTask task=new TimerTask() {
            @Override
            public void run() {
                checkNeighboursAvailability();
            }
        };
        timer.schedule(task,gossipThreadStartingDelay, gossipPeriod);
    }

    public static void checkNeighboursAvailability(){
        if (node.availableNeighbours.size() > 0) {
            ArrayList<String> nodeKeys = new ArrayList<>();
            int removingIndex = -1;
            for (Node node:node.myNeighbours) {
                nodeKeys.add(node.ip+":"+node.port);
            }

            for (String nodeKey : nodeKeys){
                if(!node.availableNeighbours.containsKey(nodeKey)){
                    for(int i =0; i<node.myNeighbours.size(); i++){
                        if(node.myNeighbours.get(i).getKey().equals(nodeKey)){
                            removingIndex = i;
                        }
                    }
                    if(removingIndex>=0){
                        System.out.println("Node IP "+node.myNeighbours.get(removingIndex).getIp()+ " Port "
                                +node.myNeighbours.get(removingIndex).getPort()+" was disconnected and remove from table");
                        node.myNeighbours.remove(removingIndex);

                    }
                }
            }
           node.availableNeighbours = new HashMap<>();

           }

    }


}
