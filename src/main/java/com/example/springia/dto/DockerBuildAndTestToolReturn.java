package com.example.springia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DockerBuildAndTestToolReturn {
    private boolean allSuccess;
    private List<DockerBuildAndTestToolRepo> builds;

    public boolean isAllSuccess(){
        for (DockerBuildAndTestToolRepo build : builds) {
            if(!build.isSuccess()){
                return false;
            }
        }
        return true;
    }
}
