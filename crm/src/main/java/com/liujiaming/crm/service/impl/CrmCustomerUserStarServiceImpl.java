package com.liujiaming.crm.service.impl;

import com.liujiaming.core.servlet.BaseServiceImpl;
import com.liujiaming.crm.entity.PO.CrmCustomerUserStar;
import com.liujiaming.crm.mapper.CrmCustomerUserStarMapper;
import com.liujiaming.crm.service.ICrmCustomerUserStarService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户客户标星关系表  服务实现类
 * </p>
 *
 * @author liujiaming
 * @since 2024-05-29
 */
@Service
public class CrmCustomerUserStarServiceImpl extends BaseServiceImpl<CrmCustomerUserStarMapper, CrmCustomerUserStar> implements ICrmCustomerUserStarService {
    /**
     * 判断用户是否关注
     *
     * @param customerId id
     * @param userId     用户ID
     * @return int
     */
    @Override
    public Integer isStar(Object customerId, Long userId) {
        return lambdaQuery().eq(CrmCustomerUserStar::getUserId, userId).eq(CrmCustomerUserStar::getCustomerId, customerId).last(" limit 1").count();
    }

    @Override
    public List<Integer> starList(Long userId) {
        return lambdaQuery().select(CrmCustomerUserStar::getCustomerId).eq(CrmCustomerUserStar::getUserId,userId).list().stream().map(CrmCustomerUserStar::getCustomerId).collect(Collectors.toList());
    }
}
