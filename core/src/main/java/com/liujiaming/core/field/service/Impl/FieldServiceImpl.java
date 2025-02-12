package com.liujiaming.core.field.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liujiaming.core.common.Const;
import com.liujiaming.core.common.FieldEnum;
import com.liujiaming.core.feign.admin.service.AdminFileService;
import com.liujiaming.core.feign.admin.service.AdminService;
import com.liujiaming.core.feign.crm.entity.CrmFieldPatch;
import com.liujiaming.core.field.service.FieldService;
import com.liujiaming.core.servlet.ApplicationContextHolder;
import com.liujiaming.core.utils.TagUtil;
import com.liujiaming.core.utils.UserCacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author liujiaming
 * @date 2021/1/13
 */
@Service
public class FieldServiceImpl implements FieldService {

    private static final String NUM_RESTRICT_REGEX = "^[\\-]?\\d+|[\\-]?\\d+\\.\\d*|[\\-]?\\d*\\.\\d+$";

    private static final FieldEnum[] DEFAULT_FIELD_ENUMS = {FieldEnum.AREA,FieldEnum.AREA_POSITION,FieldEnum.CURRENT_POSITION,FieldEnum.DETAIL_TABLE};

    @Autowired
    private AdminService adminService;

    @Override
    public Object convertValueByFormType(Object value, FieldEnum typeEnum){
        Object newValue = null;
        switch (typeEnum) {
            case USER:
                newValue = UserCacheUtil.getSimpleUsers((TagUtil.toLongSet(value.toString())));
                break;
            case STRUCTURE:
                newValue = adminService.queryDeptByIds(TagUtil.toSet(value.toString())).getData();
                break;
            case FILE:
                newValue = ApplicationContextHolder.getBean(AdminFileService.class).queryFileList(value.toString()).getData();
                break;
            case HANDWRITING_SIGN:
                newValue = value.toString();
                break;
            case SINGLE_USER:
                newValue = UserCacheUtil.getSimpleUser((Long) value);
                break;
            case AREA:
            case CURRENT_POSITION:
            case AREA_POSITION:
                String valueStr = Optional.ofNullable(value).orElse("").toString();
                newValue = JSON.parse(valueStr);
                break;
            case DETAIL_TABLE:
                newValue = handleDetailTableData(value);
                break;
            case DATE_INTERVAL:
                String dateIntervalStr = Optional.ofNullable(value).orElse("").toString();
                newValue = StrUtil.split(dateIntervalStr, Const.SEPARATOR);
                break;
            default:
                newValue = value;
                break;
        }
        return newValue;
    }



    /**
     * 处理明细表格数据
     * @date 2021/3/5 14:38
     * @param value
     * @return java.util.List<java.util.List<com.liujiaming.core.feign.crm.entity.CrmFieldPatch>>
     **/
    @SuppressWarnings("unchecked")
    private List<List<CrmFieldPatch>> handleDetailTableData(Object value){
        List<List<CrmFieldPatch>> dtDataList = new ArrayList<>();
        String dtStr = Optional.ofNullable(value).orElse("").toString();
        List<JSONArray> jsonArrayList = JSON.parseObject(dtStr,List.class);
        jsonArrayList.forEach(jsonArray ->{
            dtDataList.add(jsonArray.toJavaList(CrmFieldPatch.class));
        });
        for (List<CrmFieldPatch> crmFieldPatchList : dtDataList) {
            for (CrmFieldPatch crmFieldPatch : crmFieldPatchList) {
                FieldEnum fieldEnum = FieldEnum.parse(crmFieldPatch.getType());
                Object valueData = crmFieldPatch.getValue();
                if (ObjectUtil.isEmpty(valueData)){
                    continue;
                }
                switch (fieldEnum) {
                    case CHECKBOX:
                        if (valueData instanceof String) {
                            crmFieldPatch.setValue(StrUtil.split(valueData.toString(), Const.SEPARATOR));
                        }
                        break;
                    case USER:
                        crmFieldPatch.setValue(UserCacheUtil.getSimpleUsers(TagUtil.toLongSet(valueData.toString())));
                        break;
                    case STRUCTURE:
                        crmFieldPatch.setValue(adminService.queryDeptByIds(TagUtil.toSet(valueData.toString())).getData());
                        break;
                    case FILE:
                        crmFieldPatch.setValue(ApplicationContextHolder.getBean(AdminFileService.class).queryFileList(valueData.toString()).getData());
                        break;
                    case SINGLE_USER:
                        crmFieldPatch.setValue(UserCacheUtil.getSimpleUser((Long) value));
                        break;
                    default:
                        break;
                }
            }
        }
        return dtDataList;
    }
    
    @Override
    public <T> List<List<T>> convertFormPositionFieldList(List<T> fieldList, Function<T,Integer> groupMapper,
                                                          Function<T,Integer> sortMapper,Function<T,Integer> defaultSortMapper){
        List<List<T>> list = new ArrayList<>();
        Map<Integer, List<T>> crmFieldGroupMap = fieldList.stream().collect(Collectors.groupingBy(groupMapper));
        if (crmFieldGroupMap.size() > 0) {
            Map<Integer, List<T>> resultMap = new LinkedHashMap<>();
            crmFieldGroupMap.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEachOrdered(e -> resultMap.put(e.getKey(), e.getValue()));

            resultMap.forEach((key, value) -> {
                if (key != -1) {
                    value.sort(Comparator.comparing(sortMapper));
                    list.add(value);
                }
            });
            List<T> crmFields = resultMap.get(-1);
            if (CollUtil.isNotEmpty(crmFields)) {
                crmFields.sort(Comparator.comparing(defaultSortMapper));
                int size = crmFields.size();
                boolean isWithResidue = size % 2 != 0;
                List<T> temporaryList = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    T crmField = crmFields.get(i);
                    temporaryList.add(crmField);
                    if (i % 2 == 1) {
                        list.add(temporaryList);
                        temporaryList = new ArrayList<>();
                        continue;
                    }
                    if (isWithResidue && i == size - 1) {
                        list.add(temporaryList);
                    }
                }
            }
        }
        return list;
    }


    @Override
    public boolean verifyStrForNumRestrict(String maxNumRestrict, String minNumRestrict) {
        if (StrUtil.isNotEmpty(maxNumRestrict) || StrUtil.isNotEmpty(minNumRestrict)){
            if (StrUtil.isNotEmpty(maxNumRestrict) && StrUtil.isEmpty(minNumRestrict)){
                return maxNumRestrict.matches(NUM_RESTRICT_REGEX);
            }else if (StrUtil.isEmpty(maxNumRestrict) && StrUtil.isNotEmpty(minNumRestrict)){
                return minNumRestrict.matches(NUM_RESTRICT_REGEX);
            }else {
                if (maxNumRestrict.matches(NUM_RESTRICT_REGEX) && minNumRestrict.matches(NUM_RESTRICT_REGEX)){
                    return Double.parseDouble(maxNumRestrict) > Double.parseDouble(minNumRestrict);
                }
                return false;
            }
        }
        return true;
    }



    @Override
    public String convertObjectValueToString(Integer type, Object value, String defaultValue){
        boolean isNeedHandle = equalsByType(type);
        if (isNeedHandle) {
            if (value instanceof JSONObject) {
                return !ObjectUtil.isEmpty(value) ? ((JSONObject) value).toJSONString() : "";
            }
            return !ObjectUtil.isEmpty(value) ? JSON.toJSONString(value) : "";
        }
        if (FieldEnum.DATE_INTERVAL.getType().equals(type)){
            if (value instanceof JSONObject){
                value = !ObjectUtil.isEmpty(value) ? JSON.parseObject(((JSONObject)value).toJSONString(),List.class) : null;
            }
            return !ObjectUtil.isEmpty(value) ? StrUtil.join(Const.SEPARATOR, value) : "";
        }
        return defaultValue;
    }

    @Override
    public boolean equalsByType(Object type) {
        return equalsByType(type,DEFAULT_FIELD_ENUMS);
    }

    @Override
    public boolean equalsByType(Object type, FieldEnum... fieldEnums){
        if (type instanceof String){
            for (FieldEnum anEnum : fieldEnums) {
                if(anEnum.getFormType().equals(type)){
                    return true;
                }
            }
        }else {
            for (FieldEnum anEnum : fieldEnums) {
                if (Objects.equals(anEnum.getType(),type)) {
                    return true;
                }
            }
        }
        return false;
    }
}

