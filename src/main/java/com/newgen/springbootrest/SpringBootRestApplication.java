package com.newgen.springbootrest;

import Client.CommandHandler;
import Client.Node;
import com.newgen.springbootrest.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

@SpringBootApplication
public class SpringBootRestApplication {
    private static int port;
    public static void main(String[] args) throws IOException {

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

        String[] sampleFiles = fileService.getAll();
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

    }

    public static int getPort(){
        return port;
    }
}
