package com.newgen.springbootrest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import com.newgen.springbootrest.service.FileService;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
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
}
