package com.xuecheng.manage_cms.service;

import com.alibaba.fastjson.JSON;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.netflix.discovery.converters.Auto;
import com.xuecheng.framework.domain.cms.CmsConfig;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsSite;
import com.xuecheng.framework.domain.cms.CmsTemplate;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.exception.ExceptionCatch;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_cms.config.RabbitmqConfig;
import com.xuecheng.manage_cms.dao.CmsConfigRepository;
import com.xuecheng.manage_cms.dao.CmsPageRepository;
import com.xuecheng.manage_cms.dao.CmsSiteRepository;
import com.xuecheng.manage_cms.dao.CmsTemplateRepository;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PageService {
    @Autowired
    CmsPageRepository cmsPageRepository;
    @Autowired
    CmsConfigRepository cmsConfigRepository;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    CmsTemplateRepository cmsTemplateRepository;
    @Autowired
    GridFSBucket gridFSBucket;
    @Autowired
    GridFsTemplate gridFsTemplate;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    CmsSiteRepository cmsSiteRepository;
    public QueryResponseResult findList(@PathVariable("page") int page, @PathVariable("size") int size, QueryPageRequest queryPageRequest) {

        if (queryPageRequest==null){
            queryPageRequest=new QueryPageRequest();
        }
        CmsPage cmsPage=new CmsPage();
        if (!StringUtils.isEmpty(queryPageRequest.getSiteId())){
            cmsPage.setSiteId(queryPageRequest.getSiteId());
        }
        if (!StringUtils.isEmpty(queryPageRequest.getTemplateId())){
            cmsPage.setTemplateId(queryPageRequest.getTemplateId());
        }
        if (!StringUtils.isEmpty(queryPageRequest.getPageAliase())){
            cmsPage.setPageAliase(queryPageRequest.getPageAliase());
        }
        ExampleMatcher exampleMatcher=ExampleMatcher.matching();
        exampleMatcher=exampleMatcher.withMatcher("pageAliase",ExampleMatcher.GenericPropertyMatchers.contains());
        Example<CmsPage> example=Example.of(cmsPage,exampleMatcher);
        if (page<=0){
            page=1;
        }
        page=page-1;
        if (size<=0){
            size=10;
        }
        Pageable pageable=PageRequest.of(page,size);
        Page<CmsPage> all = cmsPageRepository.findAll(example,pageable);
        QueryResult queryResult=new QueryResult();
        queryResult.setList(all.getContent());
        queryResult.setTotal(all.getTotalElements());
        QueryResponseResult queryResponseResult=new QueryResponseResult(CommonCode.SUCCESS,queryResult);
        return queryResponseResult;
    }

    public CmsPageResult add(CmsPage cmsPage) {
        CmsPage cmsPage1 = cmsPageRepository.findByPageNameAndSiteIdAndPageWebPath(cmsPage.getPageName(), cmsPage.getSiteId(), cmsPage.getPageWebPath());
        if (cmsPage1!=null){
            ExceptionCast.cast(CmsCode.CMS_ADDPAGE_EXISTSNAME);
        }
        cmsPage.setPageId(null);
        cmsPageRepository.save(cmsPage);
        return new CmsPageResult(CommonCode.SUCCESS,cmsPage);
    }

    public CmsPage findById(String id) {
        Optional<CmsPage> optional = cmsPageRepository.findById(id);
        if (optional.isPresent()){
            return optional.get();
        }
        return null;
    }


    public CmsPageResult edit(String id, CmsPage cmsPage) {
        CmsPage one = this.findById(id);
        if (one !=null){
            one.setTemplateId(cmsPage.getTemplateId());
//更新所属站点
            one.setSiteId(cmsPage.getSiteId());
//更新页面别名
            one.setPageAliase(cmsPage.getPageAliase());
//更新页面名称
            one.setPageName(cmsPage.getPageName());
//更新访问路径
            one.setPageWebPath(cmsPage.getPageWebPath());
//更新物理路径
            one.setPagePhysicalPath(cmsPage.getPagePhysicalPath());

            one.setDataUrl(cmsPage.getDataUrl());
//执行更新
            CmsPage save = cmsPageRepository.save(one);
            return new CmsPageResult(CommonCode.SUCCESS,save);
        }
        return new CmsPageResult(CommonCode.FAIL,null);
    }

    public ResponseResult delete(String id) {
        Optional<CmsPage> optional = cmsPageRepository.findById(id);
        if (optional.isPresent()) {
            cmsPageRepository.deleteById(id);
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }

    public CmsConfig getConfigByID(String id){
        Optional<CmsConfig> optional = cmsConfigRepository.findById(id);
        if (optional.isPresent()){
            CmsConfig cmsConfig = optional.get();
            return cmsConfig;
        }
        return null;
    }

    public String getPageHtml(String pageId){
        Map modelByPageID = getModelByPageID(pageId);
        if (modelByPageID==null){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAISNULL);
        }
        String templateByPageID = getTemplateByPageID(pageId);
        if (StringUtils.isEmpty(templateByPageID)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        String content = generateHtml(templateByPageID, modelByPageID);
        if (StringUtils.isEmpty(content)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_HTMLISNULL);
        }
        return content;
    }

    private String generateHtml(String template,Map model){
        Configuration configuration=new Configuration(Configuration.getVersion());
        StringTemplateLoader templateLoader=new StringTemplateLoader();
        templateLoader.putTemplate("template",template);
        configuration.setTemplateLoader(templateLoader);
        try {
            Template template1 = configuration.getTemplate("template");
            String content = FreeMarkerTemplateUtils.processTemplateIntoString(template1, model);
            return content;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private String getTemplateByPageID(String pageID){
        CmsPage cmsPage = this.findById(pageID);
        if (cmsPage==null){
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        String templateId = cmsPage.getTemplateId();
        if (StringUtils.isEmpty(templateId)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        Optional<CmsTemplate> optional = cmsTemplateRepository.findById(templateId);
        if (optional.isPresent()){
            CmsTemplate template = optional.get();
            String templateFileId = template.getTemplateFileId();
            GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(templateFileId)));
            GridFSDownloadStream gridFSDownloadStream = gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
            GridFsResource gridFsResource=new GridFsResource(gridFSFile,gridFSDownloadStream);
            try {
                String content = IOUtils.toString(gridFsResource.getInputStream(), "utf-8");
                return content;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    private Map getModelByPageID(String pageID){
        CmsPage cmsPage = this.findById(pageID);
        if (cmsPage==null){
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        String dataUrl = cmsPage.getDataUrl();
        if (StringUtils.isEmpty(dataUrl)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAISNULL);
        }
        ResponseEntity<Map> forEntity = restTemplate.getForEntity(dataUrl, Map.class);
        Map body = forEntity.getBody();
        return body;

    }

    public ResponseResult post(String pageID){
        String pageHtml = this.getPageHtml(pageID);
        CmsPage cmsPage = saveHTML(pageID, pageHtml);
        this.sendPoostPage(pageID);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    private void sendPoostPage(String pageId){
        Map<String,String> map=new HashMap<>();
        map.put("pageId",pageId);
        String jsonString = JSON.toJSONString(map);
        CmsPage cmsPage = this.findById(pageId);
        if (cmsPage==null){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        String siteId = cmsPage.getSiteId();
        rabbitTemplate.convertAndSend(RabbitmqConfig.EX_ROUTING_CMS_POSTPAGE,siteId,jsonString);

    }
    private CmsPage saveHTML(String pageId,String content){
        CmsPage cmsPage = this.findById(pageId);
        if (cmsPage==null){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        ObjectId objectId =null;
        try {
            InputStream inputStream = IOUtils.toInputStream(content, "utf-8");
            objectId=gridFsTemplate.store(inputStream, cmsPage.getPageName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        cmsPage.setHtmlFileId(objectId.toHexString());
        cmsPageRepository.save(cmsPage);
        return cmsPage;
    }
//402885816243d2dd016243f24c030002
    public CmsPageResult save(CmsPage cmsPage) {
        CmsPage one = cmsPageRepository.findByPageNameAndSiteIdAndPageWebPath(cmsPage.getPageName(), cmsPage.getSiteId(), cmsPage.getPageWebPath());
        if (one!=null){
            return this.edit(one.getPageId(),cmsPage);
        }
        return this.add(cmsPage);
    }

    public CmsPostPageResult postPageQuick(CmsPage cmsPage) {
        CmsPageResult pageResult = this.save(cmsPage);
        if (!pageResult.isSuccess()){
            ExceptionCast.cast(CommonCode.FAIL);
        }
        CmsPage cmsPagesave = pageResult.getCmsPage();
        String pageId = cmsPagesave.getPageId();
        ResponseResult post = this.post(pageId);
        if (!post.isSuccess()){
            ExceptionCast.cast(CommonCode.FAIL);
        }
        String siteId = cmsPagesave.getSiteId();
        CmsSite cmsSiteById = this.findCmsSiteById(siteId);
        String pageUrl = cmsSiteById.getSiteDomain() + cmsSiteById.getSiteWebPath() + cmsPagesave.getPageWebPath() + cmsPagesave.getPageName();
        return new CmsPostPageResult(CommonCode.SUCCESS,pageUrl);
    }

    private CmsSite findCmsSiteById(String siteId){
        Optional<CmsSite> optionalSite = cmsSiteRepository.findById(siteId);
        if (optionalSite.isPresent()){
            CmsSite cmsSite = optionalSite.get();
            return cmsSite;
        }
        return null;
    }
}
