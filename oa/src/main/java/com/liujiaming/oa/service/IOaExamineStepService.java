package com.liujiaming.oa.service;

import com.liujiaming.core.servlet.BaseService;
import com.liujiaming.oa.entity.PO.OaExamineStep;

/**
 * <p>
 * 审批步骤表 服务类
 * </p>
 *
 * @author liujiaming
 * @since 2024-05-15
 */
public interface IOaExamineStepService extends BaseService<OaExamineStep> {


    /**
     固定审批
    查询下一个审批步骤
     */
    OaExamineStep queryExamineStepByNextExamineIdOrderByStepId(Integer categoryId, Integer examineStepId);
}
