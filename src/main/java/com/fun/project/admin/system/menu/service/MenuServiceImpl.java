package com.fun.project.admin.system.menu.service;

import com.fun.common.utils.StringUtils;
import com.fun.common.utils.TreeUtils;
import com.fun.framework.shiro.ShiroUtils;
import com.fun.framework.web.entity.Ztree;
import com.fun.project.admin.system.menu.entity.Menu;
import com.fun.project.admin.system.menu.mapper.MenuMapper;
import com.fun.project.admin.system.role.entity.Role;
import com.fun.project.admin.system.role.mapper.RoleMenuMapper;
import com.fun.project.admin.system.user.entity.AdminUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.*;

/**
 * created by DJun on 2019/9/14 20:45
 * desc:
 */
@Service
public class MenuServiceImpl implements IMenuService {
    public static final String PREMISSION_STRING = "perms[\"{0}\"]";
    @Autowired
    private MenuMapper menuMapper;
    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Override
    public List<Menu> selectMenusByUser(AdminUser user) {
        List<Menu> menus = null;
        // 管理员显示所有菜单信息
        if (user.isAdmin()) {
            menus = menuMapper.selectMenuNormalAll();
        } else {
            menus = menuMapper.selectMenusByUserId(user.getUserId());
        }
        return TreeUtils.getChildPerms(menus, 0);
    }

    @Override
    public List<Menu> selectMenuList(Menu menu) {
        List<Menu> menuList;
        AdminUser user = ShiroUtils.getSysUser();
        if (user.isAdmin()) {
            menuList = menuMapper.selectMenuList(menu);
        } else {
            menu.getParams().put("userId", user.getUserId());
            menuList = menuMapper.selectMenuListByUserId(menu);
        }
        return menuList;
    }

    @Override
    public List<Menu> selectMenuAll() {
        List<Menu> menuList;
        AdminUser user = ShiroUtils.getSysUser();
        if (user.isAdmin()) {
            menuList = menuMapper.selectMenuAll();
        } else {
            menuList = menuMapper.selectMenuAllByUserId(user.getUserId());
        }
        return menuList;
    }

    @Override
    public Set<String> selectPermsByUserId(Long userId) {
        List<String> perms = menuMapper.selectPermsByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (String perm : perms) {
            if (StringUtils.isNotEmpty(perm)) {
                permsSet.addAll(Arrays.asList(perm.trim().split(",")));
            }
        }
        return permsSet;
    }

    @Override
    public List<Ztree> roleMenuTreeData(Role role) {
        Long roleId = role.getRoleId();
        List<Ztree> ztrees;
        List<Menu> menuList = selectMenuAll();
        if (StringUtils.isNotNull(roleId)) {
            List<String> roleMenuList = menuMapper.selectMenuTree(roleId);
            ztrees = initZtree(menuList, roleMenuList, true);
        } else {
            ztrees = initZtree(menuList, null, true);
        }
        return ztrees;
    }


    /**
     * 查询所有菜单
     *
     * @return 菜单列表
     */
    @Override
    public List<Ztree> menuTreeData() {
        List<Menu> menuList = selectMenuAll();
        return initZtree(menuList);
    }

    /**
     * 查询系统所有权限
     *
     * @return 权限列表
     */
    @Override
    public Map<String, String> selectPermsAll() {
        LinkedHashMap<String, String> section = new LinkedHashMap<>();
        List<Menu> permissions = selectMenuAll();
        if (StringUtils.isNotEmpty(permissions)) {
            for (Menu menu : permissions) {
                section.put(menu.getUrl(), MessageFormat.format(PREMISSION_STRING, menu.getPerms()));
            }
        }
        return section;
    }

    /**
     * 对象转菜单树
     *
     * @param menuList 菜单列表
     * @return 树结构列表
     */
    public List<Ztree> initZtree(List<Menu> menuList) {
        return initZtree(menuList, null, false);
    }

    /**
     * 对象转菜单树
     *
     * @param menuList     菜单列表
     * @param roleMenuList 角色已存在菜单列表
     * @param permsFlag    是否需要显示权限标识
     * @return 树结构列表
     */
    public List<Ztree> initZtree(List<Menu> menuList, List<String> roleMenuList, boolean permsFlag) {
        List<Ztree> ztrees = new ArrayList<>();
        boolean isCheck = StringUtils.isNotNull(roleMenuList);
        for (Menu menu : menuList) {
            Ztree ztree = new Ztree();
            ztree.setId(menu.getMenuId());
            ztree.setPId(menu.getParentId());
            ztree.setName(transMenuName(menu, permsFlag));
            ztree.setTitle(menu.getMenuName());
            if (isCheck) {
                ztree.setChecked(roleMenuList.contains(menu.getMenuId() + menu.getPerms()));
            }
            ztrees.add(ztree);
        }
        return ztrees;
    }

    public String transMenuName(Menu menu, boolean permsFlag) {
        StringBuilder sb = new StringBuilder();
        sb.append(menu.getMenuName());
        if (permsFlag) {
            sb.append("<font color=\"#888\">&nbsp;&nbsp;&nbsp;").append(menu.getPerms()).append("</font>");
        }
        return sb.toString();
    }

    @Override
    public int deleteMenuById(Long menuId) {
        ShiroUtils.clearCachedAuthorizationInfo();
        return menuMapper.deleteMenuById(menuId);
    }

    @Override
    public Menu selectMenuById(Long menuId) {
        return menuMapper.selectMenuById(menuId);
    }

    /**
     * 查询parentId 下面的子菜单数量
     *
     * @param parentId pid
     * @return 结果
     */
    @Override
    public int selectCountMenuByParentId(Long parentId) {
        return menuMapper.selectCountMenuByParentId(parentId);
    }

    @Override
    public int selectCountRoleMenuByMenuId(Long menuId) {
        return roleMenuMapper.selectCountRoleMenuByMenuId(menuId);
    }

    /**
     * 新增保存菜单信息
     */
    @Override
    public int insertMenu(Menu menu) {
        menu.setCreateBy(ShiroUtils.getLoginName());
        menu.setCreateTime(System.currentTimeMillis());
        ShiroUtils.clearCachedAuthorizationInfo();
        return menuMapper.insertMenu(menu);
    }

    @Override
    public int updateMenu(Menu menu) {
        menu.setUpdateBy(ShiroUtils.getLoginName());
        menu.setUpdateTime(System.currentTimeMillis());
        ShiroUtils.clearCachedAuthorizationInfo();
        return menuMapper.updateMenu(menu);
    }

    @Override
    public int checkMenuNameUnique(Menu menu) {
        Menu info = menuMapper.checkMenuNameUnique(menu.getMenuName(), menu.getParentId());
        if (StringUtils.isNull(info))
            return 0;
        else
            return 1;
    }
}
