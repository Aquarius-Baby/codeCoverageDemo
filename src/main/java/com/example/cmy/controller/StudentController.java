package com.example.cmy.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.cmy.entity.Student;
import com.example.cmy.service.IStudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class StudentController {

    @Autowired
    IStudentService studentService;


    @GetMapping("/count")
    public JSONObject count(@RequestParam(value = "name", required = false) String name) {
        JSONObject result = new JSONObject();
        Integer res = studentService.countStudent(name);
        result.put("code", 200);
        result.put("data", res);
        return result;
    }

    @PostMapping("/addStudent")
    public JSONObject addStudent(@RequestBody Student student) {
        JSONObject result = new JSONObject();
        Integer res = studentService.addStudent(student);
        result.put("code", 200);
        result.put("data", res);
        return result;
    }
}
