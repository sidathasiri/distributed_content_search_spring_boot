package com.newgen.springbootrest.service;

import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.*;

@Service
public class FileService {
    String files[] = new String[20];

    public FileService() throws IOException {
        File file = ResourceUtils.getFile("classpath:static/File_Names.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));

        String st;
        int counter=0;
        while ((st = br.readLine()) != null){
            files[counter] = st;
            counter++;
        }
        br.close();
    }

    public String getFile(String name){
        for(String i: files){
            if(i.equals(name)){
                return i;
            }
        }
        return null;
    }

    public String[] getAll() throws IOException {
        return files;
    }
}
