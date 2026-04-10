package com.lcyhz.urbanova.vo.scooter;

import lombok.Data;

import java.util.List;

@Data
public class BulkScooterStatusUpdateVo {
    private String status;
    private Integer updatedCount;
    private List<String> scooterIds;
}
