package com.xuecheng.manage_cms_client.service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsSite;
import com.xuecheng.manage_cms_client.dao.CmsPageRepository;
import com.xuecheng.manage_cms_client.dao.CmsSiteRepository;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Optional;


@Service
public class PageService {

    private static  final Logger LOGGER = LoggerFactory.getLogger(PageService.class);

    @Autowired
    GridFsTemplate gridFsTemplate;

    @Autowired
    GridFSBucket gridFSBucket;

    @Autowired
    CmsPageRepository cmsPageRepository;

    @Autowired
    CmsSiteRepository cmsSiteRepository;

    //保存html页面到服务器物理路径
    public void savePageToServerPath(String pageId){
        //根据pageId查询cmsPage
        CmsPage cms = this.findPageById(pageId);
        String htmlFileId = cms.getHtmlFileId();
        InputStream inputStream = this.getFileById(htmlFileId);
        if (inputStream==null){
            LOGGER.error("getFileById InputStream is null,htmlFileId:{}"+htmlFileId);
            return;
        }
        CmsSite site = this.findSiteById(cms.getSiteId());
        String sitePhysicalPath = site.getSitePhysicalPath();
        String path=sitePhysicalPath+cms.getPagePhysicalPath()+cms.getPageName();
        FileOutputStream fileOutputStream=null;
        try {
            fileOutputStream=new FileOutputStream(new File(path));
            IOUtils.copy(inputStream,fileOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //根据文件id从GridFS中查询文件内容
    public InputStream getFileById(String fileId){
        GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(fileId)));
        GridFSDownloadStream gridFSDownloadStream = gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
        GridFsResource gridFsResource=new GridFsResource(gridFSFile,gridFSDownloadStream);
        try {
            return gridFsResource.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    //根据页面id查询页面信息

    public CmsPage findPageById(String id){
        Optional<CmsPage> optionalCmsPage = cmsPageRepository.findById(id);
        if (optionalCmsPage.isPresent()){
            CmsPage cmsPage = optionalCmsPage.get();
            return cmsPage;
        }
        return null;
    }
    //根据站点id查询站点信息
    public CmsSite findSiteById(String id){
        Optional<CmsSite> optionalcmsSite = cmsSiteRepository.findById(id);
        if (optionalcmsSite.isPresent()){
            CmsSite cmsSite = optionalcmsSite.get();
            return cmsSite;
        }
        return null;
    }
}
