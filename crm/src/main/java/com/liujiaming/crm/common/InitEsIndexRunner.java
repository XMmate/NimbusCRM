package com.liujiaming.crm.common;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.util.TypeUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liujiaming.core.common.FieldEnum;
import com.liujiaming.core.common.SystemCodeEnum;
import com.liujiaming.core.exception.CrmException;
import com.liujiaming.core.feign.admin.entity.SimpleUser;
import com.liujiaming.core.field.service.FieldService;
import com.liujiaming.core.servlet.ApplicationContextHolder;
import com.liujiaming.core.utils.UserCacheUtil;
import com.liujiaming.crm.constant.CrmTypeEnum;
import com.liujiaming.crm.entity.BO.CrmFieldDataBO;
import com.liujiaming.crm.entity.PO.CrmCustomerPoolRelation;
import com.liujiaming.crm.entity.PO.CrmField;
import com.liujiaming.crm.entity.PO.CrmTeamMembers;
import com.liujiaming.crm.entity.VO.CrmModelFiledVO;
import com.liujiaming.crm.mapper.CrmFieldMapper;
import com.liujiaming.crm.service.ICrmCustomerPoolRelationService;
import com.liujiaming.crm.service.ICrmFieldService;
import com.liujiaming.crm.service.ICrmTeamMembersService;
import com.liujiaming.crm.service.impl.*;
import com.liujiaming.crm.service.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.liujiaming.crm.common.ActionRecordUtil.THREAD_POOL;

@Component
@Slf4j
public class InitEsIndexRunner implements ApplicationRunner {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private ICrmFieldService crmFieldService;

    @Autowired
    private FieldService fieldService;

    @Override
    public void run(ApplicationArguments args) {
        for (CrmTypeEnum value : CrmTypeEnum.values()) {
            if (!Arrays.asList(CrmTypeEnum.MARKETING, CrmTypeEnum.CUSTOMER_POOL,CrmTypeEnum.NULL).contains(value) && !restTemplate.indexExists(value.getIndex())) {
                initData(value);
                log.info("es {} index init success!", value.getIndex());
            }
        }
    }

    /**
     * 初始化数据
     */
    private void initData(CrmTypeEnum crmTypeEnum) {
        /*
            初始化es索引,获取固定字段以及值
         */
        Map<String, Integer> typeMap = initField(crmTypeEnum);

        CrmFieldMapper fieldMapper = (CrmFieldMapper) crmFieldService.getBaseMapper();
        Integer lastId = 0;
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("primaryKey", crmTypeEnum.getPrimaryKey(false));
        dataMap.put("tableName", crmTypeEnum.getTableName());
        dataMap.put("lastId", lastId);
        dataMap.put("label", crmTypeEnum.getType());
        List<Future<Boolean>> futureList = new LinkedList<>();
        while (true) {
            List<Map<String, Object>> mapList = fieldMapper.initData(dataMap);
            if (mapList.size() == 0) {
                break;
            }
            Object o = mapList.get(mapList.size() - 1).get(crmTypeEnum.getPrimaryKey());
            lastId = TypeUtils.castToInt(o);
            dataMap.put("lastId", lastId);
            log.warn("最后数据id:{},线程id{}", lastId, Thread.currentThread().getName());
            futureList.add(THREAD_POOL.submit(new SaveES(crmTypeEnum, typeMap, mapList)));
        }
        /*
          等待所有数据处理完成,再进行下一步
         */
        for (Future<Boolean> future : futureList) {
            try {
                Boolean result = future.get();
                log.info("数据处理完成：{}", result);
            } catch (InterruptedException | ExecutionException e) {
                log.error("数据错误错误",e);
                throw new CrmException(SystemCodeEnum.SYSTEM_SERVER_ERROR);
            }
        }
        restTemplate.refresh(crmTypeEnum.getIndex());
        lastId = 0;
        while (true) {
            List<CrmFieldDataBO> dataBOS = fieldMapper.initFieldData(lastId, crmTypeEnum.getPrimaryKey(false), crmTypeEnum.getTableName());
            if (dataBOS.size() == 0) {
                break;
            }
            lastId = dataBOS.get(dataBOS.size() - 1).getId();
            log.warn("最后数据id:{},线程id{}", lastId, Thread.currentThread().getName());
            THREAD_POOL.execute(new SaveEsData(crmTypeEnum, dataBOS));
        }

        /* 保存公海信息 */
        if (crmTypeEnum == CrmTypeEnum.CUSTOMER) {
            savePool();
        }

        /* 保存团队成员信息 */
        if (Arrays.asList(CrmTypeEnum.CUSTOMER, CrmTypeEnum.CONTACTS, CrmTypeEnum.BUSINESS, CrmTypeEnum.RECEIVABLES, CrmTypeEnum.CONTRACT).contains(crmTypeEnum)) {
            saveTeamMembers(crmTypeEnum);
        }
        restTemplate.refresh(crmTypeEnum.getIndex());
    }
    private Map<String, Integer> initField(CrmTypeEnum crmTypeEnum) {
        String index = crmTypeEnum.getIndex();
        GetIndexRequest indexRequest = new GetIndexRequest(index);
        try {
            boolean exists = restTemplate.getClient().indices().exists(indexRequest, RequestOptions.DEFAULT);
            if (exists) {
                log.error("索引存在:{}", index);
                throw new CrmException(SystemCodeEnum.SYSTEM_SERVER_ERROR);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*
          查询所有自定义字段
         */
        List<CrmModelFiledVO> crmModelFiledList = queryInitField(crmTypeEnum.getType());
        Map<String, Object> properties = new HashMap<>(crmModelFiledList.size());
        Map<String, Integer> typeMap = new HashMap<>();
        crmModelFiledList.forEach(crmField -> {
            properties.put(crmField.getFieldName(), ElasticUtil.parseType(crmField.getType()));
            if (!Objects.equals(0, crmField.getFieldType())) {
                typeMap.put(crmField.getFieldName(), crmField.getType());
            }
        });
        Map<String, Object> mapping = new HashMap<>();
        mapping.put("properties", properties);
        CreateIndexRequest request = new CreateIndexRequest(index);
        try {
            //设置mapping参数
            request.mapping(mapping);
            CreateIndexResponse createIndexResponse = restTemplate.getClient().indices().create(request, RequestOptions.DEFAULT);
            boolean flag = createIndexResponse.isAcknowledged();
            if (flag) {
                log.info("创建索引库:{}成功！", index);
            }
            return typeMap;
        } catch (IOException e) {
            log.error("创建索引错误", e);
            throw new CrmException(SystemCodeEnum.SYSTEM_SERVER_ERROR);
        }
    }

    private void savePool() {
        CrmTypeEnum crmTypeEnum = CrmTypeEnum.CUSTOMER;
        List<CrmCustomerPoolRelation> list = ApplicationContextHolder.getBean(ICrmCustomerPoolRelationService.class).list();
        Map<Integer, List<CrmCustomerPoolRelation>> collect = list.stream().collect(Collectors.groupingBy(CrmCustomerPoolRelation::getCustomerId));
        Set<Integer> integers = collect.keySet();
        BulkRequest bulkRequest = new BulkRequest();
        for (Integer integer : integers) {
            List<CrmCustomerPoolRelation> poolRelationList = collect.get(integer);
            List<Integer> poolId = new ArrayList<>();
            for (CrmCustomerPoolRelation relation : poolRelationList) {
                poolId.add(relation.getPoolId());
            }
            Map<String, Object> map = new HashMap<>();
            map.put("poolId", poolId);
            UpdateRequest request = new UpdateRequest(crmTypeEnum.getIndex(), "_doc", integer.toString());
            request.doc(map);
            bulkRequest.add(request);
            if (bulkRequest.requests().size() >= 1000) {
                bulk(bulkRequest);
                bulkRequest = new BulkRequest();
            }
        }
        bulk(bulkRequest);
    }


    private void saveTeamMembers(CrmTypeEnum crmTypeEnum) {
        List<CrmTeamMembers> list = ApplicationContextHolder.getBean(ICrmTeamMembersService.class).lambdaQuery().eq(CrmTeamMembers::getType, crmTypeEnum.getType()).list();
        Map<Integer, List<CrmTeamMembers>> collect = list.stream().collect(Collectors.groupingBy(CrmTeamMembers::getTypeId));
        Set<Integer> typeIds = collect.keySet();
        BulkRequest bulkRequest = new BulkRequest();
        for (Integer typeId : typeIds) {
            List<CrmTeamMembers> teamMembers = collect.get(typeId);
            List<Long> memberList = teamMembers.stream().map(CrmTeamMembers::getUserId).collect(Collectors.toList());
            Map<String, Object> map = new HashMap<>();
            map.put("teamMemberIds", memberList);
            UpdateRequest request = new UpdateRequest(crmTypeEnum.getIndex(), "_doc", typeId.toString());
            request.doc(map);
            bulkRequest.add(request);
            if (bulkRequest.requests().size() >= 1000) {
                bulk(bulkRequest);
                bulkRequest = new BulkRequest();
            }
        }
        bulk(bulkRequest);
    }

    private void bulk(BulkRequest bulkRequest) {
        if (bulkRequest.requests().size() == 0) {
            return;
        }
        try {
            BulkResponse bulk = restTemplate.getClient().bulk(bulkRequest, RequestOptions.DEFAULT);
            boolean hasFailures = bulk.hasFailures();
            log.info("bulkHasFailures:{}", bulk.hasFailures());
            if (bulk.hasFailures()) {
                log.info(JSON.toJSONString(bulk.buildFailureMessage()));
                int count = 3;
                while (count > 0 && hasFailures) {
                    count--;
                    bulk = restTemplate.getClient().bulk(bulkRequest, RequestOptions.DEFAULT);
                    hasFailures = bulk.hasFailures();
                }
            }
        } catch (IOException e) {
            throw new CrmException(SystemCodeEnum.SYSTEM_SERVER_ERROR);
        }
    }

    private List<CrmModelFiledVO> queryInitField(Integer type) {
        LambdaQueryWrapper<CrmField> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmField::getLabel, type).orderByAsc(CrmField::getSorting);
        wrapper.groupBy(CrmField::getFieldName);
        List<CrmField> crmFieldList = crmFieldService.list(wrapper);
        CrmTypeEnum crmTypeEnum = CrmTypeEnum.parse(type);
        List<CrmModelFiledVO> filedList = crmFieldList.stream().map(field -> BeanUtil.copyProperties(field, CrmModelFiledVO.class)).collect(Collectors.toList());
        filedList.add(new CrmModelFiledVO("createUserId", FieldEnum.USER, 1));
        filedList.add(new CrmModelFiledVO("createUserName", FieldEnum.TEXT, 1));
        filedList.add(new CrmModelFiledVO("updateTime", FieldEnum.DATETIME, 1));
        filedList.add(new CrmModelFiledVO("createTime", FieldEnum.DATETIME, 1));
        if(CrmTypeEnum.RETURN_VISIT != crmTypeEnum) {
            filedList.add(new CrmModelFiledVO("ownerUserId", FieldEnum.USER, 1));
            filedList.add(new CrmModelFiledVO("ownerUserName", FieldEnum.TEXT, 1));
            filedList.add(new CrmModelFiledVO("ownerDeptId", FieldEnum.STRUCTURE, 1));
            filedList.add(new CrmModelFiledVO("ownerDeptName", FieldEnum.TEXT, 1));
        }
        switch (crmTypeEnum) {
            case LEADS: {
                filedList.add(new CrmModelFiledVO("lastTime", FieldEnum.DATETIME, 1));
                filedList.add(new CrmModelFiledVO("lastContent", FieldEnum.TEXTAREA, 1));
                break;
            }
            case CUSTOMER: {
                filedList.add(new CrmModelFiledVO("lastTime", FieldEnum.DATETIME, 1));
                filedList.add(new CrmModelFiledVO("lastContent", FieldEnum.TEXTAREA, 1));
                filedList.add(new CrmModelFiledVO("receiveTime", FieldEnum.DATETIME, 1));
                filedList.add(new CrmModelFiledVO("dealTime", FieldEnum.DATETIME, 1));
                filedList.add(new CrmModelFiledVO("poolTime", FieldEnum.DATETIME, 1));
                filedList.add(new CrmModelFiledVO("status", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("dealStatus", FieldEnum.SELECT, 1));
                filedList.add(new CrmModelFiledVO("detailAddress", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("address", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("preOwnerUserId", FieldEnum.USER, 1));
                filedList.add(new CrmModelFiledVO("preOwnerUserName", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("teamMemberIds", FieldEnum.USER, 1));
                break;
            }
            case CONTACTS: {
                filedList.add(new CrmModelFiledVO("lastTime", FieldEnum.DATETIME, 1));
                filedList.add(new CrmModelFiledVO("customerName", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("teamMemberIds", FieldEnum.USER, 1));
                break;
            }
            case PRODUCT: {
                filedList.add(new CrmModelFiledVO("categoryName", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("unit", FieldEnum.SELECT, 1));
                break;
            }
            case BUSINESS: {
                filedList.add(new CrmModelFiledVO("typeId", FieldEnum.SELECT, 1));
                filedList.add(new CrmModelFiledVO("statusId", FieldEnum.SELECT, 1));
                filedList.add(new CrmModelFiledVO("lastTime", FieldEnum.DATETIME, 1));
                filedList.add(new CrmModelFiledVO("nextTime", FieldEnum.DATETIME, 1));
                filedList.add(new CrmModelFiledVO("receiveTime", FieldEnum.DATETIME, 1));
                filedList.add(new CrmModelFiledVO("status", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("customerName", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("typeName", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("statusName", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("teamMemberIds", FieldEnum.USER, 1));
                break;
            }
            case CONTRACT: {
                filedList.add(new CrmModelFiledVO("lastTime", FieldEnum.DATETIME, 1));
                filedList.add(new CrmModelFiledVO("companyUserId", FieldEnum.USER, 1));
                filedList.add(new CrmModelFiledVO("checkStatus", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("contractId", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("receivedMoney", FieldEnum.FLOATNUMBER, 1));
                filedList.add(new CrmModelFiledVO("unreceivedMoney", FieldEnum.FLOATNUMBER, 1));
                filedList.add(new CrmModelFiledVO("customerName", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("businessName", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("contactsName", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("companyUserName", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("contractMoney", FieldEnum.FLOATNUMBER, 1));
                filedList.add(new CrmModelFiledVO("teamMemberIds", FieldEnum.USER, 1));
                break;
            }
            case RECEIVABLES: {
                filedList.add(new CrmModelFiledVO("lastTime", FieldEnum.DATETIME, 1));
                filedList.add(new CrmModelFiledVO("checkStatus", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("receivablesPlanId", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("customerName", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("contractNum", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("planNum", FieldEnum.NUMBER, 1));
                filedList.add(new CrmModelFiledVO("contractMoney", FieldEnum.FLOATNUMBER, 1));
                filedList.add(new CrmModelFiledVO("teamMemberIds", FieldEnum.USER, 1));
                break;
            }
            case RECEIVABLES_PLAN: {
                filedList.add(new CrmModelFiledVO("realReceivedMoney", FieldEnum.FLOATNUMBER, 1));
                filedList.add(new CrmModelFiledVO("realReturnDate", FieldEnum.DATETIME, 1));
                filedList.add(new CrmModelFiledVO("unreceivedMoney", FieldEnum.FLOATNUMBER, 1));
                filedList.add(new CrmModelFiledVO("receivedStatus", FieldEnum.SELECT, 1));
                filedList.add(new CrmModelFiledVO("customerName", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("contractNum", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("num", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("contractMoney", FieldEnum.FLOATNUMBER, 1));
                filedList.add(new CrmModelFiledVO("receivablesId", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("remindDate", FieldEnum.DATETIME, 1));
                break;
            }
            case RETURN_VISIT: {
                filedList.add(new CrmModelFiledVO("customerName", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("contactsName", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("contractNum", FieldEnum.TEXT, 1));
                break;
            }

            case INVOICE: {
                filedList.add(new CrmModelFiledVO("contractName", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("contractNum", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("contractMoney", FieldEnum.FLOATNUMBER, 1));
                filedList.add(new CrmModelFiledVO("customerName", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("updateTime", FieldEnum.DATETIME, 1));
                filedList.add(new CrmModelFiledVO("createTime", FieldEnum.DATETIME, 1));
                filedList.add(new CrmModelFiledVO("ownerUserId", FieldEnum.USER, 1));
                filedList.add(new CrmModelFiledVO("ownerUserName", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("checkStatus", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("invoiceStatus", FieldEnum.NUMBER, 1));
                filedList.add(new CrmModelFiledVO("invoiceNumber", FieldEnum.TEXT, 1));
                filedList.add(new CrmModelFiledVO("realInvoiceDate", FieldEnum.DATE, 1));
                filedList.add(new CrmModelFiledVO("logisticsNumber", FieldEnum.TEXT, 1));
                break;
            }
            default:
                break;
        }
        return filedList;
    }


    /**
     * 保存主字段数据
     */
    public class SaveES implements Callable<Boolean> {
        private CrmTypeEnum crmTypeEnum;
        private Map<String, Integer> fieldMap;
        private List<Map<String, Object>> mapList;

        private SaveES(CrmTypeEnum crmTypeEnum, Map<String, Integer> fieldMap, List<Map<String, Object>> mapList) {
            this.crmTypeEnum = crmTypeEnum;
            this.fieldMap = fieldMap;
            this.mapList = mapList;
        }

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Boolean call() {
            log.warn("线程id{}", Thread.currentThread().getName());
            BulkRequest bulkRequest = new BulkRequest();
            for (Map<String, Object> map : mapList) {
                switch (crmTypeEnum) {
                    case LEADS:
                        ApplicationContextHolder.getBean(CrmLeadsServiceImpl.class).setOtherField(map);
                        break;
                    case CUSTOMER:
                        ApplicationContextHolder.getBean(CrmCustomerServiceImpl.class).setOtherField(map);
                        break;
                    case CONTACTS:
                        ApplicationContextHolder.getBean(CrmContactsServiceImpl.class).setOtherField(map);
                        break;
                    case BUSINESS:
                        ApplicationContextHolder.getBean(CrmBusinessServiceImpl.class).setOtherField(map);
                        break;
                    case CONTRACT:
                        ApplicationContextHolder.getBean(CrmContractServiceImpl.class).setOtherField(map);
                        break;
                    case RECEIVABLES:
                        ApplicationContextHolder.getBean(CrmReceivablesServiceImpl.class).setOtherField(map);
                        break;
                    case PRODUCT:
                        ApplicationContextHolder.getBean(CrmProductServiceImpl.class).setOtherField(map);
                        break;
                    case RETURN_VISIT:
                        ApplicationContextHolder.getBean(CrmReturnVisitServiceImpl.class).setOtherField(map);
                        break;
                    case RECEIVABLES_PLAN:
                        ApplicationContextHolder.getBean(CrmReceivablesPlanServiceImpl.class).setOtherField(map);
                    case INVOICE:
                        ApplicationContextHolder.getBean(CrmInvoiceServiceImpl.class).setOtherField(map);
                        break;
                    default:
                        break;
                }
                if(map.containsKey("ownerUserId")){
                    SimpleUser simpleUser = UserCacheUtil.getSimpleUser(TypeUtils.castToLong(map.get("ownerUserId")));
                    map.put("ownerDeptId",simpleUser.getDeptId());
                    map.put("ownerDeptName",simpleUser.getDeptName());
                }
                fieldMap.forEach((k, v) -> {
                    if (FieldEnum.DATE.getType().equals(v)) {
                        Object value = map.remove(k);
                        if (value instanceof Date) {
                            map.put(k, DateUtil.formatDate((Date) value));
                        }
                    } else if (FieldEnum.DATETIME.getType().equals(v)) {
                        Object value = map.remove(k);
                        if (value instanceof Date) {
                            map.put(k, DateUtil.formatDateTime((Date) value));
                        }
                    } else if (fieldService.equalsByType(v)) {
                        Object value = map.remove(k);
                        if (!ObjectUtil.isEmpty(value)) {
                            map.put(k, JSON.toJSONString(value));
                        }
                    }
                });

                IndexRequest request = new IndexRequest(crmTypeEnum.getIndex(), "_doc");
                request.id(map.get(crmTypeEnum.getPrimaryKey()).toString());
                request.source(map);
                bulkRequest.add(request);
                if (bulkRequest.requests().size() >= 1000) {
                    bulk(bulkRequest);
                    bulkRequest = new BulkRequest();
                }
            }
            bulk(bulkRequest);
            mapList.clear();
            return true;
        }
    }

    public class SaveEsData implements Runnable {

        private CrmTypeEnum crmTypeEnum;

        private List<CrmFieldDataBO> fieldDataList;

        private SaveEsData(CrmTypeEnum crmTypeEnum, List<CrmFieldDataBO> fieldDataList) {
            this.crmTypeEnum = crmTypeEnum;
            this.fieldDataList = fieldDataList;
        }

        @Override
        public void run() {
            BulkRequest bulkRequest = new BulkRequest();
            Map<String, List<CrmFieldDataBO>> listMap = fieldDataList.stream().collect(Collectors.groupingBy(CrmFieldDataBO::getBatchId));
            for (List<CrmFieldDataBO> valueList : listMap.values()) {
                if(StrUtil.isEmpty(valueList.get(0).getTypeId())){
                    continue;
                }
                UpdateRequest request = new UpdateRequest(crmTypeEnum.getIndex(), "_doc", valueList.get(0).getTypeId());
                Map<String,Object> map = new HashMap<>(valueList.size());
                for (CrmFieldDataBO fieldDataBO : valueList) {
                    if (fieldDataBO.getName().startsWith("field_") && Arrays.asList(3, 8, 9, 10, 11, 12).contains(fieldDataBO.getType())) {
                        String value = fieldDataBO.getValue();
                        if (StrUtil.isNotEmpty(value)) {
                            map.put(fieldDataBO.getName(), StrUtil.splitTrim(value, ","));
                        } else {
                            map.put(fieldDataBO.getName(), Collections.emptyList());
                        }
                    } else {
                        String value = fieldDataBO.getValue();
                        if (StrUtil.isNotEmpty(value)) {
                            map.put(fieldDataBO.getName(), value);
                        }
                    }
                }
                request.doc(map);
                bulkRequest.add(request);
                if (bulkRequest.requests().size() >= 1000) {
                    bulk(bulkRequest);
                    bulkRequest = new BulkRequest();
                }
            }
            bulk(bulkRequest);
        }
    }

}
