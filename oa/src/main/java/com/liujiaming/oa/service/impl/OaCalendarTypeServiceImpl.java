package com.liujiaming.oa.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.liujiaming.core.entity.BasePage;
import com.liujiaming.core.feign.admin.entity.AdminConfig;
import com.liujiaming.core.feign.admin.service.AdminService;
import com.liujiaming.core.feign.crm.entity.CrmEventBO;
import com.liujiaming.core.feign.crm.entity.QueryEventCrmPageBO;
import com.liujiaming.core.feign.crm.service.CrmEventService;
import com.liujiaming.core.servlet.BaseServiceImpl;
import com.liujiaming.core.utils.UserUtil;
import com.liujiaming.oa.entity.BO.QueryEventCrmBO;
import com.liujiaming.oa.entity.BO.QueryEventTaskBO;
import com.liujiaming.oa.entity.BO.UpdateTypeUserBO;
import com.liujiaming.oa.entity.PO.OaCalendarType;
import com.liujiaming.oa.entity.PO.OaCalendarTypeUser;
import com.liujiaming.oa.entity.PO.OaEvent;
import com.liujiaming.oa.entity.PO.OaEventUpdateRecord;
import com.liujiaming.oa.entity.VO.EventTaskVO;
import com.liujiaming.oa.mapper.OaCalendarTypeMapper;
import com.liujiaming.oa.service.IOaCalendarTypeService;
import com.liujiaming.oa.service.IOaCalendarTypeUserService;
import com.liujiaming.oa.service.IOaEventService;
import com.liujiaming.oa.service.IOaEventUpdateRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 日程类型 服务实现类
 * </p>
 *
 * @author liujiaming
 * @since 2024-05-15
 */
@Service
public class OaCalendarTypeServiceImpl extends BaseServiceImpl<OaCalendarTypeMapper, OaCalendarType> implements IOaCalendarTypeService {

    @Autowired
    private IOaCalendarTypeUserService calendarTypeUserService;

    @Autowired
    private IOaEventService eventService;

    @Autowired
    private IOaEventUpdateRecordService eventUpdateRecordService;

    @Autowired
    private OaCalendarTypeMapper calendarTypeMapper;

    @Autowired
    private AdminService adminService;

    @Autowired
    private CrmEventService crmEventService;


    /**
     * 添加或修改日程类型
     * @param oaCalendarType
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addOrUpdateType(OaCalendarType oaCalendarType) {
        if (oaCalendarType.getTypeId() == null) {
            oaCalendarType.setType(2);
            save(oaCalendarType);
            calendarTypeUserService.saveSysCalendarType(oaCalendarType.getTypeId());
        } else {
            updateById(oaCalendarType);
        }
    }

    /**
     * 根据id删除日程类型
     * @param typeId
     */

    @Override
    public void deleteType(Integer typeId) {
        List<Integer> eventIds = eventService.lambdaQuery().select(OaEvent::getEventId)
                .eq(OaEvent::getTypeId, typeId).list().stream().map(OaEvent::getEventId).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(eventIds)){
            eventUpdateRecordService.lambdaUpdate().in(OaEventUpdateRecord::getEventId,eventIds).remove();
        }
        eventService.lambdaUpdate().eq(OaEvent::getTypeId,typeId).remove();
        calendarTypeUserService.lambdaUpdate().eq(OaCalendarTypeUser::getTypeId,typeId).remove();
        removeById(typeId);
    }

    /**
     * 查询日程类型列表
     * @return
     */
    @Override
    public List<OaCalendarType> queryTypeList() {
        return lambdaQuery().select(OaCalendarType::getTypeId,OaCalendarType::getTypeName,OaCalendarType::getColor)
                .eq(OaCalendarType::getType,2).list();
    }

    @Override
    public List<OaCalendarType> queryTypeListByUser(Long userId) {
        if (userId == null) {
            userId = UserUtil.getUserId();
        }
        List<OaCalendarType> oaCalendarTypeList = list();
        List<Integer> typeIds = calendarTypeUserService.lambdaQuery().select(OaCalendarTypeUser::getTypeId)
                .eq(OaCalendarTypeUser::getUserId,userId).list()
                .stream().map(OaCalendarTypeUser::getTypeId).collect(Collectors.toList());
        if (CollUtil.isEmpty(typeIds)){
            List<OaCalendarTypeUser> typeUserList = new ArrayList<>();
            Long finalUserId = userId;
            oaCalendarTypeList.forEach(type->{
                Integer typeId = type.getTypeId();
                OaCalendarTypeUser typeUser = new OaCalendarTypeUser();
                typeUser.setUserId(finalUserId);
                typeUser.setTypeId(typeId);
                typeUserList.add(typeUser);
            });
            calendarTypeUserService.saveBatch(typeUserList,20);
            oaCalendarTypeList = oaCalendarTypeList.stream().peek(type -> type.setSelect(true)).collect(Collectors.toList());
        }else {
            oaCalendarTypeList = oaCalendarTypeList.stream().peek(type -> type.setSelect(typeIds.contains(type.getTypeId()))).collect(Collectors.toList());
        }
        return oaCalendarTypeList.stream().sorted(Comparator.comparingInt(OaCalendarType::getSort)).collect(Collectors.toList());
    }

    @Override
    public void updateTypeUser(UpdateTypeUserBO updateTypeUserBO) {
        List<Integer> typeIds = updateTypeUserBO.getTypeIds();
        Long userId = updateTypeUserBO.getUserId();
        if (userId == null) {
            userId = UserUtil.getUserId();
        }
        calendarTypeUserService.lambdaUpdate().eq(OaCalendarTypeUser::getUserId,userId).remove();
        List<OaCalendarTypeUser> oaCalendarTypeUserList = new ArrayList<>();
        for (Integer typeId : typeIds) {
            OaCalendarTypeUser oaCalendarTypeUser = new OaCalendarTypeUser();
            oaCalendarTypeUser.setUserId(userId);
            oaCalendarTypeUser.setTypeId(typeId);
            oaCalendarTypeUserList.add(oaCalendarTypeUser);
        }
        calendarTypeUserService.saveBatch(oaCalendarTypeUserList,20);
    }

    @Override
    public List<EventTaskVO> eventTask(QueryEventTaskBO eventTaskBO) {
        if (eventTaskBO.getUserId() == null) {
            eventTaskBO.setUserId(UserUtil.getUserId());
        }
        return calendarTypeMapper.queryEventTask(eventTaskBO);
    }



    @Override
    public JSONObject eventCrm(QueryEventCrmBO queryEventCrmBO) {
        Long userId = queryEventCrmBO.getUserId();
        long startTime = queryEventCrmBO.getStartTime();
        long endTime = queryEventCrmBO.getEndTime();
        if (userId == null) {
            userId = UserUtil.getUserId();
        }
        JSONObject result = new JSONObject();
        AdminConfig adminConfig = adminService.queryFirstConfigByName("expiringContractDays").getData();
        CrmEventBO crmEventBO = new CrmEventBO();
        crmEventBO.setUserId(userId);
        crmEventBO.setStartTime(new Date(startTime));
        crmEventBO.setEndTime( new Date(endTime));
        if (adminConfig.getStatus() == 0 || ObjectUtil.isNull(adminConfig)) {
            result.put("endContractTimeList", new ArrayList<>());
        } else {
            crmEventBO.setExpiringDay(Integer.valueOf(adminConfig.getValue()));
            List<String> endContractTimeList = crmEventService.endContract(crmEventBO).getData();
            result.put("endContractTimeList", endContractTimeList);
        }
        List<String> customerTimeList = crmEventService.eventCustomer(crmEventBO).getData();
        List<String> receiveContractTimeList = crmEventService.receiveContract(crmEventBO).getData();
        List<String> leadsTimeList = crmEventService.eventLeads(crmEventBO).getData();
        List<String> businessTimeList = crmEventService.eventBusiness(crmEventBO).getData();
        List<String> dealBusinessTimeList = crmEventService.eventDealBusiness(crmEventBO).getData();
        result.fluentPut("customerTimeList", customerTimeList).fluentPut("receiveContractTimeList", receiveContractTimeList)
                .fluentPut("leadsTimeList",leadsTimeList).fluentPut("businessTimeList",businessTimeList).fluentPut("dealBusinessTimeList",dealBusinessTimeList);
        return result;
    }

    @Override
    public List<String> queryFixedTypeByUserId(Long userId) {
        return calendarTypeMapper.queryFixedTypeByUserId(userId);
    }

    @Override
    public BasePage<Map<String,Object>> eventCustomer(QueryEventCrmPageBO eventCrmPageBO) {
        return crmEventService.eventCustomerPageList(eventCrmPageBO).getData();
    }

    @Override
    public BasePage<Map<String, Object>> eventContract(QueryEventCrmPageBO eventCrmPageBO) {
        return crmEventService.eventContractPageList(eventCrmPageBO).getData();
    }

    @Override
    public BasePage<Map<String, Object>> eventLeads(QueryEventCrmPageBO eventCrmPageBO) {
        return crmEventService.eventLeadsPageList(eventCrmPageBO).getData();
    }

    @Override
    public BasePage<Map<String, Object>> eventBusiness(QueryEventCrmPageBO eventCrmPageBO) {
        return crmEventService.eventBusinessPageList(eventCrmPageBO).getData();
    }

    @Override
    public BasePage<Map<String, Object>> eventDealBusiness(QueryEventCrmPageBO eventCrmPageBO) {
        return crmEventService.eventDealBusinessPageList(eventCrmPageBO).getData();
    }
}
