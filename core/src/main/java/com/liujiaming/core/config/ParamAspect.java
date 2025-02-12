package com.liujiaming.core.config;

import cn.hutool.core.util.StrUtil;
import com.alicp.jetcache.Cache;
import com.alicp.jetcache.anno.CreateCache;
import com.liujiaming.core.common.Const;
import com.liujiaming.core.entity.UserExtraInfo;
import com.liujiaming.core.entity.UserInfo;
import com.liujiaming.core.exception.NoLoginException;
import com.liujiaming.core.feign.admin.service.AdminService;
import com.liujiaming.core.redis.service.Redis;
import com.liujiaming.core.servlet.LoginFromCookie;
import com.liujiaming.core.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @author liujiaming
 * user注入切面 把用户信息保存到线程变量里面去
 */
@Aspect
@Component
@Slf4j
public class ParamAspect implements Ordered {

    @Autowired
    private Redis redis;

    @CreateCache(name = Const.COMPANY_MANAGE_CACHE_NAME, expire = 3, timeUnit = TimeUnit.DAYS)
    private Cache<Long, Boolean> companyCache;

    @Autowired
    private AdminService adminService;

    /**所有访问controller层的方法都会被在这里拦截校验token
     * @Aspect：该注解表明这是一个切面类，用于拦截特定的方法调用并在方法前后执行一些逻
     * @param point
     * @return
     * @throws Throwable
     */
    @Around("(execution(* com.liujiaming.*.controller..*(..))||execution(* com.liujiaming.*.*.controller..*(..))) && execution(@(org.springframework.web.bind.annotation.*Mapping) * *(..))  && !execution(@(com.liujiaming.core.common.ParamAspect) * *(..))")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        ServletRequestAttributes attributes = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());
        HttpServletRequest request = attributes.getRequest();
        try {
            String token = request.getHeader(Const.TOKEN_NAME);
            if (StrUtil.isEmpty(token)) {
                if (point instanceof MethodInvocationProceedingJoinPoint) {
                    Method targetMethod = ((MethodSignature) point.getSignature()).getMethod();
                    LoginFromCookie fromCookie = targetMethod.getAnnotation(LoginFromCookie.class);
                    if (fromCookie != null) {
                        for (Cookie cookie : request.getCookies()) {
                            if (Const.TOKEN_NAME.equals(cookie.getName())) {
                                token = cookie.getValue();
                                break;
                            }
                        }
                    }
                }
            }
            UserInfo info = null;
            if (StrUtil.isNotEmpty(token)) {
                Object data = redis.get(token);
                if (data instanceof UserExtraInfo) {
                    throw new NoLoginException((UserExtraInfo) data);
                } else if (data instanceof UserInfo) {
                    info = (UserInfo) data;
                }
                if (info != null) {
                    info.setRequest(request);
                    info.setResponse(attributes.getResponse());
                    UserUtil.setUser(info);
                    UserUtil.userExpire(token);
                }
            }
            if (info == null) {
                //TODO 未登录时有个空的user对象
                info = new UserInfo();
                info.setRequest(request);
                info.setResponse(attributes.getResponse());
                UserUtil.setUser(info);
            }
            return point.proceed();
        } finally {
            UserUtil.removeUser();
        }
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
