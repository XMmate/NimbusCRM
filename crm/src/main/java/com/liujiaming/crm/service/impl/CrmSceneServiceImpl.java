package com.liujiaming.crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.liujiaming.core.common.Const;
import com.liujiaming.core.common.FieldEnum;
import com.liujiaming.core.common.SystemCodeEnum;
import com.liujiaming.core.exception.CrmException;
import com.liujiaming.core.servlet.ApplicationContextHolder;
import com.liujiaming.core.servlet.BaseServiceImpl;
import com.liujiaming.core.utils.UserUtil;
import com.liujiaming.crm.constant.CrmTypeEnum;
import com.liujiaming.crm.constant.CrmSceneEnum;
import com.liujiaming.crm.entity.BO.CrmSceneConfigBO;
import com.liujiaming.crm.entity.PO.*;
import com.liujiaming.crm.entity.VO.CrmModelFiledVO;
import com.liujiaming.crm.mapper.CrmSceneMapper;
import com.liujiaming.crm.service.ICrmBusinessTypeService;
import com.liujiaming.crm.service.ICrmFieldService;
import com.liujiaming.crm.service.ICrmSceneDefaultService;
import com.liujiaming.crm.service.ICrmSceneService;
import com.liujiaming.crm.entity.PO.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 场景 服务实现类
 * </p>
 *
 * @author liujiaming
 * @since 2024-06-06
 */
@Service
public class CrmSceneServiceImpl extends BaseServiceImpl<CrmSceneMapper, CrmScene> implements ICrmSceneService {

    @Autowired
    private ICrmSceneDefaultService crmSceneDefaultService;

    @Autowired
    private ICrmFieldService crmFieldService;

    /**
     * 查询场景
     *
     * @param crmTypeEnum 类型
     * @return data
     */
    @Override
    public List<CrmScene> queryScene(CrmTypeEnum crmTypeEnum) {
        Long userId = UserUtil.getUserId();
        List<CrmScene> sceneList = new ArrayList<>();
        //查询userId下是否有系统场景，没有则插入
        Integer number = lambdaQuery().eq(CrmScene::getIsSystem, 1).eq(CrmScene::getType, crmTypeEnum.getType()).eq(CrmScene::getUserId, UserUtil.getUserId()).count();
        if (number.equals(0)) {
            if (CrmTypeEnum.LEADS == crmTypeEnum) {
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "全部线索", userId, 0, "", 0, 1, CrmSceneEnum.ALL.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "我负责的线索", userId, 0, "", 0, 1, CrmSceneEnum.SELF.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "下属负责的线索", userId, 0, "", 0, 1, CrmSceneEnum.CHILD.getName()));
                JSONArray array = new JSONArray();
                array.add(new JSONObject().fluentPut("name", "isTransform").fluentPut("type", 1).fluentPut("values", Collections.singletonList("1")));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "已转化的线索", userId, 0, array.toJSONString(), 0, 1, "transform"));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "我关注的线索", userId, 0, array.toJSONString(), 0, 1, CrmSceneEnum.STAR.getName()));
            } else if (CrmTypeEnum.CUSTOMER == crmTypeEnum) {
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "全部客户", userId, 0, "", 0, 1, CrmSceneEnum.ALL.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "我负责的客户", userId, 0, "", 0, 1, CrmSceneEnum.SELF.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "下属负责的客户", userId, 0, "", 0, 1, CrmSceneEnum.CHILD.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "我关注的客户", userId, 0, "", 0, 1, CrmSceneEnum.STAR.getName()));
            } else if (CrmTypeEnum.CONTACTS == crmTypeEnum) {
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "全部联系人", userId, 0, "", 0, 1, CrmSceneEnum.ALL.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "我负责的联系人", userId, 0, "", 0, 1, CrmSceneEnum.SELF.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "下属负责的联系人", userId, 0, "", 0, 1, CrmSceneEnum.CHILD.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "我关注的联系人", userId, 0, "", 0, 1, CrmSceneEnum.STAR.getName()));
            } else if (CrmTypeEnum.PRODUCT == crmTypeEnum) {
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "全部产品", userId, 0, "", 0, 1, CrmSceneEnum.ALL.getName()));
                JSONArray array = new JSONArray();
                array.add(new JSONObject().fluentPut("name", "status").fluentPut("type", 1).fluentPut("values", Collections.singletonList(1)));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "上架的产品", userId, 0, array.toJSONString(), 0, 1, ""));
                array.clear();
                array.add(new JSONObject().fluentPut("name", "status").fluentPut("type", 1).fluentPut("values", Collections.singletonList(0)));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "下架的产品", userId, 0, array.toJSONString(), 0, 1, ""));
            } else if (CrmTypeEnum.BUSINESS == crmTypeEnum) {
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "全部商机", userId, 0, "", 0, 1, CrmSceneEnum.ALL.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "我负责的商机", userId, 0, "", 0, 1, CrmSceneEnum.SELF.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "下属负责的商机", userId, 0, "", 0, 1, CrmSceneEnum.CHILD.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "我关注的商机", userId, 0, "", 0, 1, CrmSceneEnum.STAR.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "赢单商机", userId, 0, new JSONArray().fluentAdd(new JSONObject().fluentPut("name", "isEnd").fluentPut("type", 1).fluentPut("values", Collections.singletonList(1))).toJSONString(), 0, 1, ""));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "输单商机", userId, 0, new JSONArray().fluentAdd(new JSONObject().fluentPut("name", "isEnd").fluentPut("type", 1).fluentPut("values", Collections.singletonList(2))).toJSONString(), 0, 1, ""));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "无效商机", userId, 0, new JSONArray().fluentAdd(new JSONObject().fluentPut("name", "isEnd").fluentPut("type", 1).fluentPut("values", Collections.singletonList(3))).toJSONString(), 0, 1, ""));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "进行中的商机", userId, 0, new JSONArray().fluentAdd(new JSONObject().fluentPut("name", "isEnd").fluentPut("type", 1).fluentPut("values", Collections.singletonList(0))).toJSONString(), 0, 1, ""));
            } else if (CrmTypeEnum.CONTRACT == crmTypeEnum) {
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "全部合同", userId, 0, "", 0, 1, CrmSceneEnum.ALL.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "我负责的合同", userId, 0, "", 0, 1, CrmSceneEnum.SELF.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "下属负责的合同", userId, 0, "", 0, 1, CrmSceneEnum.CHILD.getName()));
            } else if (CrmTypeEnum.RECEIVABLES == crmTypeEnum) {
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "全部回款", userId, 0, "", 0, 1, CrmSceneEnum.ALL.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "我负责的回款", userId, 0, "", 0, 1, CrmSceneEnum.SELF.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "下属负责的回款", userId, 0, "", 0, 1, CrmSceneEnum.CHILD.getName()));
            } else if(CrmTypeEnum.RECEIVABLES_PLAN == crmTypeEnum){
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "全部回款计划", userId, 0, "", 0, 1, CrmSceneEnum.ALL.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "我负责的回款计划", userId, 1, "", 0, 1, CrmSceneEnum.SELF.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "下属负责的回款计划", userId, 2, "", 0, 1, CrmSceneEnum.CHILD.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "全部逾期未回款", userId, 0, new JSONArray().fluentAdd(new JSONObject().fluentPut("name", "receivedStatus").fluentPut("type", 1).fluentPut("values", Collections.singletonList(4))).toJSONString(), 0, 1, ""));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "本月到期未回款", userId, 0, new JSONArray().fluentAdd(new JSONObject().fluentPut("name", "receivedStatus").fluentPut("type", 1).fluentPut("values", Collections.singletonList(4))).fluentAdd(new JSONObject().fluentPut("name", "returnDate").fluentPut("type", 14).fluentPut("values", Collections.singletonList("month"))).toJSONString(), 0, 1, ""));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "本周到期未回款", userId, 0, new JSONArray().fluentAdd(new JSONObject().fluentPut("name", "receivedStatus").fluentPut("type", 1).fluentPut("values", Collections.singletonList(4))).fluentAdd(new JSONObject().fluentPut("name", "returnDate").fluentPut("type", 14).fluentPut("values", Collections.singletonList("week"))).toJSONString(), 0, 1, ""));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "已回款", userId, 0, new JSONArray().fluentAdd(new JSONObject().fluentPut("name", "receivedStatus").fluentPut("type", 1).fluentPut("values", Collections.singletonList(1))).toJSONString(), 0, 1, ""));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "本月已回款", userId, 0, new JSONArray().fluentAdd(new JSONObject().fluentPut("name", "receivedStatus").fluentPut("type", 1).fluentPut("values", Collections.singletonList(1))).fluentAdd(new JSONObject().fluentPut("name", "returnDate").fluentPut("type", 14).fluentPut("values", Collections.singletonList("month"))).toJSONString(), 0, 1, ""));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "本周已回款", userId, 0, new JSONArray().fluentAdd(new JSONObject().fluentPut("name", "receivedStatus").fluentPut("type", 1).fluentPut("values", Collections.singletonList(1))).fluentAdd(new JSONObject().fluentPut("name", "returnDate").fluentPut("type", 14).fluentPut("values", Collections.singletonList("week"))).toJSONString(), 0, 1, ""));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "部分回款", userId, 0, new JSONArray().fluentAdd(new JSONObject().fluentPut("name", "receivedStatus").fluentPut("type", 1).fluentPut("values", Collections.singletonList(2))).toJSONString(), 0, 1, ""));
            } else if (CrmTypeEnum.RETURN_VISIT == crmTypeEnum) {
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "全部回访", userId, 0, "", 0, 1, CrmSceneEnum.ALL.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "我负责的回访", userId, 0, "", 0, 1, CrmSceneEnum.SELF.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "下属负责的回访", userId, 0, "", 0, 1, CrmSceneEnum.CHILD.getName()));
            } else if (CrmTypeEnum.INVOICE == crmTypeEnum) {
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "全部发票", userId, 0, "", 0, 1, CrmSceneEnum.ALL.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "我负责的发票", userId, 0, "", 0, 1, CrmSceneEnum.SELF.getName()));
                sceneList.add(new CrmScene(crmTypeEnum.getType(), "下属负责的发票", userId, 0, "", 0, 1, CrmSceneEnum.CHILD.getName()));
            }
            saveBatch(sceneList, Const.BATCH_SAVE_SIZE);
        } else {
            sceneList.addAll(lambdaQuery().eq(CrmScene::getType, crmTypeEnum.getType()).eq(CrmScene::getUserId, UserUtil.getUserId()).eq(CrmScene::getIsHide,0).list());
        }
        List<CrmSceneDefault> defaults = crmSceneDefaultService.lambdaQuery().eq(CrmSceneDefault::getType, crmTypeEnum.getType()).eq(CrmSceneDefault::getUserId, userId).list();
        for (CrmSceneDefault sceneDefault : defaults) {
            Integer sceneId = sceneDefault.getSceneId();
            for (CrmScene crmScene : sceneList) {
                if (Objects.equals(sceneId, crmScene.getSceneId())) {
                    crmScene.setIsDefault(1);
                }
            }
        }
        return sceneList;
    }
    @Override
    public List<CrmModelFiledVO> queryField(Integer label) {
        LambdaQueryWrapper<CrmField> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmField::getLabel, label);
        wrapper.eq(CrmField::getFieldType, 1);
        wrapper.eq(CrmField::getIsHidden, 0);
        wrapper.select(CrmField::getOptions, CrmField::getType, CrmField::getName, CrmField::getFieldName,CrmField::getPrecisions);
        List<CrmModelFiledVO> fieldList = crmFieldService.list(wrapper).stream()
                .map(field -> {
                    CrmModelFiledVO filedVO = BeanUtil.copyProperties(field, CrmModelFiledVO.class);
                    if (ListUtil.toList(14,15,16,17,20).contains(filedVO.getType())){
                        filedVO.setType(1);
                    }
                    crmFieldService.recordToFormType(filedVO, FieldEnum.parse(filedVO.getType()));
                    return filedVO;
                }).collect(Collectors.toList());
        CrmTypeEnum crmTypeEnum = CrmTypeEnum.parse(label);
        if (crmTypeEnum == CrmTypeEnum.NULL) {
            throw new CrmException(SystemCodeEnum.SYSTEM_NO_VALID);
        }
        if(CrmTypeEnum.RETURN_VISIT != crmTypeEnum){
            fieldList.add(new CrmModelFiledVO("owner_user_id", FieldEnum.USER, "负责人", 1));
            fieldList.add(new CrmModelFiledVO("owner_dept_id", FieldEnum.STRUCTURE, "所属部门", 1));
        }
        fieldList.add(new CrmModelFiledVO("create_user_id", FieldEnum.USER, "创建人", 1));
        fieldList.add(new CrmModelFiledVO("update_time", FieldEnum.DATETIME, "更新时间", 1));
        fieldList.add(new CrmModelFiledVO("create_time", FieldEnum.DATETIME, "创建时间", 1));
        if (CrmTypeEnum.LEADS == crmTypeEnum) {
            fieldList.add(new CrmModelFiledVO("last_time", FieldEnum.DATETIME, "最后跟进时间", 1));
        } else if (CrmTypeEnum.CUSTOMER == crmTypeEnum) {
            List<Object> dealStatusList = new ArrayList<>();
            dealStatusList.add(new JSONObject().fluentPut("name", "未成交").fluentPut("value", 0));
            dealStatusList.add(new JSONObject().fluentPut("name", "已成交").fluentPut("value", 1));
            fieldList.add(new CrmModelFiledVO("deal_status", FieldEnum.BUSINESS, "成交状态", 1).setSetting(dealStatusList).setFormType("dealStatus").setType(null));
            fieldList.add(new CrmModelFiledVO("last_time", FieldEnum.DATETIME, "最后跟进时间", 1));
            fieldList.add(new CrmModelFiledVO("detail_address", FieldEnum.TEXT, "详细地址", 1));
            fieldList.add(new CrmModelFiledVO("address", FieldEnum.MAP_ADDRESS, "地区定位", 1));
            fieldList.add(new CrmModelFiledVO("teamMemberIds", FieldEnum.USER, "相关团队",1));
        } else if (CrmTypeEnum.CONTACTS == crmTypeEnum) {
            fieldList.add(new CrmModelFiledVO("last_time", FieldEnum.DATETIME, "最后跟进时间", 1));
            fieldList.add(new CrmModelFiledVO("teamMemberIds", FieldEnum.USER, "相关团队",1));
        } else if (CrmTypeEnum.PRODUCT == crmTypeEnum) {
            List<Object> statusList = new ArrayList<>();
            statusList.add(new JSONObject().fluentPut("name", "上架").fluentPut("value", 1));
            statusList.add(new JSONObject().fluentPut("name", "下架").fluentPut("value", 0));
            fieldList.add(new CrmModelFiledVO("status", FieldEnum.SELECT, "是否上下架", 1).setSetting(statusList));
        } else if (CrmTypeEnum.BUSINESS == crmTypeEnum) {
            List<CrmBusinessType> crmBusinessTypes = ApplicationContextHolder.getBean(ICrmBusinessTypeService.class).queryBusinessStatusOptions();
            crmBusinessTypes.forEach(record -> {
                record.getStatusList().add(new CrmBusinessStatus().setName("赢单").setTypeId(record.getTypeId()).setStatusId(-1));
                record.getStatusList().add(new CrmBusinessStatus().setName("输单").setTypeId(record.getTypeId()).setStatusId(-2));
                record.getStatusList().add(new CrmBusinessStatus().setName("无效").setTypeId(record.getTypeId()).setStatusId(-3));
            });
            fieldList.add(new CrmModelFiledVO("type_id", FieldEnum.BUSINESS, "商机状态组", 1).setFormType("business_type").setSetting(new ArrayList<>(crmBusinessTypes)));
            fieldList.add(new CrmModelFiledVO("last_time", FieldEnum.DATETIME, "最后跟进时间", 1));
            fieldList.add(new CrmModelFiledVO("teamMemberIds", FieldEnum.USER, "相关团队",1));
        } else if (CrmTypeEnum.CONTRACT == crmTypeEnum) {
            List<Object> checkList = new ArrayList<>();
            checkList.add(new JSONObject().fluentPut("name", "待审核").fluentPut("value", 0));
            checkList.add(new JSONObject().fluentPut("name", "通过").fluentPut("value", 1));
            checkList.add(new JSONObject().fluentPut("name", "拒绝").fluentPut("value", 2));
            checkList.add(new JSONObject().fluentPut("name", "审核中").fluentPut("value", 3));
            checkList.add(new JSONObject().fluentPut("name", "已撤回").fluentPut("value", 4));
            checkList.add(new JSONObject().fluentPut("name", "未提交").fluentPut("value", 5));
            checkList.add(new JSONObject().fluentPut("name", "已作废").fluentPut("value", 8));
            fieldList.add(new CrmModelFiledVO("check_status", FieldEnum.CHECKBOX, "审核状态", 1).setFormType("checkStatus").setType(null).setSetting(checkList));
            fieldList.add(new CrmModelFiledVO("last_time", FieldEnum.DATETIME, "最后跟进时间", 1));
            fieldList.add(new CrmModelFiledVO("teamMemberIds", FieldEnum.USER, "相关团队",1));
        } else if (CrmTypeEnum.RECEIVABLES == crmTypeEnum) {
            List<Object> checkList = new ArrayList<>();
            checkList.add(new JSONObject().fluentPut("name", "待审核").fluentPut("value", 0));
            checkList.add(new JSONObject().fluentPut("name", "通过").fluentPut("value", 1));
            checkList.add(new JSONObject().fluentPut("name", "拒绝").fluentPut("value", 2));
            checkList.add(new JSONObject().fluentPut("name", "审核中").fluentPut("value", 3));
            checkList.add(new JSONObject().fluentPut("name", "未提交").fluentPut("value", 5));
            fieldList.add(new CrmModelFiledVO("check_status", FieldEnum.CHECKBOX, "审核状态", 1).setFormType("checkStatus").setType(null).setSetting(checkList));
            fieldList.add(new CrmModelFiledVO("teamMemberIds", FieldEnum.USER, "相关团队",1));
        } else if (CrmTypeEnum.RECEIVABLES_PLAN == crmTypeEnum) {
            fieldList.add(new CrmModelFiledVO("realReceivedMoney", FieldEnum.FLOATNUMBER, "实际回款金额",1));
            fieldList.add(new CrmModelFiledVO("realReturnDate", FieldEnum.DATE, "实际回款日期",1));
            List<Object> checkList = new ArrayList<>();
            checkList.add(new JSONObject().fluentPut("name", "待回款").fluentPut("value", 0));
            checkList.add(new JSONObject().fluentPut("name", "回款完成").fluentPut("value", 1));
            checkList.add(new JSONObject().fluentPut("name", "部分回款").fluentPut("value", 2));
            checkList.add(new JSONObject().fluentPut("name", "作废").fluentPut("value", 3));
            checkList.add(new JSONObject().fluentPut("name", "逾期").fluentPut("value", 4));
            checkList.add(new JSONObject().fluentPut("name", "待生效").fluentPut("value", 5));
            fieldList.add(new CrmModelFiledVO("receivedStatus", FieldEnum.CHECKBOX, "回款状态",1).setSetting(checkList));
        } else if (CrmTypeEnum.INVOICE == crmTypeEnum){
            for (CrmModelFiledVO crmModelFiledVO : fieldList) {
                if("invoiceType".equals(crmModelFiledVO.getFieldName())){
                    List<Object> checkList = new ArrayList<>();
                    checkList.add(new JSONObject().fluentPut("name", "增值税专用发票").fluentPut("value", 1));
                    checkList.add(new JSONObject().fluentPut("name", "增值税普通发票").fluentPut("value", 2));
                    checkList.add(new JSONObject().fluentPut("name", "国税通用机打发票").fluentPut("value", 3));
                    checkList.add(new JSONObject().fluentPut("name", "地税通用机打发票").fluentPut("value", 4));
                    checkList.add(new JSONObject().fluentPut("name", "收据").fluentPut("value", 5));
                    crmModelFiledVO.setSetting(checkList);
                    break;
                }
            }
            List<Object> settingList = new ArrayList<>(2);
            settingList.add(new JSONObject().fluentPut("name", "未开票").fluentPut("value", 0));
            settingList.add(new JSONObject().fluentPut("name", "已开票").fluentPut("value", 1));
            fieldList.add(new CrmModelFiledVO("invoiceStatus", FieldEnum.SELECT, "开票状态",1).setSetting(settingList));
            fieldList.add(new CrmModelFiledVO("invoiceNumber", FieldEnum.TEXT, "发票号码",1));
            fieldList.add(new CrmModelFiledVO("realInvoiceDate", FieldEnum.DATE, "实际开票日期",1));
            fieldList.add(new CrmModelFiledVO("logisticsNumber", FieldEnum.TEXT, "物流单号",1));
        }
        LambdaQueryWrapper<CrmField> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CrmField::getLabel, label);
        queryWrapper.ne(CrmField::getFieldType, 1);
        queryWrapper.eq(CrmField::getIsHidden, 0);
        queryWrapper.select(CrmField::getFieldName, CrmField::getName, CrmField::getType, CrmField::getOptions,CrmField::getRemark,CrmField::getPrecisions);
        List<CrmField> crmFields = crmFieldService.list(queryWrapper);
        List<CrmModelFiledVO> records = crmFields.stream()
                .map(field -> {
                    CrmModelFiledVO filedVO = BeanUtil.copyProperties(field, CrmModelFiledVO.class);
                    crmFieldService.recordToFormType(filedVO, FieldEnum.parse(filedVO.getType()));
                    return filedVO;
                }).collect(Collectors.toList());
        List<FieldEnum> fieldEnums = Arrays.asList(FieldEnum.FILE,FieldEnum.DATE_INTERVAL,
                FieldEnum.HANDWRITING_SIGN,FieldEnum.DESC_TEXT,FieldEnum.DETAIL_TABLE,FieldEnum.CALCULATION_FUNCTION);
        records.removeIf(record -> fieldEnums.contains(FieldEnum.parse(record.getType())));
        fieldList.addAll(records);
        return fieldList;
    }

    /**
     * 新增场景
     *
     * @param crmScene data
     */
    @Override
    public void addScene(CrmScene crmScene) {
        Long userId = UserUtil.getUserId();
        try {
            JSON.parse(crmScene.getData());
        } catch (Exception e) {
            return;
        }
        crmScene.setIsHide(0).setSort(99999).setIsSystem(0).setCreateTime(DateUtil.date()).setUserId(userId);
        save(crmScene);
        if (Objects.equals(1, crmScene.getIsDefault())) {
            crmSceneDefaultService.lambdaUpdate().eq(CrmSceneDefault::getType,crmScene.getType()).eq(CrmSceneDefault::getUserId,userId).remove();
            CrmSceneDefault adminSceneDefault = new CrmSceneDefault();
            adminSceneDefault.setSceneId(crmScene.getSceneId()).setType(crmScene.getType()).setUserId(userId);
            crmSceneDefaultService.save(adminSceneDefault);
        }
    }

    /**
     * 修改场景
     *
     * @param crmScene data
     */
    @Override
    public void updateScene(CrmScene crmScene) {
        Long userId = UserUtil.getUserId();
        CrmScene oldAdminScene = getById(crmScene.getSceneId());
        if (oldAdminScene == null) {
            return;
        }
        try {
            JSON.parse(crmScene.getData());
        } catch (Exception e) {
            return;
        }
        if (Objects.equals(1, crmScene.getIsDefault())) {
            LambdaUpdateWrapper<CrmSceneDefault> wrapper = new LambdaUpdateWrapper<>();
            wrapper.set(CrmSceneDefault::getSceneId, crmScene.getSceneId());
            wrapper.eq(CrmSceneDefault::getUserId, userId);
            wrapper.eq(CrmSceneDefault::getType, oldAdminScene.getType());
            crmSceneDefaultService.update(wrapper);
        }else {
            LambdaQueryWrapper<CrmSceneDefault> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CrmSceneDefault::getSceneId, crmScene.getSceneId())
                    .eq(CrmSceneDefault::getUserId, userId)
                    .eq(CrmSceneDefault::getType, oldAdminScene.getType());
            crmSceneDefaultService.remove(wrapper);
        }
        crmScene.setUserId(userId).setType(oldAdminScene.getType()).setSort(oldAdminScene.getSort()).setIsSystem(oldAdminScene.getIsSystem()).setUpdateTime(DateUtil.date());
        updateById(crmScene);
    }

    /**
     * 保存默认场景
     *
     * @param sceneId
     */
    @Override
    public void setDefaultScene(Integer sceneId) {
        Long userId = UserUtil.getUserId();
        CrmScene oldAdminScene = getById(sceneId);
        if (oldAdminScene != null) {
            crmSceneDefaultService.removeByMap(new JSONObject().fluentPut("user_id", userId).fluentPut("type", oldAdminScene.getType()));
            CrmSceneDefault adminSceneDefault = new CrmSceneDefault();
            adminSceneDefault.setSceneId(sceneId).setType(oldAdminScene.getType()).setUserId(userId);
            crmSceneDefaultService.save(adminSceneDefault);
        }
    }

    /**
     * 删除场景
     *
     * @param sceneId sceneId
     */
    @Override
    public void deleteScene(Integer sceneId) {
        CrmScene crmScene = getById(sceneId);
        if (crmScene != null && !Objects.equals(1, crmScene.getIsSystem())) {
            removeById(sceneId);
        }
    }

    /**
     * 查询场景设置
     *
     * @param type type
     * @return data
     */
    @Override
    public JSONObject querySceneConfig(Integer type) {
        Long userId = UserUtil.getUserId();
        List<CrmScene> crmSceneList = lambdaQuery().eq(CrmScene::getUserId, userId).eq(CrmScene::getType, type).list();
        Map<Integer, List<CrmScene>> collect = crmSceneList.stream().collect(Collectors.groupingBy(CrmScene::getIsHide));
        List<CrmSceneDefault> defaults = crmSceneDefaultService.lambdaQuery().eq(CrmSceneDefault::getType, type).eq(CrmSceneDefault::getUserId, userId).list();

        if (!collect.containsKey(1)) {
            collect.put(1, new ArrayList<>());
        }
        if (!collect.containsKey(0)) {
            collect.put(0, new ArrayList<>());
        }
        for (CrmSceneDefault sceneDefault : defaults) {
            Integer sceneId = sceneDefault.getSceneId();
            for (CrmScene crmScene : collect.get(0)) {
                if (Objects.equals(sceneId, crmScene.getSceneId())) {
                    crmScene.setIsDefault(1);
                }
            }
        }
        return new JSONObject().fluentPut("value", collect.get(0)).fluentPut("hide_value", collect.get(1));
    }

    /**
     * 设置场景
     */
    @Override
    public void sceneConfig(CrmSceneConfigBO config) {
        Long userId = UserUtil.getUserId();
        List<CrmScene> crmSceneList = lambdaQuery().eq(CrmScene::getUserId, userId).eq(CrmScene::getType, config.getType()).list();
        Map<Integer, CrmScene> crmSceneMap = new HashMap<>(crmSceneList.size());
        for (CrmScene crmScene : crmSceneList) {
            crmSceneMap.put(crmScene.getSceneId(), crmScene);
        }
        for (int i = 0; i < config.getNoHideIds().size(); i++) {
            Integer id = config.getNoHideIds().get(i);
            if (crmSceneMap.containsKey(id)) {
                crmSceneMap.get(id).setSort(i).setIsHide(0);
            }
        }
        for (int i = 0; i < config.getHideIds().size(); i++) {
            Integer id = config.getHideIds().get(i);
            if (crmSceneMap.containsKey(id)) {
                CrmScene scene = crmSceneMap.get(id);
                scene.setSort(0);
                if (scene.getIsSystem() == 1) {
                    scene.setIsHide(0);
                } else {
                    scene.setIsHide(1);
                }

            }
        }
        updateBatchById(crmSceneMap.values());
    }


}
