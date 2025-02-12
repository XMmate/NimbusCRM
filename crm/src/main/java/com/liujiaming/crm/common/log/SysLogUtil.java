package com.liujiaming.crm.common.log;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.liujiaming.core.common.FieldEnum;
import com.liujiaming.core.common.log.BehaviorEnum;
import com.liujiaming.core.common.log.Content;
import com.liujiaming.core.entity.UserInfo;
import com.liujiaming.core.feign.admin.entity.SimpleUser;
import com.liujiaming.core.servlet.ApplicationContextHolder;
import com.liujiaming.core.utils.BaseUtil;
import com.liujiaming.core.utils.TagUtil;
import com.liujiaming.core.utils.UserCacheUtil;
import com.liujiaming.core.utils.UserUtil;
import com.liujiaming.crm.common.ActionRecordUtil;
import com.liujiaming.crm.constant.CrmTypeEnum;
import com.liujiaming.crm.entity.PO.CrmActionRecord;
import com.liujiaming.crm.entity.PO.CrmCustomer;
import com.liujiaming.crm.entity.VO.CrmModelFiledVO;
import com.liujiaming.crm.service.*;
import com.liujiaming.crm.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
public class SysLogUtil {

    private static final ExecutorService THREAD_POOL = new ThreadPoolExecutor(10, 20, 5L, TimeUnit.SECONDS, new LinkedBlockingDeque<>(2048), new ThreadPoolExecutor.AbortPolicy());


    public static class ActionRecordTask implements Runnable {
        private static final Integer BATCH_NUMBER = 1;
        private static volatile List<CrmActionRecord> SQL_LIST = new CopyOnWriteArrayList<>();
        private UserInfo userInfo;

        public ActionRecordTask(CrmActionRecord actionRecord) {
            if (actionRecord != null) {
                SQL_LIST.add(actionRecord);
            }
            userInfo = UserUtil.getUser();
        }

        @Override
        public void run() {
            if (SQL_LIST.size() >= BATCH_NUMBER) {
                List<CrmActionRecord> list = new ArrayList<>(SQL_LIST);
                //底层已经做过size为0的判断，此处不再限制
                try {
                    UserUtil.setUser(userInfo);
                    ApplicationContextHolder.getBean(ICrmActionRecordService.class).saveBatch(list, BATCH_NUMBER);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    UserUtil.removeUser();
                    SQL_LIST.clear();
                }
            }
        }
    }


    private List<String> textList = new ArrayList<>();

    /**
     * 更新记录
     *
     * @param oldObj  之前对象
     * @param newObj  新对象
     * @param crmTypeEnum 类型
     */
    @SuppressWarnings("unchecked")
    public Content updateRecord(Map<String, Object> oldObj, Map<String, Object> newObj, CrmTypeEnum crmTypeEnum, String name) {
        try {
            searchChange(textList, oldObj, newObj, crmTypeEnum.getType());
            return new Content(name,StrUtil.join("", textList),BehaviorEnum.UPDATE);
        } finally {
            textList.clear();
        }

    }

    public Content addRecord(CrmTypeEnum crmTypeEnum, String name) {
        return new Content(name,"新建了" + crmTypeEnum.getRemarks() + "：" + name,BehaviorEnum.SAVE);
    }

    @SuppressWarnings("unchecked")
    public void updateRecord(List<CrmModelFiledVO> newFieldList, Dict kv) {
        textList.clear();
        if (newFieldList == null) {
            return;
        }
        List<CrmModelFiledVO> oldFieldList = ApplicationContextHolder.getBean(ICrmActionRecordService.class).queryFieldValue(kv);
        newFieldList.forEach(newField -> {
            for (CrmModelFiledVO oldField : oldFieldList) {
                if (oldField.getFieldId().equals(newField.getFieldId())) {
                    if (ObjectUtil.isEmpty(oldField.getValue()) && ObjectUtil.isEmpty(newField.getValue())) {
                        continue;
                    }
                    if(Objects.equals(FieldEnum.parse(oldField.getType()),FieldEnum.DETAIL_TABLE)){
                        ActionRecordUtil.parseDetailTable(oldField.getValue(),newField.getValue(),oldField.getName(),oldField.getType(),textList);
                    }else{
                        String oldFieldValue = (String) ActionRecordUtil.parseValue(oldField.getValue(),oldField.getType(),true);
                        String newFieldValue = (String) ActionRecordUtil.parseValue(newField.getValue(),newField.getType(),true);
                        if (!oldFieldValue.equals(newFieldValue)) {
                            textList.add("将" + oldField.getName() + " 由" + oldFieldValue + "修改为" + newFieldValue + "。");
                        }
                    }
                }
            }
        });
    }

    private void searchChange(List<String> textList, Map<String, Object> oldObj, Map<String, Object> newObj, Integer crmTypes) {
        for (String oldKey : oldObj.keySet()) {
            for (String newKey : newObj.keySet()) {
                if (ActionRecordUtil.propertiesMap.get(crmTypes).containsKey(oldKey)) {
                    Object oldValue = oldObj.get(oldKey);
                    Object newValue = newObj.get(newKey);
                    if (oldValue instanceof Date) {
                        oldValue = DateUtil.formatDateTime((Date) oldValue);
                    }
                    if (newValue instanceof Date) {
                        newValue = DateUtil.formatDateTime((Date) newValue);
                    }
                    if (ObjectUtil.isEmpty(oldValue) || ("address".equals(oldKey) && ",,".equals(oldValue))) {
                        oldValue = "空";
                    }
                    if (ObjectUtil.isEmpty(newValue) || ("address".equals(newKey) && ",,".equals(newValue))) {
                        newValue = "空";
                    }
                    if (oldValue instanceof BigDecimal || newValue instanceof BigDecimal) {
                        oldValue = Convert.toBigDecimal(oldValue, new BigDecimal(0)).setScale(2, BigDecimal.ROUND_UP).toString();
                        newValue = Convert.toBigDecimal(newValue, new BigDecimal(0)).setScale(2, BigDecimal.ROUND_UP).toString();
                    }
                    if (newKey.equals(oldKey) && !Objects.equals(oldValue,newValue)) {
                        switch (oldKey) {
                            case "companyUserId":
                                if (!"空".equals(newValue)) {
                                    newValue = UserCacheUtil.getUserName(Long.valueOf(newValue.toString()));
                                }
                                if (!"空".equals(oldValue)) {
                                    oldValue = UserCacheUtil.getUserName(Long.valueOf(oldValue.toString()));
                                }
                                break;
                            case "customerId":
                                if (!"空".equals(newValue)) {
                                    newValue = ApplicationContextHolder.getBean(ICrmCustomerService.class).getCustomerName(Integer.valueOf(newValue.toString()));
                                }
                                if (!"空".equals(oldValue)) {
                                    oldValue = ApplicationContextHolder.getBean(ICrmCustomerService.class).getCustomerName(Integer.valueOf(oldValue.toString()));
                                }
                                break;
                            case "businessId":
                                if (!"空".equals(newValue)) {
                                    newValue = ApplicationContextHolder.getBean(ICrmBusinessService.class).getBusinessName(Integer.parseInt(newValue.toString()));
                                }
                                if (!"空".equals(oldValue)) {
                                    oldValue = ApplicationContextHolder.getBean(ICrmBusinessService.class).getBusinessName(Integer.parseInt(oldValue.toString()));
                                }
                                break;
                            case "contractId":
                                if (!"空".equals(newValue)) {
                                    newValue = ApplicationContextHolder.getBean(ICrmContractService.class).getContractName(Integer.parseInt(newValue.toString()));
                                }
                                if (!"空".equals(oldValue)) {
                                    oldValue = ApplicationContextHolder.getBean(ICrmContractService.class).getContractName(Integer.parseInt(oldValue.toString()));
                                }
                                break;
                            case "contactsId":
                                if (!"空".equals(newValue)) {
                                    newValue = ApplicationContextHolder.getBean(ICrmContactsService.class).getContactsName(Integer.parseInt(newValue.toString()));
                                }
                                if (!"空".equals(oldValue)) {
                                    oldValue = ApplicationContextHolder.getBean(ICrmContactsService.class).getContactsName(Integer.parseInt(oldValue.toString()));
                                }
                                break;
                            case "typeId":
                                if (!"空".equals(newValue)) {
                                    newValue = ApplicationContextHolder.getBean(ICrmBusinessTypeService.class).getBusinessTypeName(Integer.parseInt(newValue.toString()));
                                }
                                if (!"空".equals(oldValue)) {
                                    oldValue = ApplicationContextHolder.getBean(ICrmBusinessTypeService.class).getBusinessTypeName(Integer.parseInt(oldValue.toString()));
                                }
                                break;
                            case "statusId":
                                if (!"空".equals(newValue)) {
                                    newValue = ApplicationContextHolder.getBean(ICrmBusinessStatusService.class).getBusinessStatusName(Integer.parseInt(newValue.toString()));
                                }
                                if (!"空".equals(oldValue)) {
                                    oldValue = ApplicationContextHolder.getBean(ICrmBusinessStatusService.class).getBusinessStatusName(Integer.parseInt(oldValue.toString()));
                                }
                                break;
                            case "receivablesPlanId":
                                if (!"空".equals(newValue)) {
                                    newValue = ApplicationContextHolder.getBean(ICrmReceivablesPlanService.class).getReceivablesPlanNum(Integer.parseInt(newValue.toString()));
                                }
                                if (!"空".equals(oldValue)) {
                                    oldValue = ApplicationContextHolder.getBean(ICrmReceivablesPlanService.class).getReceivablesPlanNum(Integer.parseInt(oldValue.toString()));
                                }
                                break;
                            case "categoryId":
                                if (!"空".equals(newValue)) {
                                    newValue = ApplicationContextHolder.getBean(ICrmProductCategoryService.class).getProductCategoryName(Integer.parseInt(newValue.toString()));
                                }
                                if (!"空".equals(oldValue)) {
                                    oldValue = ApplicationContextHolder.getBean(ICrmProductCategoryService.class).getProductCategoryName(Integer.parseInt(oldValue.toString()));
                                }
                                break;
                            case "crmType":
                                if (!"空".equals(newValue)) {
                                    newValue = newValue.equals(1) ? "线索" : "客户";
                                }
                                if (!"空".equals(oldValue)) {
                                    oldValue = oldValue.equals(1) ? "线索" : "客户";
                                }
                                break;
                            case "relationUserId":
                                if (!"空".equals(newValue)) {
                                    List<SimpleUser> newList = UserCacheUtil.getSimpleUsers(TagUtil.toLongSet((String) newValue));
                                    newValue = newList.stream().map(SimpleUser::getRealname).collect(Collectors.joining(","));
                                }
                                if (!"空".equals(oldValue)) {
                                    List<SimpleUser> oldList = UserCacheUtil.getSimpleUsers(TagUtil.toLongSet((String) oldValue));
                                    oldValue = oldList.stream().map(SimpleUser::getRealname).collect(Collectors.joining(","));
                                }
                                break;
                            default:
                                break;
                        }

                        if (ObjectUtil.isEmpty(oldValue)) {
                            oldValue = "空";
                        }
                        if (ObjectUtil.isEmpty(newValue)) {
                            newValue = "空";
                        }
                        textList.add("将" + ActionRecordUtil.propertiesMap.get(crmTypes).get(oldKey) + " 由" + oldValue + "修改为" + newValue + "。");
                    }
                }
            }
        }
    }

    /**
     * 添加转移记录
     *
     */
    public Content addConversionRecord(CrmTypeEnum crmTypeEnum, Long userId, String name) {
        String userName = UserCacheUtil.getUserName(userId);
        return new Content(name,"将" + crmTypeEnum.getRemarks() + "：" + name + "转移给：" + userName,BehaviorEnum.CHANGE_OWNER);
    }

    @Autowired
    private ICrmCustomerService crmCustomerService;

    /**
     * 添加(锁定/解锁)记录
     */
    public List<Content> addIsLockRecord(List<String> ids, Integer isLock) {
        List<Content> contentList = new ArrayList<>();
        for (String actionId : ids) {
            String name = crmCustomerService.lambdaQuery().select(CrmCustomer::getCustomerName).eq(CrmCustomer::getCustomerId, actionId).one().getCustomerName();
            String detail;
            if (isLock == 2) {
                detail = "将客户：" + name + "锁定";
                contentList.add(new Content(name,detail,BehaviorEnum.LOCK));
            } else {
                detail = "将客户：" + name + "解锁";
                contentList.add(new Content(name,detail,BehaviorEnum.UNLOCK));
            }
        }
        return contentList;
    }



    public Content addDeleteActionRecord(CrmTypeEnum crmTypeEnum, String name) {
        return new Content(name,"删除了" + crmTypeEnum.getRemarks() + "：" + name,BehaviorEnum.DELETE);
    }

    public Content addMemberActionRecord(CrmTypeEnum crmTypeEnum, Integer actionId, Long userId, String name) {
        String userName = UserCacheUtil.getUserName(userId);
        return new Content(name,"给" + crmTypeEnum.getRemarks() + "：" + name + "添加了团队成员：" + userName);
    }

    public Content addDeleteMemberActionRecord(CrmTypeEnum crmTypeEnum, Long userId, boolean isSelf, String name) {
        if (isSelf) {
            return new Content(name,"退出了" + crmTypeEnum.getRemarks() + "：" + name + "的团队成员",BehaviorEnum.EXIT_MEMBER);
        } else {
            String userName = UserCacheUtil.getUserName(userId);
            return new Content(name,"移除了" + crmTypeEnum.getRemarks() + "：" + name + "的团队成员：" + userName,BehaviorEnum.REMOVE_MEMBER);
        }
    }

    public void addOaLogSaveRecord(CrmTypeEnum crmTypeEnum, Integer actionId) {
        CrmActionRecord actionRecord = new CrmActionRecord();
        actionRecord.setCreateUserId(UserUtil.getUserId());
        actionRecord.setCreateTime(new Date());
        actionRecord.setIpAddress(BaseUtil.getIp());
        actionRecord.setTypes(crmTypeEnum.getType());
        actionRecord.setBehavior(BehaviorEnum.SAVE.getType());
        actionRecord.setActionId(actionId);
        actionRecord.setDetail("新建了" + crmTypeEnum.getRemarks() + "：" + DateUtil.formatDate(new Date()));
        actionRecord.setObject(DateUtil.formatDate(new Date()));
        ActionRecordTask actionRecordTask = new ActionRecordTask(actionRecord);
        THREAD_POOL.execute(actionRecordTask);
    }

    public void addCrmExamineActionRecord(CrmTypeEnum crmTypeEnum, Integer actionId, BehaviorEnum behaviorEnum, String number) {
        CrmActionRecord actionRecord = new CrmActionRecord();
        actionRecord.setCreateUserId(UserUtil.getUserId());
        actionRecord.setCreateTime(new Date());
        actionRecord.setIpAddress(BaseUtil.getIp());
        actionRecord.setTypes(crmTypeEnum.getType());
        actionRecord.setBehavior(behaviorEnum.getType());
        actionRecord.setActionId(actionId);
        String prefix = "";
        switch (behaviorEnum) {
            case SUBMIT_EXAMINE:
                prefix = "提交了";
                break;
            case RECHECK_EXAMINE:
                prefix = "撤回了";
                break;
            case PASS_EXAMINE:
                prefix = "通过了";
                break;
            case REJECT_EXAMINE:
                prefix = "驳回了";
                break;
            default:
                break;
        }
        actionRecord.setDetail(prefix + crmTypeEnum.getRemarks() + "：" + number);
        actionRecord.setObject(number);
        ActionRecordTask actionRecordTask = new ActionRecordTask(actionRecord);
        THREAD_POOL.execute(actionRecordTask);
    }

    /**
     * 通用模板，无需特殊处理的操作记录适用
     *
     * @param crmTypeEnum
     * @param actionId
     * @param behaviorEnum
     */
    public void addObjectActionRecord(CrmTypeEnum crmTypeEnum, Integer actionId, BehaviorEnum behaviorEnum, String name) {
        CrmActionRecord actionRecord = new CrmActionRecord();
        actionRecord.setCreateUserId(UserUtil.getUserId());
        actionRecord.setCreateTime(new Date());
        actionRecord.setIpAddress(BaseUtil.getIp());
        actionRecord.setTypes(crmTypeEnum.getType());
        actionRecord.setBehavior(behaviorEnum.getType());
        actionRecord.setActionId(actionId);
        String detail;
        switch (behaviorEnum) {
            case CANCEL_EXAMINE:
                detail = "将" + crmTypeEnum.getRemarks() + "：" + name + "作废";
                break;
            case FOLLOW_UP:
                detail = "给" + crmTypeEnum.getRemarks() + "：" + name + "新建了跟进记录";
                break;
            default:
                detail = behaviorEnum.getName() + "了" + crmTypeEnum.getRemarks() + "：" + name;
                break;
        }
        actionRecord.setDetail(detail);
        actionRecord.setObject(name);
        ActionRecordTask actionRecordTask = new ActionRecordTask(actionRecord);
        THREAD_POOL.execute(actionRecordTask);
    }
}
