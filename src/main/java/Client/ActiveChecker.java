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

    public static int activeCheckerThreadStartingDelay=30000; //10s
    public static int activeCheckerPeriod =30000; //10s
    public static Node node;
    public static DatagramSocket ds;
    public static DatagramSocket socket;
    public static int counter;


    public ActiveChecker(Node nodeRecieve){
        node = nodeRecieve;
        ds = node.ds;
        socket = node.socket;
    }

    @Override
    public void run(){
        checkAvailability();
    }

    public static void checkAvailability(){ //scheduler to schedule active checking command in interval
        Timer timer=new Timer();
        TimerTask task=new TimerTask() {
            @Override
            public void run() {
                checkNeighboursAvailability();
            }
        };
        timer.schedule(task,activeCheckerThreadStartingDelay, activeCheckerPeriod);
    }

    public static void checkNeighboursAvailability(){ //check availability of neighbours
        counter++;
        if (node.availableNeighbours.size() > 0) {
            ArrayList<String> nodeKeys = new ArrayList<>();
            int removingIndex = -1;
            for (Node node:node.myNeighbours) {
                nodeKeys.add(node.ip+":"+node.port);
            }

            for (String nodeKey : nodeKeys){ //identify missing neighbour index
                if(!node.availableNeighbours.containsKey(nodeKey)){
                    for(int i =0; i<node.myNeighbours.size(); i++){
                        if(node.myNeighbours.get(i).getKey().equals(nodeKey)){
                            removingIndex = i;
                        }
                    }
                    if(removingIndex>=0){ //remove missing index
                        System.out.println("Node IP "+node.myNeighbours.get(removingIndex).getIp()+ " Port "
                                +node.myNeighbours.get(removingIndex).getPort()+" was disconnected and remove from table");
                        node.myNeighbours.remove(removingIndex);
                        node.blacklist.add(nodeKey);

                    }
                }
            }
           node.availableNeighbours = new HashMap<>();

           }

        if(counter==5){ //reset blacklist after counter increment
            node.blacklist = new ArrayList<>();
            counter=0;
        }
    }


}
