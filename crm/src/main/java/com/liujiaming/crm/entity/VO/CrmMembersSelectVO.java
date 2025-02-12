package com.liujiaming.crm.entity.VO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

/**
 * 团队成员查询VO
 *
 * @author liujiaming
 */
@Data
@ToString
@ApiModel("团队成员查询VO")
public class CrmMembersSelectVO {

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("昵称")
    private String realname;

    @ApiModelProperty("部门名称")
    private String deptName;

    @ApiModelProperty("权限")
    private Integer power;

    @ApiModelProperty("过期时间")
    private String expiresTime;
}
