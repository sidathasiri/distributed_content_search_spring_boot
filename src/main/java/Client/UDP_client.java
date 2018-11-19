package Client;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.Scanner;


public class UDP_client {
    public static void main(String args[]) throws IOException, NoSuchAlgorithmException {

        Scanner scanner = new Scanner(System.in);
        Random randomNum = new Random();

        System.out.println("IP is 127.0.0.1");
        String ip = "127.0.0.1";

        System.out.print("Enter port:");
        int port = scanner.nextInt();

        scanner.nextLine();

        System.out.print("Ënter username:");
        String username = scanner.nextLine();

        Node node1 = new Node(ip, port, username);

        new Thread(node1).start();

        String[] sampleFiles = loadFileNames();
        int rand = 3 + randomNum.nextInt(6-3);
        for(int i=0; i<rand; i++) {
            int index = randomNum.nextInt(20);
            node1.addResource(sampleFiles[index], "/"+sampleFiles[index]);
        }
        node1.showResources();

        System.out.println(node1.getPort()+": registering");
        node1.register();

        CommandHandler commandHandler = new CommandHandler(node1);

        while (true){
            String command = scanner.nextLine();
            commandHandler.execute(command);
        }

        ////////////////////////////////////////////

//        Node node1 = new Node("127.0.0.1", 5000);
//        Node node2 = new Node("127.0.0.1", 5003);
//        Node node3 = new Node("127.0.0.1", 5006);
//
//        System.out.println(node1.getPort()+": registering");
//        node1.register();
//        node1.addResource("Harry Potter", "/harry");
//        new Thread(node1).start();
//
//        System.out.println(node2.getPort()+": registering");
//        node2.register();
//        node2.addResource("mario", "/mario");
//        node2.join();
//        new Thread(node2).start();
//
//        System.out.println(node3.getPort()+": registering");
//        node3.register();
//        node3.addResource("idea", "/idea");
//        node3.join();
//        new Thread(node3).start();
//
//        node3.search("mario");
    }

    public static String[] loadFileNames() throws FileNotFoundException, IOException {
        String files[] = new String[20];
        File file = new File("src/Client/resources/Files.txt");

        BufferedReader br = new BufferedReader(new FileReader(file));

        String st;
        int counter=0;
        while ((st = br.readLine()) != null){
            files[counter] = st;
            counter++;
        }
        br.close();
        return files;
    }
}
