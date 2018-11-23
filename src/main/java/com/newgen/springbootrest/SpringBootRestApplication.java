package com.newgen.springbootrest;

import Client.CommandHandler;
import Client.Gossip;
import Client.Node;
import com.newgen.springbootrest.service.FileService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.Scanner;

@SpringBootApplication
public class SpringBootRestApplication {
    private static int port;
    public static String[] servingFiles;
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        Scanner scanner = new Scanner(System.in);
        Random randomNum = new Random();
        FileService fileService = new FileService();

        System.out.println("IP is 127.0.0.1");
        String ip = "127.0.0.1";

        System.out.print("Enter port:");
        port = scanner.nextInt();

        SpringApplication.run(SpringBootRestApplication.class, args);

        scanner.nextLine();

        System.out.print("Ënter username:");
        String username = scanner.nextLine();

        Node node1 = new Node(ip, port, username);

        new Thread(node1).start();

        for(int i=0; i<servingFiles.length; i++) {
            node1.addResource(servingFiles[i], "/"+servingFiles[i]);
        }
        node1.showResources();

        System.out.println(node1.getPort()+": registering");
        node1.register();

        CommandHandler commandHandler = new CommandHandler(node1);

        Gossip gossip = new Gossip(node1);
        gossip.run();

        while (true){
            String command = scanner.nextLine();
            commandHandler.execute(command);
        }

    }

    public static int getPort(){
        return port;
    }
}
