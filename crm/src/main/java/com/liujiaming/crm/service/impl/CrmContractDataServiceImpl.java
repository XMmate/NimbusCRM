package com.liujiaming.crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liujiaming.core.common.Const;
import com.liujiaming.core.field.service.FieldService;
import com.liujiaming.core.servlet.BaseServiceImpl;
import com.liujiaming.crm.common.CrmModel;
import com.liujiaming.crm.entity.PO.CrmContractData;
import com.liujiaming.crm.entity.VO.CrmModelFiledVO;
import com.liujiaming.crm.mapper.CrmContractDataMapper;
import com.liujiaming.crm.service.ICrmContractDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 合同扩展字段数据表 服务实现类
 * </p>
 *
 * @author liujiaming
 * @since 2024-05-27
 */
@Service
public class CrmContractDataServiceImpl extends BaseServiceImpl<CrmContractDataMapper, CrmContractData> implements ICrmContractDataService {

    @Autowired
    private FieldService fieldService;

    /**
     * 设置用户数据
     *
     * @param crmModel crmModel
     */
    @Override
    public void setDataByBatchId(CrmModel crmModel) {
        List<CrmContractData> contractDataList = query().eq("batch_id", crmModel.getBatchId()).list();
        contractDataList.forEach(obj -> {
            crmModel.put(obj.getName(), obj.getValue());
        });
    }

    /**
     * 保存自定义字段数据
     *
     * @param array data
     */
    @Override
    public void saveData(List<CrmModelFiledVO> array, String batchId) {
        List<CrmContractData> contractDataList = new ArrayList<>();
        remove(new LambdaQueryWrapper<CrmContractData>().eq(CrmContractData::getBatchId, batchId));
        Date date = new Date();
        for (CrmModelFiledVO obj : array) {
            CrmContractData crmContractData = BeanUtil.copyProperties(obj, CrmContractData.class);
            crmContractData.setValue(fieldService.convertObjectValueToString(obj.getType(),obj.getValue(),crmContractData.getValue()));
            crmContractData.setName(obj.getFieldName());
            crmContractData.setCreateTime(date);
            crmContractData.setBatchId(batchId);
            contractDataList.add(crmContractData);
        }
        saveBatch(contractDataList, Const.BATCH_SAVE_SIZE);
    }

    /**
     * 通过batchId删除数据
     *
     * @param batchId data
     */
    @Override
    public void deleteByBatchId(List<String> batchId) {
        LambdaQueryWrapper<CrmContractData> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CrmContractData::getBatchId,batchId);
        remove(wrapper);
    }
}
