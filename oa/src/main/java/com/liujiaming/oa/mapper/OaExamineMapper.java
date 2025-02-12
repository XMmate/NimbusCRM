package com.liujiaming.oa.mapper;

import com.alibaba.fastjson.JSONObject;
import com.liujiaming.core.entity.BasePage;
import com.liujiaming.core.servlet.BaseMapper;
import com.liujiaming.oa.entity.BO.ExamineExportBO;
import com.liujiaming.oa.entity.BO.ExaminePageBO;
import com.liujiaming.oa.entity.PO.OaExamine;
import com.liujiaming.oa.entity.PO.OaExamineField;
import com.liujiaming.oa.entity.VO.ExamineVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 审批表 Mapper 接口
 * </p>
 *
 * @author liujiaming
 * @since 2024-05-15
 */
public interface OaExamineMapper extends BaseMapper<OaExamine> {

    BasePage<ExamineVO> myInitiate(BasePage<Object> parse, @Param("data") ExaminePageBO examinePageBO, @Param("userId") Long userId, @Param("isAdmin") boolean isAdmin, @Param("biStatus") Integer biStatus);

    List<Long> queryExamineUserByExamineLog(@Param("record") ExamineVO record);

    BasePage<ExamineVO> myOaExamine(BasePage<Object> parse, @Param("data") ExaminePageBO examinePageBO, @Param("userId") Long userId, @Param("isAdmin") boolean isAdmin);

    ExamineVO queryExamineById(String id);

    JSONObject queryExamineRecordById(String recordId);

    List<JSONObject> myInitiateExcel(@Param("data") ExamineExportBO examineExportBO, @Param("userId") Long userId, @Param("isAdmin") boolean isAdmin);

    List<JSONObject> myOaExamineExcel(@Param("data") ExamineExportBO examineExportBO, @Param("userId") Long userId, @Param("isAdmin") boolean isAdmin);

    List<JSONObject> queryTravelExamineList(@Param("examineIdList") List<Integer> examineIdList);

    List<JSONObject> queryCustomExamineList(@Param("examineIdList") List<Integer> examineIdList,@Param("batchIds") List<String> batchIdList, @Param("fieldMap") List<OaExamineField> fields);

    List<JSONObject> queryExamineList(@Param("examineIdList") List<Integer> examineIdList);


    BasePage<ExamineVO> myInitiateOaExamine(BasePage<Object> parse, @Param("data") ExaminePageBO examinePageBO, @Param("userId") Long userId, @Param("isAdmin") boolean isAdmin);

    List<JSONObject> myInitiateOaExcel(@Param("data") ExamineExportBO examineExportBO, @Param("userId") Long userId, @Param("isAdmin") boolean isAdmin);


}
