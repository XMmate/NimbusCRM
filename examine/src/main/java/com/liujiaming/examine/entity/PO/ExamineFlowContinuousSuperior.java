package com.liujiaming.examine.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 审批流程连续多级主管审批记录表
 * </p>
 *
 * @author liujiaming
 * @since 2024-11-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wk_examine_flow_continuous_superior")
@ApiModel(value="ExamineFlowContinuousSuperior对象", description="审批流程连续多级主管审批记录表")
public class ExamineFlowContinuousSuperior implements Serializable {

    private static final long serialVersionUID=1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "审批流程ID")
    private Integer flowId;

    @ApiModelProperty(value = "角色ID")
    private Integer roleId;

    @ApiModelProperty(value = "角色审批的最高级别或者组织架构的第N级")
    private Integer maxLevel;

    @ApiModelProperty(value = "1 指定角色 2 组织架构的最上级")
    private Integer type;

    @ApiModelProperty(value = "批次ID")
    private String batchId;


}
