package com.liujiaming.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liujiaming.core.servlet.BaseServiceImpl;
import com.liujiaming.crm.constant.CrmBackLogModelEnum;
import com.liujiaming.crm.constant.CrmTypeEnum;
import com.liujiaming.crm.entity.PO.CrmBackLogDeal;
import com.liujiaming.crm.mapper.CrmBackLogDealMapper;
import com.liujiaming.crm.service.ICrmBackLogDealService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author liujiaming
 * @since 2024-05-23
 */
@Service
public class CrmBackLogDealServiceImpl extends BaseServiceImpl<CrmBackLogDealMapper, CrmBackLogDeal> implements ICrmBackLogDealService {

    /**
     * 更新对应的待办事项提醒
     *
     * @param userId         用户ID
     * @param crmTypeEnum        类型
     * @param crmBackLogModelEnum 模块
     * @param typeId         类型ID
     */
    @Override
    public void deleteByType(Long userId, CrmTypeEnum crmTypeEnum, CrmBackLogModelEnum crmBackLogModelEnum, Integer typeId) {
        LambdaQueryWrapper<CrmBackLogDeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmBackLogDeal::getCreateUserId, userId);
        wrapper.eq(CrmBackLogDeal::getCrmType, crmTypeEnum.getType());
        wrapper.eq(CrmBackLogDeal::getModel, crmBackLogModelEnum.getModelID());
        wrapper.eq(CrmBackLogDeal::getTypeId, typeId);
        remove(wrapper);
    }

    /**
     * 更新对应的待办事项提醒
     *
     * @param userId         用户ID
     * @param crmTypeEnum        类型
     * @param typeId         类型ID
     * @param crmBackLogEnum 模块
     */
    @Override
    public void deleteByTypes(Long userId, CrmTypeEnum crmTypeEnum, Integer typeId, CrmBackLogModelEnum... crmBackLogEnum) {
        List<CrmBackLogModelEnum> backLogEnums = Arrays.asList(crmBackLogEnum);
        LambdaQueryWrapper<CrmBackLogDeal> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(CrmBackLogDeal::getCreateUserId, userId);
        }
        wrapper.eq(CrmBackLogDeal::getCrmType, crmTypeEnum.getType());
        List<Integer> types = backLogEnums.stream().map(CrmBackLogModelEnum::getModelID).collect(Collectors.toList());
        wrapper.in(CrmBackLogDeal::getModel, types);
        wrapper.eq(CrmBackLogDeal::getTypeId, typeId);
        remove(wrapper);
    }

    /**
     * 查询对应的主键ID
     *
     * @param model   model
     * @param crmType 类型
     * @param userId  当前用户ID
     * @return data
     */
    @Override
    public List<String> queryTypeId(Integer model, Integer crmType, Long userId) {
        List<CrmBackLogDeal> list = lambdaQuery().select(CrmBackLogDeal::getTypeId).eq(CrmBackLogDeal::getModel, model).eq(CrmBackLogDeal::getCrmType, crmType).eq(CrmBackLogDeal::getCreateUserId, userId).list();
        return list.stream().map(log -> log.getTypeId().toString()).collect(Collectors.toList());
    }
}
