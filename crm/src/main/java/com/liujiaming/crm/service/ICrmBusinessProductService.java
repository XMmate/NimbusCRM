package com.liujiaming.crm.service;

import com.liujiaming.core.servlet.BaseService;
import com.liujiaming.crm.entity.PO.CrmBusinessProduct;

import java.util.List;

/**
 * <p>
 * 商机产品关系表 服务类
 * </p>
 *
 * @author liujiaming
 * @since 2024-05-27
 */
public interface ICrmBusinessProductService extends BaseService<CrmBusinessProduct> {
    /**
     * 通过商机ID删除商机产品关联
     * @param ids ids
     */
    public void deleteByBusinessId(Integer... ids);

    /**
     * 保存商机产品关联
     * @param crmBusinessProductList data
     */
    public void save(List<CrmBusinessProduct> crmBusinessProductList);

    /**
     * 通过产品ID删除商机产品关联
     * @param ids ids
     */
    public void deleteByProductId(Integer... ids);

    /**
     * 查询商机下产品
     * @param businessId 商机ID
     * @return data
     */
    public List<CrmBusinessProduct> queryList(Integer businessId);
}
