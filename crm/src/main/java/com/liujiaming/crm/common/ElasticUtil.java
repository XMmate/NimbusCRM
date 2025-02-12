package com.liujiaming.crm.common;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.liujiaming.core.common.FieldEnum;
import com.liujiaming.core.common.JxcEnum;
import com.liujiaming.core.common.SystemCodeEnum;
import com.liujiaming.core.exception.CrmException;
import com.liujiaming.core.feign.crm.entity.BiParams;
import com.liujiaming.core.servlet.ApplicationContextHolder;
import com.liujiaming.core.utils.BiTimeUtil;
import com.liujiaming.crm.constant.CrmTypeEnum;
import com.liujiaming.crm.entity.BO.CrmSearchBO;
import com.liujiaming.crm.entity.BO.EsUpdateFieldBO;
import com.liujiaming.crm.entity.BO.EsUpdatePropertiesBO;
import com.liujiaming.crm.entity.PO.CrmFieldConfig;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

import java.io.IOException;
import java.util.*;

/**
 * @author elastic的一些通用操作
 */
@Slf4j
public class ElasticUtil {
    public static void addField(RestHighLevelClient client, CrmFieldConfig crmField, Integer fieldType) {
        CrmTypeEnum crmTypeEnum = CrmTypeEnum.parse(crmField.getLabel());
        try {
            JSONObject object = new JSONObject();
            JSONObject child = new JSONObject();
            child.put(StrUtil.toCamelCase(crmField.getFieldName()), parseType(fieldType));
            object.put("properties", child);
            PutMappingRequest request = new PutMappingRequest(crmTypeEnum.getIndex());
            request.source(object);
            client.indices().putMapping(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("新增字段错误", e);
            throw new CrmException(SystemCodeEnum.SYSTEM_ERROR);
        }
    }

    public static void removeField(RestHighLevelClient client, String fieldName, Integer label) {
        UpdateByQueryRequest request = new UpdateByQueryRequest(CrmTypeEnum.parse(label).getIndex());
        request.setScript(new Script(ScriptType.INLINE, "painless", "ctx._source." + StrUtil.toCamelCase(fieldName) + " = null", new HashMap<>()));
        request.setBatchSize(1000);
        request.setRefresh(true);
        try {
            BulkByScrollResponse bulkByScrollResponse = client.updateByQuery(request, RequestOptions.DEFAULT);
            log.info(JSON.toJSONString(bulkByScrollResponse));
        } catch (IOException e) {
            log.error("删除字段错误", e);
            throw new CrmException(SystemCodeEnum.SYSTEM_ERROR);
        }
    }

    public static Map<String, Object> parseType(Integer type) {
        FieldEnum fieldEnum = FieldEnum.parse(type);
        Map<String, Object> map = new HashMap<>();
        String name;
        switch (fieldEnum) {
            case TEXT:
                name = "keyword";
                break;
            case TEXTAREA:
                name = "text";
                map.put("fields", new JSONObject().fluentPut("sort", new JSONObject().fluentPut("type", "icu_collation_keyword").fluentPut("language", "zh").fluentPut("country", "CN"))
                        .fluentPut("keyword", new JSONObject().fluentPut("type", "keyword")));
                break;
            case SELECT:
                name = "keyword";
                break;
            case DATE:
                name = "date";
                map.put("format", "yyyy-MM-dd");
                break;
            case NUMBER:
                name = "scaled_float";
                map.put("scaling_factor", 1);
                break;
            case FLOATNUMBER:
                name = "scaled_float";
                map.put("scaling_factor", 100);
                break;
            case MOBILE:
                name = "keyword";
                break;
            case FILE:
                name = "keyword";
                break;
            case CHECKBOX:
                name = "keyword";
                break;
            case USER:
                name = "keyword";
                break;
            case STRUCTURE:
                name = "keyword";
                break;
            case DATETIME:
                name = "date";
                map.put("format", "yyyy-MM-dd HH:mm:ss");
                break;
            case EMAIL:
                name = "keyword";
                break;
            default:
                name = "keyword";
                break;
        }
        map.put("type", name);
        if ("keyword".equals(name)) {
            map.put("fields", new JSONObject().fluentPut("sort", new JSONObject().fluentPut("type", "icu_collation_keyword").fluentPut("language", "zh").fluentPut("country", "CN")));
        }
        return map;
    }

    public static void updateField(RestHighLevelClient client, EsUpdateFieldBO esUpdateFieldBO, List<String> indexs) {
        UpdateByQueryRequest request = new UpdateByQueryRequest(indexs.toArray(new String[0]));
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
		queryBuilder.filter(QueryBuilders.termQuery(esUpdateFieldBO.getConditionField(),
						esUpdateFieldBO.getConditionValue()));
		if (MapUtil.isNotEmpty(esUpdateFieldBO.getConditions())) {
			esUpdateFieldBO.getConditions().forEach((k, v) -> {
				queryBuilder.filter(QueryBuilders.termQuery(k, v));
			});
		}
		request.setQuery(queryBuilder);

        String script = "ctx._source.%s=params.value";
        request.setScript(new Script(ScriptType.INLINE, "painless", String.format(script, esUpdateFieldBO.getUpdateField()), Collections.singletonMap("value", esUpdateFieldBO.getUpdateValue())));
        request.setBatchSize(1000);
        request.setRefresh(true);
        try {
            client.updateByQuery(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new CrmException(SystemCodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 冗余字段map对应
     * 这里是线程安全的 static方法只会在类初始化的时候加载一次，其他地方都是get()操作，因此updateMap是线程安全的
     */
    private static Map<String, List<EsUpdatePropertiesBO>> updateMap = new HashMap<>();

    static {
        List<EsUpdatePropertiesBO> userPropertiesList = new ArrayList<>();
        userPropertiesList.add(new EsUpdatePropertiesBO("ownerUserId", "ownerUserName", Lists.newArrayList(CrmTypeEnum.LEADS.getIndex()
                , CrmTypeEnum.CUSTOMER.getIndex(), CrmTypeEnum.CONTACTS.getIndex(), CrmTypeEnum.CONTRACT.getIndex(), CrmTypeEnum.BUSINESS.getIndex()
                , CrmTypeEnum.RECEIVABLES.getIndex(), CrmTypeEnum.RETURN_VISIT.getIndex(), CrmTypeEnum.PRODUCT.getIndex(), CrmTypeEnum.INVOICE.getIndex())));
        userPropertiesList.add(new EsUpdatePropertiesBO("ownerDeptId", "ownerDeptName", Lists.newArrayList(CrmTypeEnum.LEADS.getIndex()
                , CrmTypeEnum.CUSTOMER.getIndex(), CrmTypeEnum.CONTACTS.getIndex(), CrmTypeEnum.CONTRACT.getIndex(), CrmTypeEnum.BUSINESS.getIndex()
                , CrmTypeEnum.RECEIVABLES.getIndex(), CrmTypeEnum.RETURN_VISIT.getIndex(), CrmTypeEnum.PRODUCT.getIndex(), CrmTypeEnum.INVOICE.getIndex())));
        userPropertiesList.add(new EsUpdatePropertiesBO("createUserId", "createUserName", Lists.newArrayList(CrmTypeEnum.LEADS.getIndex()
                , CrmTypeEnum.CUSTOMER.getIndex(), CrmTypeEnum.CONTACTS.getIndex(), CrmTypeEnum.CONTRACT.getIndex(), CrmTypeEnum.BUSINESS.getIndex()
                , CrmTypeEnum.RECEIVABLES.getIndex(), CrmTypeEnum.RETURN_VISIT.getIndex(), CrmTypeEnum.PRODUCT.getIndex(), CrmTypeEnum.INVOICE.getIndex())));
        userPropertiesList.add(new EsUpdatePropertiesBO("companyUserId", "companyUserName", Lists.newArrayList(CrmTypeEnum.CONTRACT.getIndex())));
        updateMap.put("user", userPropertiesList);
        List<EsUpdatePropertiesBO> deptPropertiesList = new ArrayList<>(2);
        deptPropertiesList.add(new EsUpdatePropertiesBO("ownerDeptId", "ownerDeptName", Lists.newArrayList(CrmTypeEnum.LEADS.getIndex()
                , CrmTypeEnum.CUSTOMER.getIndex(), CrmTypeEnum.CONTACTS.getIndex(), CrmTypeEnum.CONTRACT.getIndex(), CrmTypeEnum.BUSINESS.getIndex()
                , CrmTypeEnum.RECEIVABLES.getIndex(), CrmTypeEnum.PRODUCT.getIndex(), CrmTypeEnum.INVOICE.getIndex())));
        updateMap.put("dept", deptPropertiesList);
        List<EsUpdatePropertiesBO> customerPropertiesList = new ArrayList<>();

        List<String> customerIndexList = Lists.newArrayList(CrmTypeEnum.CONTACTS.getIndex(),
                CrmTypeEnum.BUSINESS.getIndex(), CrmTypeEnum.CONTRACT.getIndex(), CrmTypeEnum.RECEIVABLES.getIndex(),
                CrmTypeEnum.RETURN_VISIT.getIndex(), CrmTypeEnum.INVOICE.getIndex());

        customerPropertiesList.add(new EsUpdatePropertiesBO("customerId", "customerName", customerIndexList));
        updateMap.put("customer", customerPropertiesList);
        List<EsUpdatePropertiesBO> contactsPropertiesList = new ArrayList<>();
        contactsPropertiesList.add(new EsUpdatePropertiesBO("contactsId", "contactsName", Lists.newArrayList(CrmTypeEnum.CONTRACT.getIndex(),
                CrmTypeEnum.RETURN_VISIT.getIndex())));
        updateMap.put("contacts", contactsPropertiesList);
        List<EsUpdatePropertiesBO> businessPropertiesList = new ArrayList<>();
        businessPropertiesList.add(new EsUpdatePropertiesBO("businessId", "businessName", Lists.newArrayList(CrmTypeEnum.CONTRACT.getIndex())));
        updateMap.put("business", businessPropertiesList);
        List<EsUpdatePropertiesBO> contractPropertiesList = new ArrayList<>();
        contractPropertiesList.add(new EsUpdatePropertiesBO("contractId", "contractNum", Lists.newArrayList(CrmTypeEnum.RECEIVABLES.getIndex(), CrmTypeEnum.RETURN_VISIT.getIndex(), CrmTypeEnum.INVOICE.getIndex())));
        updateMap.put("contract", contractPropertiesList);
        List<EsUpdatePropertiesBO> productPropertiesList = new ArrayList<>();
        productPropertiesList.add(new EsUpdatePropertiesBO("categoryId", "categoryName", Lists.newArrayList(CrmTypeEnum.RECEIVABLES.getIndex(), CrmTypeEnum.RETURN_VISIT.getIndex())));
        updateMap.put("product", productPropertiesList);
    }

    /**
     * 根据类型更新es冗余数据
     *
     * @param client
     * @param type
     * @param id
     * @param name
     */
    public static void batchUpdateEsData(RestHighLevelClient client, String type, String id, String name) {
        // sourceProperties =(new EsUpdatePropertiesBO("businessId", "businessName", Lists.newArrayList(CrmTypeEnum.CONTRACT.getIndex())));
        List<EsUpdatePropertiesBO> sourceProperties = updateMap.get(type);
        List<EsUpdatePropertiesBO> propertiesList = JSON.parseArray(JSON.toJSONString(sourceProperties), EsUpdatePropertiesBO.class);
        DiscoveryClient discoveryClient = ApplicationContextHolder.getBean(DiscoveryClient.class);

        if (CollUtil.isNotEmpty(discoveryClient.getInstances("jxc"))) {
            EsUpdatePropertiesBO customerProperty = propertiesList.stream().filter(p -> ObjectUtil.equal("customerId", p.getIdField())).findFirst().orElse(null);
            if (ObjectUtil.isNotNull(customerProperty)) {
                customerProperty.getIndexs().add(JxcEnum.SALE.getIndex());
                customerProperty.getIndexs().add(JxcEnum.SALE_RETURN.getIndex());
            }
            // 销售回款
            Map<String, String> collectionSupplierMap = new HashMap<>();
            collectionSupplierMap.put("collectionType", "1");
            propertiesList.add(new EsUpdatePropertiesBO("collectionObjectId", "collectionObject",
                    Lists.newArrayList(JxcEnum.COLLECTION.getIndex()), collectionSupplierMap));
            // 销售退货
            Map<String, String> paymentSupplierMap = new HashMap<>();
            paymentSupplierMap.put("paymentType", "11");
            propertiesList.add(new EsUpdatePropertiesBO("collectionObjectId", "collectionObject",
                    Lists.newArrayList(JxcEnum.PAYMENT.getIndex()), paymentSupplierMap));
        }
		for (EsUpdatePropertiesBO properties : propertiesList) {
			if (MapUtil.isEmpty(properties.getConditions())) {
				updateField(client, new EsUpdateFieldBO(properties.getIdField(), id, properties.getNameField(), name), properties.getIndexs());
			} else {
				updateField(client, new EsUpdateFieldBO(properties.getIdField(), id, properties.getNameField(), name,
								properties.getConditions()),
						properties.getIndexs());
			}
		}
    }

    public static void updateField(ElasticsearchRestTemplate template, String fieldName, Object value, List<Integer> ids, String index) {
        BulkRequest bulkRequest = new BulkRequest();
        ids.forEach(id -> {
            Map<String, Object> map = new HashMap<>();
            map.put(fieldName, value);
            UpdateRequest request = new UpdateRequest(index, "_doc", id.toString());
            request.doc(map);
            bulkRequest.add(request);
        });
        try {
            template.getClient().bulk(bulkRequest, RequestOptions.DEFAULT);
            template.refresh(index);
        } catch (IOException e) {
            log.error("es修改失败", e);
        }
    }

    /**
     * 修改多个字段的值
     *
     * @param id id
     */
    public static void updateField(ElasticsearchRestTemplate template, Map<String, Object> map, Integer id, String index) {
        try {
            UpdateRequest request = new UpdateRequest(index, "_doc", id.toString());
            request.doc(map);
            template.getClient().update(request, RequestOptions.DEFAULT);
            template.refresh(index);
        } catch (IOException e) {
            log.error("es修改失败", e);
        }
    }


    /**
     * 普通文本类型的es搜索
     *
     * @param search       搜索条件
     * @param queryBuilder 查询器
     */
    public static void textSearch(CrmSearchBO.Search search, BoolQueryBuilder queryBuilder) {
        if (search.getValues().size() == 0 && !Arrays.asList(5, 6).contains(search.getSearchEnum().getType())) {
            return;
        }
        switch (search.getSearchEnum()) {
            case IS:
                queryBuilder.filter(QueryBuilders.termsQuery(search.getName(), search.getValues()));
                break;
            case IS_NOT:
                queryBuilder.mustNot(QueryBuilders.termsQuery(search.getName(), search.getValues()));
                break;
            case PREFIX:
                if (search.getValues().size() == 1) {
                    queryBuilder.filter(QueryBuilders.prefixQuery(search.getName(), search.getValues().get(0)));
                } else {
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    for (String value : search.getValues()) {
                        boolQuery.should(QueryBuilders.prefixQuery(search.getName(), value));
                    }
                    queryBuilder.filter(boolQuery);
                }
                break;
            case SUFFIX:
            case CONTAINS:
                String suffix = search.getSearchEnum() == CrmSearchBO.FieldSearchEnum.SUFFIX ? "" : "*";
                if (search.getValues().size() == 1) {
                    queryBuilder.filter(QueryBuilders.wildcardQuery(search.getName(), "*" + search.getValues().get(0) + suffix));
                } else {
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    for (String value : search.getValues()) {
                        boolQuery.should(QueryBuilders.wildcardQuery(search.getName(), "*" + value + suffix));
                    }
                    queryBuilder.filter(boolQuery);
                }
                break;
            case NOT_CONTAINS:
                if (search.getValues().size() == 1) {
                    queryBuilder.mustNot(QueryBuilders.wildcardQuery(search.getName(), "*" + search.getValues().get(0) + "*"));
                } else {
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    for (String value : search.getValues()) {
                        boolQuery.should(QueryBuilders.wildcardQuery(search.getName(), "*" + value + "*"));
                    }
                    queryBuilder.mustNot(boolQuery);
                }
                break;
            case IS_NULL:
                isNullSearch(search, queryBuilder);
                break;
            case IS_NOT_NULL:
                isNotNullSearch(search, queryBuilder);
                break;
        }
    }

    /**
     * 多选类型的es搜索
     *
     * @param search       搜索条件
     * @param queryBuilder 查询器
     */
    public static void checkboxSearch(CrmSearchBO.Search search, BoolQueryBuilder queryBuilder) {
        if (search.getValues().size() == 0 && !Arrays.asList(5, 6).contains(search.getSearchEnum().getType())) {
            return;
        }
        switch (search.getSearchEnum()) {
            case IS_NOT: {
                BoolQueryBuilder builder = QueryBuilders.boolQuery();
                Map<String, Object> map = new HashMap<>();
                map.put("key", search.getName());
                map.put("size", search.getValues().size());
                Script painless = new Script(ScriptType.INLINE, "painless", "doc[params.key].size() != params.size", map);
                builder.should(QueryBuilders.scriptQuery(painless));
                builder.should(QueryBuilders.boolQuery().mustNot(QueryBuilders.termsQuery(search.getName(), search.getValues())));
                queryBuilder.filter(builder);
                break;
            }
            case IS: {
                Map<String, Object> map = new HashMap<>();
                map.put("key", search.getName());
                map.put("size", search.getValues().size());
                Script painless = new Script(ScriptType.INLINE, "painless", "doc[params.key].size() == params.size", map);
                queryBuilder.filter(QueryBuilders.scriptQuery(painless));
                queryBuilder.filter(QueryBuilders.termsQuery(search.getName(), search.getValues()));
                break;
            }
            case CONTAINS:
                queryBuilder.filter(QueryBuilders.termsQuery(search.getName(), search.getValues()));
                break;
            case NOT_CONTAINS: {
                queryBuilder.mustNot(QueryBuilders.termsQuery(search.getName(), search.getValues()));
                break;
            }
            case IS_NULL:
                isNullSearch(search, queryBuilder);
                break;
            case IS_NOT_NULL:
                isNotNullSearch(search, queryBuilder);
                break;
        }
    }

    /**
     * 数字类型的es搜索
     *
     * @param search       搜索条件
     * @param queryBuilder 查询器
     */
    public static void numberSearch(CrmSearchBO.Search search, BoolQueryBuilder queryBuilder) {
        if (search.getValues().size() == 0 && !Arrays.asList(5, 6).contains(search.getSearchEnum().getType())) {
            return;
        }
        switch (search.getSearchEnum()) {
            case IS:
                queryBuilder.filter(QueryBuilders.termQuery(search.getName(), search.getValues().get(0)));
                break;
            case IS_NOT:
                queryBuilder.mustNot(QueryBuilders.termQuery(search.getName(), search.getValues().get(0)));
                break;
            case GT: {
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(search.getName());
                rangeQuery.gt(search.getValues().get(0));
                queryBuilder.filter(rangeQuery);
                break;
            }
            case EGT: {
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(search.getName());
                rangeQuery.gte(search.getValues().get(0));
                queryBuilder.filter(rangeQuery);
                break;
            }
            case LT: {
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(search.getName());
                rangeQuery.lt(search.getValues().get(0));
                queryBuilder.filter(rangeQuery);
                break;
            }
            case ELT: {
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(search.getName());
                rangeQuery.lte(search.getValues().get(0));
                queryBuilder.filter(rangeQuery);
                break;
            }
            case RANGE: {
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(search.getName());
                rangeQuery.gte(search.getValues().get(0));
                rangeQuery.lte(search.getValues().get(1));
                queryBuilder.filter(rangeQuery);
                break;
            }
            case IS_NULL:
                isNullSearch(search, queryBuilder);
                break;
            case IS_NOT_NULL:
                isNotNullSearch(search, queryBuilder);
                break;
        }
    }

    /**
     * 时间类型的es搜索
     *
     * @param search       搜索条件
     * @param queryBuilder 查询器
     */
    public static void dateSearch(CrmSearchBO.Search search, BoolQueryBuilder queryBuilder, FieldEnum fieldEnum) {
        List<String> values = search.getValues();
        if (values.size() == 0) {
            throw new CrmException(SystemCodeEnum.SYSTEM_NO_VALID);
        }
        switch (search.getSearchEnum()) {
            case IS: {
                queryBuilder.filter(QueryBuilders.termQuery(search.getName(), search.getValues().get(0)));
                break;
            }
            case IS_NOT: {
                queryBuilder.mustNot(QueryBuilders.termQuery(search.getName(), search.getValues().get(0)));
                break;
            }
            case IS_NULL:
                isNullSearch(search, queryBuilder);
                break;
            case IS_NOT_NULL:
                isNotNullSearch(search, queryBuilder);
                break;
            case GT: {
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(search.getName());
                rangeQuery.gt(search.getValues().get(0));
                queryBuilder.filter(rangeQuery);
                break;
            }
            case EGT: {
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(search.getName());
                rangeQuery.gte(search.getValues().get(0));
                queryBuilder.filter(rangeQuery);
                break;
            }
            case LT: {
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(search.getName());
                rangeQuery.lt(search.getValues().get(0));
                queryBuilder.filter(rangeQuery);
                break;
            }
            case ELT: {
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(search.getName());
                rangeQuery.lte(search.getValues().get(0));
                queryBuilder.filter(rangeQuery);
                break;
            }
            case RANGE: {
                BiParams biParams = new BiParams();
                if (search.getValues().size() > 1) {
                    biParams.setStartTime(values.get(0));
                    biParams.setEndTime(values.get(1));
                } else {
                    biParams.setType(search.getValues().get(0));
                }
                BiTimeUtil.BiTimeEntity timeEntity = BiTimeUtil.analyzeTime(biParams);
                Date beginDate = timeEntity.getBeginDate();
                Date endDate = timeEntity.getEndDate();
                RangeQueryBuilder builder = QueryBuilders.rangeQuery(search.getName());
                builder.gte(fieldEnum == FieldEnum.DATETIME ? DateUtil.formatDateTime(beginDate) : DateUtil.formatDate(beginDate));
                builder.lte(fieldEnum == FieldEnum.DATETIME ? DateUtil.formatDateTime(endDate) : DateUtil.formatDate(endDate));
                queryBuilder.filter(builder);
                break;
            }
        }
    }


    /**
     * 搜索用户信息
     *
     * @param search       搜索条件
     * @param queryBuilder 查询器
     */
    public static void userSearch(CrmSearchBO.Search search, BoolQueryBuilder queryBuilder) {
        switch (search.getSearchEnum()) {
            case CONTAINS:
                queryBuilder.filter(QueryBuilders.termsQuery(search.getName(), search.getValues()));
                break;
            case NOT_CONTAINS:
                queryBuilder.mustNot(QueryBuilders.termsQuery(search.getName(), search.getValues()));
                break;
            case IS_NULL:
                isNullSearch(search, queryBuilder);
                break;
            case IS_NOT_NULL:
                isNotNullSearch(search, queryBuilder);
                break;
        }
    }

    /**
     * 为空搜索
     *
     * @param search       搜索条件
     * @param queryBuilder 查询器
     */
    private static void isNullSearch(CrmSearchBO.Search search, BoolQueryBuilder queryBuilder) {
        FieldEnum fieldEnum = FieldEnum.parse(search.getFormType());
        if (Arrays.asList(FieldEnum.DATETIME, FieldEnum.DATE, FieldEnum.NUMBER, FieldEnum.FLOATNUMBER).contains(fieldEnum)) {
            queryBuilder.mustNot(QueryBuilders.existsQuery(search.getName()));
        } else {
            BoolQueryBuilder builder = QueryBuilders.boolQuery();
            builder.should(QueryBuilders.termQuery(search.getName(), ""));
            builder.should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(search.getName())));
            queryBuilder.filter(builder);
        }
    }

    /**
     * 不为空搜索
     *
     * @param search       搜索条件
     * @param queryBuilder 查询器
     */
    private static void isNotNullSearch(CrmSearchBO.Search search, BoolQueryBuilder queryBuilder) {
        queryBuilder.filter(QueryBuilders.existsQuery(search.getName()));
        FieldEnum fieldEnum = FieldEnum.parse(search.getFormType());
        if (!Arrays.asList(FieldEnum.DATETIME, FieldEnum.DATE, FieldEnum.NUMBER, FieldEnum.FLOATNUMBER).contains(fieldEnum)) {
            queryBuilder.mustNot(QueryBuilders.termQuery(search.getName(), ""));
        }
    }
}
