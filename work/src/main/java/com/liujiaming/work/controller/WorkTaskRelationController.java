package com.liujiaming.work.controller;


import com.liujiaming.core.common.R;
import com.liujiaming.core.common.Result;
import com.liujiaming.work.entity.PO.WorkTaskRelation;
import com.liujiaming.work.service.IWorkTaskRelationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 任务关联业务表 前端控制器
 * </p>
 *
 * @author liujiaming
 * @since 2024-05-18
 */
@RestController
@RequestMapping("/workTaskRelation")
@Api(tags = "任务关联业务")
public class WorkTaskRelationController {
    @Autowired
    private IWorkTaskRelationService workTaskRelationService;

    @PostMapping("/saveWorkTaskRelation")
    @ApiOperation("保存任务业务关联")
    public Result saveWorkTaskRelation(@RequestBody WorkTaskRelation workTaskRelation){
        workTaskRelationService.saveWorkTaskRelation(workTaskRelation);
        return R.ok();
    }
}

