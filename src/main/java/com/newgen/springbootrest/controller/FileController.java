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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;

@RestController
@RequestMapping("/files")
public class FileController {
    @Autowired
    FileService fileService;

    @RequestMapping("/all")
    public String[] getAll() throws IOException {
        return fileService.getAllServingFiles();
    }

    @RequestMapping("/file")
    public HashMap<String, String> getOne(@RequestParam(value="name") String name){
        HashMap<String, String> map = new HashMap<>();
        map.put("name", fileService.getFile(name));
        return map;
    }

    @RequestMapping(path = "/download", method = RequestMethod.GET)
    public ResponseEntity<Resource> download(@RequestParam(value="name") String name) throws IOException {
        Random rand = new Random();
        int fileSize = (2 + rand.nextInt(8))*1024*1024;
        System.out.println(fileSize);
        char[] chars = new char[fileSize];
        Arrays.fill(chars, 'a');

        String writingStr = new String(chars);

        //random file
        BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\Sidath\\IdeaProjects\\spring-boot-rest\\src\\main\\resources\\static\\downloading_files\\"+name+".txt"));
        writer.write(writingStr);

        writer.close();

        HttpHeaders headers = new HttpHeaders();
        String headerValue = "attachment; filename="+name+".txt";
        headers.add(HttpHeaders.CONTENT_DISPOSITION, headerValue);

        File file = ResourceUtils.getFile("classpath:static/File_Names.txt");
//        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        Path path = Paths.get(file.getAbsolutePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource);
    }
}
