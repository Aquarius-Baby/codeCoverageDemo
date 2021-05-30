package com.example.cmy.service;

import com.example.cmy.entity.Student;

public interface IStudentService {
    Integer countStudent(String name);

    Integer addStudent(Student student);
}
