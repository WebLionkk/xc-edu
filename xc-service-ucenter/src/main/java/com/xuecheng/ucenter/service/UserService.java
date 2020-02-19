package com.xuecheng.ucenter.service;

import com.netflix.discovery.converters.Auto;
import com.xuecheng.framework.domain.ucenter.XcCompanyUser;
import com.xuecheng.framework.domain.ucenter.XcMenu;
import com.xuecheng.framework.domain.ucenter.XcUser;
import com.xuecheng.framework.domain.ucenter.ext.XcUserExt;
import com.xuecheng.ucenter.dao.XcCompanyUserRepository;
import com.xuecheng.ucenter.dao.XcMenuMapper;
import com.xuecheng.ucenter.dao.XcUserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    @Autowired
    XcUserRepository xcUserRepository;
    @Autowired
    XcCompanyUserRepository xcCompanyUserRepository;
    @Autowired
    XcMenuMapper xcMenuMapper;
    public XcUser findXcUserByUsername(String username){
        return xcUserRepository.findByUsername(username);
    }
    public XcUserExt getUserExt(String username){
        XcUser xcUser = this.findXcUserByUsername(username);
        if (xcUser==null){
            return null;
        }
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(xcUser.getId());
        XcCompanyUser xcCompanyUser = xcCompanyUserRepository.findByUserId(xcUser.getId());
        String companyId = null;
        if (xcCompanyUser!=null){
            companyId=xcCompanyUser.getCompanyId();
        }
        XcUserExt xcUserExt= new XcUserExt();
        BeanUtils.copyProperties(xcUser,xcUserExt);
        xcUserExt.setCompanyId(companyId);
        xcUserExt.setPermissions(xcMenus);
        return xcUserExt;
    }
}
