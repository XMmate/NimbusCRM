package com.liujiaming.authorization.service;

import com.liujiaming.authorization.entity.AdminUserStatusBO;
import com.liujiaming.authorization.service.Fallback.AdminUserServiceFallback;
import com.liujiaming.core.common.Result;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author zhang
 * 向admin模块请求企业信息
 */
@Component
@FeignClient(name = "admin",fallbackFactory= AdminUserServiceFallback.class)
public interface AdminUserService {

    /**
     * 通过用户名查询用户
     *
     * @param username 用户名
     * @return 结果信息
     */
    @RequestMapping(value = "/adminUser/findByUsername")
    Result findByUsername(@RequestParam("username") String username);

    /**
     * 通过用户ID查询所属角色
     * @param userId 用户ID
     * @return data
     */
    @RequestMapping(value = "/adminUser/queryUserRoleIds")
    Result<List<Integer>> queryUserRoleIds(@RequestParam("userId") Long userId);

    @PostMapping("/adminUser/setUserStatus")
    @ApiOperation("禁用启用")
    Result setUserStatus(@RequestBody AdminUserStatusBO adminUserStatusBO);

    @PostMapping("/adminUser/activateUser")
    @ApiOperation("激活账号")
    Result activateUser(@RequestBody AdminUserStatusBO adminUserStatusBO);


    /**
    * 通过用户ID查询没有权限的菜单
    * @date 2024/8/20 15:25
    * @param userId
    * @return 结果信息
    **/
    @PostMapping(value = "/adminRole/queryNoAuthMenu")
    Result<List<String>> queryNoAuthMenu(@RequestParam("userId") Long userId);
}
