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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
