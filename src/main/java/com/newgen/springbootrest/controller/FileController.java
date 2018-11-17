package com.newgen.springbootrest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import com.newgen.springbootrest.service.FileService;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;

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
    public String getOne(@RequestParam(value="name") String name){
        return fileService.getFile(name);
    }
}
