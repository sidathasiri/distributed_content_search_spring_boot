package com.newgen.springbootrest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import com.newgen.springbootrest.service.FileService;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletContext;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@RestController
@RequestMapping("/files")
public class FileController {
    @Autowired
    FileService fileService;

    //get all files
    @RequestMapping("/all")
    public String[] getAll() throws IOException {
        return fileService.getAllServingFiles();
    }

    //get a single file
    @RequestMapping("/file")
    public HashMap<String, String> getOne(@RequestParam(value="name") String name){
        HashMap<String, String> map = new HashMap<>();
        map.put("name", fileService.getFile(name));
        return map;
    }

    //download a file
    @RequestMapping(path = "/download", method = RequestMethod.GET)
    public ResponseEntity<Resource> download(@RequestParam(value="name") String name) throws IOException, NoSuchAlgorithmException {
        String[] servingFiles = fileService.getAllServingFiles();
        boolean isIncluded = false;
        for(String i: servingFiles){
            if(i.equalsIgnoreCase(name)){
                isIncluded = true;
                break;
            }
        }
        //check if file is srving
        if (isIncluded) {
            Random rand = new Random();
            int fileSize = (2 + rand.nextInt(8)) * 1024 * 1024;
            char[] chars = new char[fileSize];
            Arrays.fill(chars, 'a');

            String writingStr = new String(chars);

            //calculate hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(writingStr.getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.getEncoder().encodeToString(hash);

            System.out.println("File: " + name + "\nFile Size:" + fileSize / (1024 * 1024) + "Mb\nHash:" + encoded);

            //create random file
            ClassLoader classLoader = getClass().getClassLoader();
            String fileName = "C:\\Users\\Sidath\\IdeaProjects\\spring-boot-rest\\src\\main\\resources\\static\\created_files\\"+name+".txt";
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(writingStr);

            writer.close();

            HttpHeaders headers = new HttpHeaders();
            String headerValue = "attachment; filename=" + name + ".txt";
            headers.add(HttpHeaders.CONTENT_DISPOSITION, headerValue);

            File file = ResourceUtils.getFile(fileName);
//        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

            Path path = Paths.get(file.getAbsolutePath());
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
            //send created file
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .body(resource);
        } else{
            System.out.println("File does not exists!");
        }
        return null;
    }
}
