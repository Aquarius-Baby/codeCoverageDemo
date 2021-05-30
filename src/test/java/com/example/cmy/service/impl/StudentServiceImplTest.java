package com.example.cmy.service.impl;

import com.example.cmy.entity.Student;
import org.junit.Test;

import static org.junit.Assert.*;

public class StudentServiceImplTest {

    @Test
    public void addStudent() {
        Integer res = new StudentServiceImpl().addStudent(new Student());
        System.out.println(res);
     }
    @Test
    public void addStudent2() {
        Integer res = new StudentServiceImpl().addStudent(new Student());
        System.out.println(res);
     }
}