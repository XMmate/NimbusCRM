package com.liujiaming.crm.entity.PO;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 联系人扩展字段数据表
 * </p>
 *
 * @author liujiaming
 * @since 2024-05-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wk_crm_contacts_data")
@ApiModel(value="CrmContactsData对象", description="联系人扩展字段数据表")
public class CrmContactsData implements Serializable {

    private static final long serialVersionUID=1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer fieldId;

    @ApiModelProperty(value = "字段名称")
    private String name;

    private String value;

    @TableField(fill = FieldFill.INSERT) //自动赋予系统当前时间
    private Date createTime;

    private String batchId;



}
