package com.liujiaming.crm.mapper;

import com.liujiaming.core.servlet.BaseMapper;
import com.liujiaming.crm.entity.PO.CrmFieldSort;
import com.liujiaming.crm.entity.VO.CrmFieldSortVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 字段排序表 Mapper 接口
 * </p>
 *
 * @author liujiaming
 * @since 2024-05-19
 */
public interface CrmFieldSortMapper extends BaseMapper<CrmFieldSort> {

    /**
     * 查询模块字段列表
     *
     * @param label label
     * @return data
     */
    public List<CrmFieldSortVO> queryListHead(@Param("label") Integer label,@Param("userId") Long userId);
}
