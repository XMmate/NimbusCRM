package com.liujiaming.examine.entity.VO;

import com.liujiaming.core.feign.admin.entity.SimpleDept;
import com.liujiaming.core.feign.admin.entity.SimpleUser;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@ApiModel(value="Examine对象", description="审批表")
public class ExamineVO{

    @ApiModelProperty(value = "审批ID")
    private Long examineId;

    @ApiModelProperty(value = "0 OA 1 合同 2 回款 3发票 4薪资 5 采购审核 6采购退货审核 7销售审核 8 销售退货审核 9付款单审核10 回款单审核11盘点审核12调拨审核")
    private Integer label;

    @ApiModelProperty(value = "图标")
    private String examineIcon;

    @ApiModelProperty(value = "撤回之后重新审核操作 1 从第一层开始 2 从拒绝的层级开始")
    private Integer recheckType;

    @ApiModelProperty(value = "审批名称")
    private String examineName;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "创建人ID")
    private Long createUserId;

    @ApiModelProperty(value = "创建人名称")
    private String createUserName;

    @ApiModelProperty(value = "创建人名称")
    private String updateUserName;

    @ApiModelProperty(value = "修改时间")
    private Date updateTime;

    @ApiModelProperty(value = "1 正常 2 停用 3 删除 ")
    private Integer status;

    @ApiModelProperty(value = "备注")
    private String remarks;

    @ApiModelProperty(value = "审批管理员列表")
    private List<Long> managerList;

    @ApiModelProperty(value = "可见范围（员工）")
    private String userIds;

    @ApiModelProperty(value = "可见范围（部门）")
    private String deptIds;

    @ApiModelProperty(value = "1 普通审批 2 请假审批 3 出差审批 4 加班审批 5 差旅报销 6 借款申请 0 自定义审批")
    private Integer oaType;


    private List<SimpleUser> userList;

    private List<SimpleDept> deptList;
}
