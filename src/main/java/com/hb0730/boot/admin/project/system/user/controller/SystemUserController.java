package com.hb0730.boot.admin.project.system.user.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.hb0730.boot.admin.commons.annotation.Log;
import com.hb0730.boot.admin.commons.constant.BusinessTypeEnum;
import com.hb0730.boot.admin.commons.constant.ModuleName;
import com.hb0730.boot.admin.commons.constant.RequestMappingNameConstants;
import com.hb0730.boot.admin.commons.utils.PageInfoUtil;
import com.hb0730.boot.admin.commons.utils.bean.BeanUtils;
import com.hb0730.boot.admin.commons.utils.spring.SecurityUtils;
import com.hb0730.boot.admin.commons.web.controller.BaseController;
import com.hb0730.boot.admin.commons.web.response.ResponseResult;
import com.hb0730.boot.admin.commons.web.response.Result;
import com.hb0730.boot.admin.project.system.user.model.entity.SystemUserEntity;
import com.hb0730.boot.admin.project.system.user.model.vo.SystemUserVO;
import com.hb0730.boot.admin.project.system.user.model.vo.UserParamsVO;
import com.hb0730.boot.admin.project.system.user.model.vo.UserVO;
import com.hb0730.boot.admin.project.system.user.service.ISystemUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 系统用户  前端控制器
 * </p>
 *
 * @author bing_huang
 * @since 2020-03-24
 */
@RestController
@RequestMapping(RequestMappingNameConstants.REQUEST_USER)
public class SystemUserController extends BaseController {
    @Autowired
    private ISystemUserService systemUserService;

    /**
     * <p>
     * 用户新增
     * </p>
     *
     * @param vo 用户信息
     * @return 是否成功
     */
    @Deprecated
    public Result save(SystemUserVO vo) {
        if (Objects.isNull(vo)) {
            return ResponseResult.resultFall("新增用户失败，用户账号为空");
        }
        SystemUserEntity entity = BeanUtils.transformFrom(vo, SystemUserEntity.class);
        systemUserService.save(entity);
        return ResponseResult.resultSuccess("保存成功");
    }

    /**
     * 用户新增
     *
     * @param vo 用户信息
     * @return 是否成功
     */
    @PostMapping("/save")
    @Log(paramsName = {"vo"}, module = ModuleName.USER, title = "用户保存", businessType = BusinessTypeEnum.INSERT)
    public Result save(@Validated @RequestBody UserVO vo) {
        if (Objects.isNull(vo)) {
            return ResponseResult.resultFall("新增用户失败，用户账号为空");
        }
        systemUserService.save(vo);
        return ResponseResult.resultSuccess("保存成功");
    }

    /**
     * <p>
     * 根据用户账号查找用户信息
     * </p>
     *
     * @param username 用户账号
     * @return 用户信息
     */
    public Result<SystemUserVO> loadUserByUsername(String username) {
        SystemUserEntity entity = new SystemUserEntity();
        entity.setUsername(username);
        QueryWrapper<SystemUserEntity> queryWrapper = new QueryWrapper<>(entity);
        entity = systemUserService.getOne(queryWrapper);
        SystemUserVO userVO = BeanUtils.transformFrom(entity, SystemUserVO.class);
        return ResponseResult.resultSuccess(userVO);
    }

    /**
     * <p>
     * 根据用户id获取用户信息
     * </p>
     *
     * @param id 用户id
     * @return 用户信息
     */
    @GetMapping("/info/{id}")
    public Result getUserInfoById(@PathVariable Long id) {
        SystemUserEntity entity = systemUserService.getById(id);
        SystemUserVO vo = BeanUtils.transformFrom(entity, SystemUserVO.class);
        return ResponseResult.resultSuccess(vo);
    }

    /**
     * <p>
     * 根据用户id修改用户信息
     * </p>
     *
     * @param vo 需要被修改的信息(不包含用户密码,用户组织,用户岗位和用户角色)
     * @param id 用户id
     * @return 用户信息
     */
    @PostMapping("/update/info/{id}")
    @Deprecated
    public Result updateInfoByUserId(@RequestBody SystemUserVO vo, @PathVariable Long id) {
        SystemUserEntity entity = systemUserService.getById(id);
        vo.setPassword(null);
        vo.setDeptId(null);
        BeanUtils.updateProperties(vo, entity);
        systemUserService.updateById(entity);
        return ResponseResult.resultSuccess("修改成功");
    }

    /**
     * 根据用户id修改用户密码
     *
     * @param oldPassword  原密码
     * @param newPassword  新密码
     * @param newPassword2 二次验证
     * @param id           用户id
     * @return 是否成功
     */
    @PostMapping("/update/password/{id}")
    @Log(paramsName = {"oldPassword", "newPassword", "newPassword2"}, module = ModuleName.USER, title = "用户修改密码", businessType = BusinessTypeEnum.UPDATE)
    public Result updatePasswordByUserId(String oldPassword, String newPassword, String newPassword2, @PathVariable Long id) {
        if (StringUtils.isBlank(oldPassword)) {
            return ResponseResult.resultFall("原密码为空");
        }
        SystemUserEntity entity = systemUserService.getById(id);
        if (!SecurityUtils.matchesPassword(oldPassword, entity.getPassword())) {
            return ResponseResult.resultFall("原密码不正确");
        }
        if (StringUtils.isBlank(newPassword)) {
            return ResponseResult.resultFall("新密码为空");
        }
        if (!newPassword.equals(newPassword2)) {
            return ResponseResult.resultFall("两次输入密码不一致");
        }
        String encryptNewPassword = SecurityUtils.encryptPassword(newPassword2);
        entity = new SystemUserEntity();
        entity.setPassword(encryptNewPassword);
        entity.setId(id);
        systemUserService.updateById(entity);
        return ResponseResult.resultSuccess("修改成功");
    }

    /**
     * <p>
     * 分页用户
     * </p>
     *
     * @param page     页数
     * @param pageSize 数量
     * @param vo       过滤条件
     * @return 分页后的用户信息
     */
    @PostMapping("/all/{page}/{pageSize}")
    @PreAuthorize("hasAnyAuthority('user:query','ROLE_ADMIN','ROLE_USER')")
    public Result getUserPage(@PathVariable Integer page, @PathVariable Integer pageSize, @RequestBody UserParamsVO vo) {
        QueryWrapper<SystemUserEntity> queryWrapper = new QueryWrapper<>();
        if (!Objects.isNull(vo)) {
            Long deptId = vo.getDeptId();
            if (!Objects.isNull(deptId)) {
                queryWrapper.eq(SystemUserEntity.DEPTID, deptId);
            }
            if (StringUtils.isNotBlank(vo.getNickName())) {
                queryWrapper.eq(SystemUserEntity.NICK_NAME, vo.getNickName());
            }
            if (StringUtils.isNotBlank(vo.getUsername())) {
                queryWrapper.eq(SystemUserEntity.USERNAME, vo.getUsername());
            }
            if (Objects.nonNull(vo.getIsEnabled())) {
                queryWrapper.eq(SystemUserEntity.IS_ENABLED, vo.getIsEnabled());
            }
        }
        PageHelper.startPage(page, pageSize);
        List<SystemUserEntity> entities = systemUserService.list(queryWrapper);
        PageInfo<SystemUserEntity> pageInfo = new PageInfo<>(entities);
        PageInfo<SystemUserVO> info = PageInfoUtil.toBean(pageInfo, SystemUserVO.class);
        return ResponseResult.resultSuccess(info);
    }


    /**
     * <p>
     * 获取用户详情(包含用户角色岗位)
     * </p>
     *
     * @param userId 用户id
     * @return 用户详情
     */
    @GetMapping("/user/info/{userId}")
    public Result getUserInfo(@PathVariable Long userId) {
        UserVO info = systemUserService.getUserInfo(userId);
        return ResponseResult.resultSuccess(info);
    }

    /**
     * 更新用户信息
     *
     * @param user   用户信息(用户角色与用户岗位)
     * @param userId 用户id
     * @return 是否成功
     */
    @PostMapping("/update/user/{userId}")
    @Log(paramsName = {"user"}, module = ModuleName.USER, title = "更新用户信息", businessType = BusinessTypeEnum.UPDATE)
    public Result updateUserById(@RequestBody UserVO user, @PathVariable Long userId) {
        if (Objects.isNull(user)) {
            return ResponseResult.resultSuccess("修改成功");
        }
        systemUserService.updateUser(user, userId);
        return ResponseResult.resultSuccess("修改成功");
    }

    /**
     * <p>
     * 重置密码
     * </p>
     *
     * @param id 用户id
     * @return 是否成功
     */
    @GetMapping("/update/reset/password/{id}")
    @Log(module = ModuleName.USER, title = "重置密码", businessType = BusinessTypeEnum.UPDATE)
    public Result resetPassword(@PathVariable Long id) {
        systemUserService.resetPassword(id);
        return ResponseResult.resultSuccess("重置成功");
    }


    /**
     * <p>
     * 删除
     * </p>
     *
     * @param id 是否成功
     * @return 是否成功
     */
    @GetMapping("/delete/{id}")
    @Log(module = ModuleName.USER, title = "删除用户", businessType = BusinessTypeEnum.DELETE)
    public Result deleteById(@PathVariable Long id) {
        systemUserService.removeById(id);
        return ResponseResult.resultSuccess("删除成功");
    }

    /**
     * <p>
     * 删除用户
     * </p>
     *
     * @param ids id
     * @return 是否成功
     */
    @PostMapping("/delete")
    @Log(module = ModuleName.USER, title = "删除用户", businessType = BusinessTypeEnum.DELETE)
    public Result deleteByIds(@RequestBody List<Long> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            systemUserService.removeByIds(ids);
            return ResponseResult.resultSuccess("删除成功");
        }
        return ResponseResult.resultFall("请选择");
    }
}
