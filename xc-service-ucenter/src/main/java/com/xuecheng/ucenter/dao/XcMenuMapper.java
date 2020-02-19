package com.xuecheng.ucenter.dao;

import com.xuecheng.framework.domain.ucenter.XcMenu;

import java.util.List;

public interface XcMenuMapper {
    public List<XcMenu> selectPermissionByUserId(String userid);
}
