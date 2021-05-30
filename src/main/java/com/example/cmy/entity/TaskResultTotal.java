package com.example.cmy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TaskResultTotal implements Serializable {
  private String id;

  private String taskId;

  private Integer missInstructions;

  private Integer instructions;

  private Integer instructionsCov;

  private Integer missBranches;

  private Integer branches;

  private Integer branchesCov;

  private Integer missCxty;

  private Integer cxty;

  private Integer missLines;

  private Integer lines;

  private Integer missMethods;

  private Integer methods;

  private Integer missClasses;

  private Integer classes;

  private Timestamp updateTime;

  private static final long serialVersionUID = 1L;
}
