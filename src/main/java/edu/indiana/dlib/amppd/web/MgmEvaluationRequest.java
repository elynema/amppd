package edu.indiana.dlib.amppd.web;

import lombok.Data;

import java.util.ArrayList;

@Data
public class MgmEvaluationRequest {
    private Long categoryId;
    private Long mstId;
    private ArrayList<MgmEvaluationParameterObj> parameters;
    private ArrayList<MgmEvaluationFilesObj> files;
}
