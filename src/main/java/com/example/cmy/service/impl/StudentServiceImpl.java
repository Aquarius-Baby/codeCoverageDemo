package com.example.cmy.service.impl;

import com.example.cmy.entity.Student;
import com.example.cmy.service.IStudentService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StudentServiceImpl implements IStudentService {
    @Override
    public Integer countStudent(String name) {
        System.out.println("countStudent..." + name);
        if (StringUtils.isEmpty(name)) {
            System.out.println("Name is Empty....");
        } else {
            System.out.println("count student by name ~~~~~");
        }
        return 0;
    }

    @Override
    public Integer addStudent(Student student) {
        System.out.println("AddStudent...");
        if (StringUtils.isEmpty(student.getAge())) {
            System.out.println("AGE is Empty....");
        } else {
            System.out.println("add student ~~~~~");
        }
        return 1;
    }
}
