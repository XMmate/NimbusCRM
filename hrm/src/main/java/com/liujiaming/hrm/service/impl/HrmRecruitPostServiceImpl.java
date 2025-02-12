package com.liujiaming.hrm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.liujiaming.core.entity.BasePage;
import com.liujiaming.core.feign.admin.service.AdminService;
import com.liujiaming.core.servlet.BaseServiceImpl;
import com.liujiaming.hrm.constant.HrmActionBehaviorEnum;
import com.liujiaming.hrm.constant.MenuIdConstant;
import com.liujiaming.hrm.entity.BO.QueryRecruitPostPageListBO;
import com.liujiaming.hrm.entity.BO.UpdateRecruitPostStatusBO;
import com.liujiaming.hrm.entity.PO.HrmDept;
import com.liujiaming.hrm.entity.PO.HrmRecruitPost;
import com.liujiaming.hrm.entity.VO.RecruitPostVO;
import com.liujiaming.hrm.mapper.HrmRecruitPostMapper;
import com.liujiaming.hrm.service.IHrmDeptService;
import com.liujiaming.hrm.service.IHrmRecruitPostService;
import com.liujiaming.hrm.service.actionrecord.impl.RecruitPostActionRecordServiceImpl;
import com.liujiaming.hrm.utils.EmployeeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 招聘职位表 服务实现类
 * </p>
 *
 * @author huangmingbo
 * @since 2024-05-12
 */
@Service
public class HrmRecruitPostServiceImpl extends BaseServiceImpl<HrmRecruitPostMapper, HrmRecruitPost> implements IHrmRecruitPostService {

    @Autowired
    private HrmRecruitPostMapper recruitPostMapper;

    @Resource
    private RecruitPostActionRecordServiceImpl recruitPostActionRecordService;

    @Autowired
    private IHrmDeptService deptService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private EmployeeUtil employeeUtil;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRecruitPost(HrmRecruitPost recruitPost) {
        save(recruitPost);
        recruitPostActionRecordService.addOrDeleteRecord(HrmActionBehaviorEnum.ADD, recruitPost.getPostId(), recruitPost.getPostName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setRecruitPost(HrmRecruitPost recruitPost) {
        recruitPostActionRecordService.entityUpdateRecord(BeanUtil.beanToMap(getById(recruitPost.getPostId())), BeanUtil.beanToMap(recruitPost), recruitPost.getPostId());
        baseMapper.setRecruitPost(recruitPost);
    }

    @Override
    public RecruitPostVO queryById(Integer postId) {
        return recruitPostMapper.queryById(postId);
    }

    @Override
    public BasePage<RecruitPostVO> queryRecruitPostPageList(QueryRecruitPostPageListBO queryRecruitPostPageListBO) {
        Collection<Integer> deptIds = employeeUtil.queryDataAuthDeptIdByMenuId(MenuIdConstant.RECRUIT_MENU_ID);
        return recruitPostMapper.queryRecruitPostPageList(queryRecruitPostPageListBO.parse(), queryRecruitPostPageListBO, deptIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRecruitPostStatus(UpdateRecruitPostStatusBO updateRecruitPostStatusBO) {
        recruitPostActionRecordService.updateStatusRecord(updateRecruitPostStatusBO);
        lambdaUpdate().set(HrmRecruitPost::getStatus, updateRecruitPostStatusBO.getStatus())
                .eq(HrmRecruitPost::getPostId, updateRecruitPostStatusBO.getPostId()).update();
    }

    @Override
    public Map<Integer, Long> queryPostStatusNum() {
        Collection<Integer> deptIds = employeeUtil.queryDataAuthDeptIdByMenuId(MenuIdConstant.RECRUIT_MENU_ID);
        List<Integer> statusList = getBaseMapper().queryPostStatusList(deptIds);
        TreeMap<Integer, Long> collect = statusList.stream().collect(Collectors.groupingBy(e->e, TreeMap::new, Collectors.counting()));
        for (Integer i : Arrays.asList(0, 1)) {
            if (!collect.containsKey(i)) {
                collect.put(i, 0L);
            }
        }
        return collect;
    }

    @Override
    public List<HrmRecruitPost> queryAllRecruitPostList() {
        List<HrmRecruitPost> list = lambdaQuery().select(HrmRecruitPost::getPostId, HrmRecruitPost::getPostName, HrmRecruitPost::getDeptId)
                .eq(HrmRecruitPost::getStatus, 1).list();
        for (HrmRecruitPost post : list) {
            HrmDept dept = deptService.getById(post.getDeptId());
            if (dept != null) {
                post.setDeptName(dept.getName());
            }
        }
        return list;
    }
}
