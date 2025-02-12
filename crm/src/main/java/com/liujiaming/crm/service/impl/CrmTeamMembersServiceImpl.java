package com.liujiaming.crm.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.util.TypeUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liujiaming.core.common.Const;
import com.liujiaming.core.common.SystemCodeEnum;
import com.liujiaming.core.entity.UserInfo;
import com.liujiaming.core.exception.CrmException;
import com.liujiaming.core.feign.admin.entity.AdminMessageBO;
import com.liujiaming.core.feign.admin.entity.AdminMessageEnum;
import com.liujiaming.core.feign.admin.service.AdminMessageService;
import com.liujiaming.core.servlet.ApplicationContextHolder;
import com.liujiaming.core.servlet.BaseServiceImpl;
import com.liujiaming.core.utils.UserCacheUtil;
import com.liujiaming.core.utils.UserUtil;
import com.liujiaming.crm.common.ActionRecordUtil;
import com.liujiaming.crm.common.AuthUtil;
import com.liujiaming.crm.constant.CrmAuthEnum;
import com.liujiaming.crm.constant.CrmTypeEnum;
import com.liujiaming.crm.entity.BO.CrmMemberSaveBO;
import com.liujiaming.crm.entity.PO.*;
import com.liujiaming.crm.entity.VO.CrmMembersSelectVO;
import com.liujiaming.crm.mapper.CrmTeamMembersMapper;
import com.liujiaming.crm.service.*;
import com.liujiaming.crm.entity.PO.*;
import com.liujiaming.crm.service.*;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * crm团队成员表
 * 服务实现类
 * </p>
 *
 * @author liujiaming
 * @since 2021-04-07
 */
@Service
@Slf4j
public class CrmTeamMembersServiceImpl extends BaseServiceImpl<CrmTeamMembersMapper, CrmTeamMembers> implements ICrmTeamMembersService {

    @Autowired
    private ActionRecordUtil actionRecordUtil;

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    /**
     * 获取团队成员
     *
     * @param crmTypeEnum     对应类型
     * @param typeId      对应类型ID
     * @param ownerUserId 负责人ID
     * @return data
     */
    @Override
    public List<CrmMembersSelectVO> getMembers(CrmTypeEnum crmTypeEnum, Integer typeId, Long ownerUserId) {
        List<CrmMembersSelectVO> selectVOS = new ArrayList<>();
        if (ownerUserId != null) {
            List<Long> authUserList = AuthUtil.queryAuthUserList(crmTypeEnum, CrmAuthEnum.READ);
            Integer num = lambdaQuery().eq(CrmTeamMembers::getType,crmTypeEnum.getType()).eq(CrmTeamMembers::getTypeId,typeId).eq(CrmTeamMembers::getUserId,UserUtil.getUserId()).count();
            if (!authUserList.contains(ownerUserId) && num == 0) {
                throw new CrmException(SystemCodeEnum.SYSTEM_NO_AUTH);
            }
            UserInfo userInfo = UserCacheUtil.getUserInfo(ownerUserId);
            CrmMembersSelectVO selectVO = new CrmMembersSelectVO();
            selectVO.setUserId(userInfo.getUserId());
            selectVO.setPower(3);
            selectVO.setDeptName(userInfo.getDeptName());
            selectVO.setRealname(userInfo.getRealname());
            selectVO.setExpiresTime(null);
            selectVOS.add(selectVO);
        }
        List<CrmTeamMembers> teamMembers = lambdaQuery().eq(CrmTeamMembers::getType, crmTypeEnum.getType()).eq(CrmTeamMembers::getTypeId, typeId).list();
        for (CrmTeamMembers teamMember : teamMembers) {
            if (Objects.equals(teamMember.getUserId(), ownerUserId)) {
                continue;
            }
            CrmMembersSelectVO selectVO = new CrmMembersSelectVO();
            UserInfo userInfo = UserCacheUtil.getUserInfo(teamMember.getUserId());
            selectVO.setUserId(teamMember.getUserId());
            selectVO.setPower(teamMember.getPower());
            selectVO.setDeptName(userInfo.getDeptName());
            selectVO.setRealname(userInfo.getRealname());
            selectVO.setExpiresTime(DateUtil.formatDate(teamMember.getExpiresTime()));
            selectVOS.add(selectVO);
        }
        return selectVOS;
    }

    /**
     * 添加团队成员
     *
     * @param crmTypeEnum         对应类型
     * @param crmMemberSaveBO data
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMember(CrmTypeEnum crmTypeEnum, CrmMemberSaveBO crmMemberSaveBO) {
        addMember(crmTypeEnum, crmMemberSaveBO, false, new ArrayList<>());
    }

    /**
     * 删除团队成员
     *
     * @param crmTypeEnum         对应类型
     * @param crmMemberSaveBO data
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMember(CrmTypeEnum crmTypeEnum, CrmMemberSaveBO crmMemberSaveBO) {
        for (Integer typeId : crmMemberSaveBO.getIds()) {
            if (crmMemberSaveBO.getChangeType() != null && crmTypeEnum == CrmTypeEnum.CUSTOMER) {
                if (crmMemberSaveBO.getChangeType().contains(1)) {
                    LambdaQueryWrapper<CrmContacts> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(CrmContacts::getCustomerId, typeId);
                    queryWrapper.select(CrmContacts::getContactsId);
                    List<Integer> ids = ApplicationContextHolder.getBean(ICrmContactsService.class).listObjs(queryWrapper, TypeUtils::castToInt);
                    deleteMember(CrmTypeEnum.CONTACTS, new CrmMemberSaveBO(ids, crmMemberSaveBO));
                }
                if (crmMemberSaveBO.getChangeType().contains(2)) {
                    LambdaQueryWrapper<CrmBusiness> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(CrmBusiness::getCustomerId, typeId);
                    queryWrapper.select(CrmBusiness::getBusinessId);
                    List<Integer> ids = ApplicationContextHolder.getBean(ICrmBusinessService.class).listObjs(queryWrapper, TypeUtils::castToInt);
                    deleteMember(CrmTypeEnum.BUSINESS, new CrmMemberSaveBO(ids, crmMemberSaveBO));
                }
                if (crmMemberSaveBO.getChangeType().contains(3)) {
                    LambdaQueryWrapper<CrmContract> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(CrmContract::getCustomerId, typeId);
                    queryWrapper.select(CrmContract::getContractId);
                    List<Integer> ids = ApplicationContextHolder.getBean(ICrmContractService.class).listObjs(queryWrapper, TypeUtils::castToInt);
                    deleteMember(CrmTypeEnum.CONTRACT, new CrmMemberSaveBO(ids, crmMemberSaveBO));
                }
            }
            deleteMembers(crmTypeEnum, typeId, crmMemberSaveBO.getMemberIds());
        }

        updateEsField(crmTypeEnum, crmMemberSaveBO.getIds(), crmMemberSaveBO.getMemberIds(), true);
    }

    /**
     * 退出团队
     *
     * @param crmTypeEnum 对应类型
     * @param typeId  对应类型ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void exitTeam(CrmTypeEnum crmTypeEnum, Integer typeId) {
        deleteMembers(crmTypeEnum, typeId, Collections.singletonList(UserUtil.getUserId()));
        updateEsField(crmTypeEnum, Collections.singletonList(typeId), Collections.singletonList(UserUtil.getUserId()), true);
    }

    /**
     * 添加单条团队成员数据
     *
     * @param crmTypeEnum 对应类型
     * @param typeId  对应类型ID
     * @param userId  用户ID
     * @param power   读写类型
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSingleMember(CrmTypeEnum crmTypeEnum, Integer typeId, Long userId, Integer power, Date expiresTime, String name) {
        /*
          添加单条数据前先尝试删除，防止出现多余的数据
         */
        lambdaUpdate()
                .eq(CrmTeamMembers::getUserId,userId)
                .eq(CrmTeamMembers::getTypeId,typeId)
                .eq(CrmTeamMembers::getType,crmTypeEnum.getType())
                .remove();
        CrmTeamMembers crmTeamMembers = new CrmTeamMembers();
        crmTeamMembers.setUserId(userId);
        crmTeamMembers.setTypeId(typeId);
        crmTeamMembers.setType(crmTypeEnum.getType());
        crmTeamMembers.setPower(power);
        crmTeamMembers.setExpiresTime(expiresTime);
        save(crmTeamMembers);
        addTermMessage(crmTypeEnum, typeId, name, userId, 1);
        updateEsField(crmTypeEnum,Collections.singletonList(typeId),Collections.singletonList(userId),false);
    }

    /**
     * 查询团队成员数量
     *
     * @param crmTypeEnum     对应类型
     * @param typeId      对应类型ID
     * @param ownerUserId 用户ID
     */
    @Override
    public Integer queryMemberCount(CrmTypeEnum crmTypeEnum, Integer typeId, Long ownerUserId) {
        Integer count = lambdaQuery()
                .eq(CrmTeamMembers::getType, crmTypeEnum.getType())
                .eq(CrmTeamMembers::getTypeId, typeId).count();
        return ownerUserId != null ? count + 1 : count;
    }

    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    private TransactionDefinition transactionDefinition;

    /**
     * 删除过期的团队成员数据
     */
    @Override
    public void removeOverdueTeamMembers() {
        List<CrmTeamMembers> teamMembers = lambdaQuery().lt(CrmTeamMembers::getExpiresTime, new Date()).list();
        Map<Integer, List<CrmTeamMembers>> listMap = teamMembers.stream().collect(Collectors.groupingBy(CrmTeamMembers::getType));
        listMap.forEach((type,typeIds)->{
            BulkRequest bulkRequest = new BulkRequest();
            Map<Integer, List<CrmTeamMembers>> typeIdMap = typeIds.stream().collect(Collectors.groupingBy(CrmTeamMembers::getTypeId));
            CrmTypeEnum crmTypeEnum = CrmTypeEnum.parse(type);
            typeIdMap.forEach((typeId,memberIds)->{
                UpdateRequest request = new UpdateRequest(crmTypeEnum.getIndex(),"_doc",typeId.toString());
                List<Long> ids = memberIds.stream().map(CrmTeamMembers::getUserId).collect(Collectors.toList());
                request.script(new Script(ScriptType.INLINE, "painless", "ctx._source.teamMemberIds.removeAll(params.value)", Collections.singletonMap("value", ids)));
                bulkRequest.add(request);
            });
            TransactionStatus transactionStatus = null;
            try {
                transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
                lambdaUpdate().in(CrmTeamMembers::getId,typeIds.stream().map(CrmTeamMembers::getId).collect(Collectors.toList())).remove();
                restTemplate.getClient().bulk(bulkRequest,RequestOptions.DEFAULT);
                dataSourceTransactionManager.commit(transactionStatus);
            } catch (IOException e) {
                dataSourceTransactionManager.rollback(transactionStatus);
            }
        });
    }

    private void addMember(CrmTypeEnum crmTypeEnum, CrmMemberSaveBO crmMemberSaveBO, boolean append, List<CrmTeamMembers> teamMembers) {
        if (crmMemberSaveBO.getPower() != 1 && crmMemberSaveBO.getPower() != 2) {
            throw new CrmException(SystemCodeEnum.SYSTEM_NO_VALID);
        }
        if (crmMemberSaveBO.getMemberIds().size() == 0) {
            return;
        }
        for (Integer id : crmMemberSaveBO.getIds()) {
            if (AuthUtil.isRwAuth(id, crmTypeEnum, CrmAuthEnum.EDIT)) {
                continue;
            }
            List<Long> memberIds = new ArrayList<>(crmMemberSaveBO.getMemberIds());
            LambdaQueryWrapper<CrmTeamMembers> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(CrmTeamMembers::getUserId).eq(CrmTeamMembers::getType, crmTypeEnum.getType())
                    .eq(CrmTeamMembers::getTypeId, id).in(CrmTeamMembers::getUserId,memberIds);
            List<Long> userIds = listObjs(wrapper, TypeUtils::castToLong);
            if(userIds.size() > 0) {
                lambdaUpdate()
                        .set(CrmTeamMembers::getPower,crmMemberSaveBO.getPower())
                        .set(CrmTeamMembers::getExpiresTime,crmMemberSaveBO.getExpiresTime())
                        .in(CrmTeamMembers::getUserId,userIds).update();
                memberIds.removeAll(userIds);
            }
            Object[] objects = getTypeName(crmTypeEnum, id);
            if (objects.length == 0) {
                continue;
            }
            memberIds.removeIf(memberId -> Objects.equals(objects[0], memberId));
            if(memberIds.size() == 0) {
                continue;
            }
            for (Long memberId : memberIds) {
                CrmTeamMembers crmTeamMembers = new CrmTeamMembers();
                crmTeamMembers.setPower(crmMemberSaveBO.getPower());
                crmTeamMembers.setType(crmTypeEnum.getType());
                crmTeamMembers.setTypeId(id);
                crmTeamMembers.setCreateTime(new Date());
                crmTeamMembers.setExpiresTime(crmMemberSaveBO.getExpiresTime());
                crmTeamMembers.setUserId(memberId);
                teamMembers.add(crmTeamMembers);
                addTermMessage(crmTypeEnum, id, (String) objects[1], memberId, 1);
            }
            if (memberIds.size() > 0){
                actionRecordUtil.addMemberActionRecord(crmTypeEnum, id, memberIds, (String) objects[1]);
            }
            updateEsField(crmTypeEnum, Collections.singletonList(id), memberIds, false);
            if (crmMemberSaveBO.getChangeType() != null && crmTypeEnum == CrmTypeEnum.CUSTOMER) {
                if (crmMemberSaveBO.getChangeType().contains(1)) {
                    LambdaQueryWrapper<CrmContacts> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(CrmContacts::getCustomerId, id);
                    queryWrapper.select(CrmContacts::getContactsId);
                    List<Integer> ids = ApplicationContextHolder.getBean(ICrmContactsService.class).listObjs(queryWrapper, TypeUtils::castToInt);
                    addMember(CrmTypeEnum.CONTACTS, new CrmMemberSaveBO(ids, crmMemberSaveBO), true, teamMembers);
                }
                if (crmMemberSaveBO.getChangeType().contains(2)) {
                    LambdaQueryWrapper<CrmBusiness> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(CrmBusiness::getCustomerId, id);
                    queryWrapper.select(CrmBusiness::getBusinessId);
                    List<Integer> ids = ApplicationContextHolder.getBean(ICrmBusinessService.class).listObjs(queryWrapper, TypeUtils::castToInt);
                    addMember(CrmTypeEnum.BUSINESS, new CrmMemberSaveBO(ids, crmMemberSaveBO), true, teamMembers);
                }
                if (crmMemberSaveBO.getChangeType().contains(3)) {
                    LambdaQueryWrapper<CrmContract> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(CrmContract::getCustomerId, id);
                    queryWrapper.select(CrmContract::getContractId);
                    List<Integer> ids = ApplicationContextHolder.getBean(ICrmContractService.class).listObjs(queryWrapper, TypeUtils::castToInt);
                    addMember(CrmTypeEnum.CONTRACT, new CrmMemberSaveBO(ids, crmMemberSaveBO), true, teamMembers);
                }
            }
        }
        if (!append) {
            saveBatch(teamMembers, Const.BATCH_SAVE_SIZE);
        }
    }


    /**
     * 发送通知
     *
     * @param crmTypeEnum 对应crm
     * @param typeId  对应crm类型ID
     * @param title   标题 即对应类型名称
     * @param userId  用户ID
     * @param type    1 新增 2 移除 3 退出
     */
    private void addTermMessage(CrmTypeEnum crmTypeEnum, Integer typeId, String title, Long userId, Integer type) {
        String enumName = "CRM_" + crmTypeEnum.name();

        switch (type) {
            case 1: {
                enumName += "_USER";
                break;
            }
            case 2: {
                enumName += "_REMOVE_TEAM";
                break;
            }
            case 3: {
                enumName += "_TEAM_EXIT";
                break;
            }
            default:
                return;
        }
        AdminMessageEnum adminMessageEnum = AdminMessageEnum.valueOf(enumName);
        AdminMessageBO adminMessageBO = new AdminMessageBO();
        adminMessageBO.setTitle(title);
        adminMessageBO.setTypeId(typeId);
        adminMessageBO.setUserId(UserUtil.getUserId());
        adminMessageBO.setIds(Collections.singletonList(userId));
        adminMessageBO.setMessageType(adminMessageEnum.getType());
        ApplicationContextHolder.getBean(AdminMessageService.class).sendMessage(adminMessageBO);
    }

    private void updateEsField(CrmTypeEnum crmTypeEnum, List<Integer> ids, List<Long> members, boolean isRemove) {
        if (ids.size() == 0 || members.size() == 0) {
            return;
        }
        try {
            for (Integer id : ids) {
                UpdateRequest updateRequest = new UpdateRequest(crmTypeEnum.getIndex(), "_doc", id.toString());
                String script;
                if (isRemove) {
                    script = "if (ctx._source.teamMemberIds== null) {ctx._source.teamMemberIds=[]}else{ctx._source.teamMemberIds.removeAll(params.value)}";
                } else {
                    script = "if (ctx._source.teamMemberIds== null) {ctx._source.teamMemberIds=params.value}else{ctx._source.teamMemberIds.addAll(params.value)}";
                }
                updateRequest.script(new Script(ScriptType.INLINE, "painless", script, Collections.singletonMap("value", members)));
                restTemplate.getClient().update(updateRequest, RequestOptions.DEFAULT);
            }
            restTemplate.refresh(crmTypeEnum.getIndex());
        } catch (Exception ex) {
            log.error("添加团队成员异常:", ex);
        }
    }

    private Object[] getTypeName(CrmTypeEnum crmTypeEnum, Integer typeId) {
        switch (crmTypeEnum) {
            case CUSTOMER: {
                CrmCustomer customer = ApplicationContextHolder.getBean(ICrmCustomerService.class)
                        .lambdaQuery()
                        .select(CrmCustomer::getOwnerUserId, CrmCustomer::getCustomerName)
                        .eq(CrmCustomer::getCustomerId, typeId)
                        .one();
                return new Object[]{customer.getOwnerUserId(), customer.getCustomerName()};
            }
            case CONTRACT: {
                CrmContract contract = ApplicationContextHolder.getBean(ICrmContractService.class)
                        .lambdaQuery()
                        .select(CrmContract::getOwnerUserId, CrmContract::getName)
                        .eq(CrmContract::getContractId, typeId)
                        .one();
                return new Object[]{contract.getOwnerUserId(), contract.getName()};
            }
            case BUSINESS: {
                CrmBusiness business = ApplicationContextHolder.getBean(ICrmBusinessService.class)
                        .lambdaQuery()
                        .select(CrmBusiness::getOwnerUserId, CrmBusiness::getBusinessName)
                        .eq(CrmBusiness::getBusinessId, typeId)
                        .one();
                return new Object[]{business.getOwnerUserId(), business.getBusinessName()};
            }
            case CONTACTS: {
                CrmContacts contacts = ApplicationContextHolder.getBean(ICrmContactsService.class)
                        .lambdaQuery()
                        .select(CrmContacts::getOwnerUserId, CrmContacts::getName)
                        .eq(CrmContacts::getContactsId, typeId)
                        .one();
                return new Object[]{contacts.getOwnerUserId(), contacts.getName()};
            }
            case RECEIVABLES: {
                CrmReceivables receivables = ApplicationContextHolder.getBean(ICrmReceivablesService.class)
                        .lambdaQuery()
                        .select(CrmReceivables::getOwnerUserId, CrmReceivables::getNumber)
                        .eq(CrmReceivables::getReceivablesId, typeId)
                        .one();
                return new Object[]{receivables.getOwnerUserId(), receivables.getNumber()};
            }
            default: {
                return new Object[0];
            }
        }
    }

    private void deleteMembers(CrmTypeEnum crmTypeEnum, Integer typeId, List<Long> memberIds) {
        Object[] objects = getTypeName(crmTypeEnum, typeId);
        if (objects.length == 0) {
            return;
        }
        for (Long memberId : memberIds) {
            Integer count = lambdaQuery()
                    .eq(CrmTeamMembers::getType, crmTypeEnum.getType())
                    .eq(CrmTeamMembers::getTypeId, typeId)
                    .eq(CrmTeamMembers::getUserId, memberId)
                    .count();
            if (count == 0) {
                continue;
            }
            if (!memberId.equals(UserUtil.getUserId())) {
                addTermMessage(crmTypeEnum, typeId, (String) objects[1], memberId, 2);
                actionRecordUtil.addDeleteMemberActionRecord(crmTypeEnum, typeId, memberId, false, (String) objects[1]);
            } else {
                addTermMessage(crmTypeEnum, typeId, (String) objects[1], memberId, 3);
                actionRecordUtil.addDeleteMemberActionRecord(crmTypeEnum, typeId, memberId, true, (String) objects[1]);
            }
        }
        lambdaUpdate()
                .eq(CrmTeamMembers::getType, crmTypeEnum.getType())
                .eq(CrmTeamMembers::getTypeId, typeId)
                .in(CrmTeamMembers::getUserId, memberIds).remove();
    }
}
