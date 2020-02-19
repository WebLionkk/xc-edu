package com.xuecheng.manage_course.service;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.netflix.discovery.converters.Auto;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.domain.course.*;
import com.xuecheng.framework.domain.course.ext.CategoryNode;
import com.xuecheng.framework.domain.course.ext.CourseInfo;
import com.xuecheng.framework.domain.course.ext.CourseView;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.domain.course.response.AddCourseResult;
import com.xuecheng.framework.domain.course.response.CourseCode;
import com.xuecheng.framework.domain.course.response.CoursePublishResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.*;
import com.xuecheng.manage_course.client.CmsPageClient;
import com.xuecheng.manage_course.dao.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {
    @Autowired
    TeachplanMapper teachplanMapper;
    @Autowired
    CourseBaseRepository courseBaseRepository;
    @Autowired
    TeachplanRepository teachplanRepository;
    @Autowired
    CourseMapper courseMapper;
    @Autowired
    CourseMarketRepository courseMarketRepository;
    @Autowired
    CoursePicRepository coursePicRepository;
    @Autowired
    CmsPageClient cmsPageClient;
    @Autowired
    CoursePubRepository coursePubRepository;
    @Autowired
    TeachplanMediaRepository teachplanMediaRepository;
    @Autowired
    TeachplanMediaPubRepository teachplanMediaPubRepository;

    @Value("${course-publish.dataUrlPre}")
    private String publish_dataUrlPre;
    @Value("${course-publish.pagePhysicalPath}")
    private String publish_page_physicalpath;
    @Value("${course-publish.pageWebPath}")
    private String publish_page_webpath;
    @Value("${course-publish.siteId}")
    private String publish_siteId;
    @Value("${course-publish.templateId}")
    private String publish_templateId;
    @Value("${course-publish.previewUrl}")
    private String previewUrl;

    public TeachplanNode findTeachplanNodeList(String courseId) {
        return teachplanMapper.selectList(courseId);
    }

    @Transactional
    public ResponseResult addTeachplan(Teachplan teachplan) {
        if (teachplan == null || StringUtils.isEmpty(teachplan.getCourseid()) || StringUtils.isEmpty(teachplan.getPname())) {
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        String courseid = teachplan.getCourseid();
        String parentid = teachplan.getParentid();
        if (StringUtils.isEmpty(parentid)){
            parentid=this.getTeachplanRoot(courseid);
        }
        Optional<Teachplan> optional = teachplanRepository.findById(parentid);
        Teachplan teachplan2 = optional.get();
        String grade = teachplan2.getGrade();
        Teachplan teachplan1=new Teachplan();
        BeanUtils.copyProperties(teachplan,teachplan1);
        teachplan1.setCourseid(courseid);
        teachplan1.setParentid(parentid);
        if (grade.equals("1")){
            teachplan1.setGrade("2");
        }else {
            teachplan1.setGrade("3");
        }
        teachplanRepository.save(teachplan1);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    private String getTeachplanRoot(String courseId){
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if (!optional.isPresent()){
            return null;
        }
        CourseBase courseBase = optional.get();
        List<Teachplan> list = teachplanRepository.findByCourseidAndAndParentid(courseId, "0");
        if (list==null||list.size()<=0){
            Teachplan teachplan=new Teachplan();
            teachplan.setParentid("0");
            teachplan.setGrade("1");
            teachplan.setPname(courseBase.getName());
            teachplan.setCourseid(courseId);
            teachplan.setStatus("0");
            teachplanRepository.save(teachplan);
            return teachplan.getId();
        }
        return list.get(0).getId();
    }

    public QueryResponseResult<CourseInfo> findCourseList(String company_id,int page, int size, CourseListRequest courseListRequest) {
        if (courseListRequest==null){
            courseListRequest=new CourseListRequest();
        }
        courseListRequest.setCompanyId(company_id);
        if (page<=0){
            page=0;
        }
        if (size<=0){
            size=20;
        }
        PageHelper.startPage(page,size);
        Page<CourseInfo> courseListPage = courseMapper.findCourseListPage(courseListRequest);
        List<CourseInfo> result = courseListPage.getResult();
        long total = courseListPage.getTotal();
        QueryResult<CourseInfo> queryResult=new QueryResult<>();
        queryResult.setList(result);
        queryResult.setTotal(total);
        return new QueryResponseResult<CourseInfo>(CommonCode.SUCCESS,queryResult);
    }

    public AddCourseResult addCourseBase(CourseBase courseBase) {
        courseBase.setStatus("202001");
        courseBaseRepository.save(courseBase);
        return new AddCourseResult(CommonCode.SUCCESS,courseBase.getId());
    }

    public CourseBase getCourseBaseById(String courseId) throws RuntimeException {
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if (optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    public ResponseResult updateCourseBase(String id, CourseBase courseBase) {
        CourseBase one = this.getCourseBaseById(id);
        if (one==null){
            ExceptionCast.cast(CommonCode.FAIL);
        }
        one.setName(courseBase.getName());
        one.setMt(courseBase.getMt());
        one.setSt(courseBase.getSt());
        one.setGrade(courseBase.getGrade());
        one.setStudymodel(courseBase.getStudymodel());
        one.setUsers(courseBase.getUsers());
        one.setDescription(courseBase.getDescription());
        CourseBase save = courseBaseRepository.save(one);
        return new ResponseResult(CommonCode.SUCCESS);
    }
    public CourseMarket getCourseMarketById(String courseId) {
        Optional<CourseMarket> optional = courseMarketRepository.findById(courseId);
        if (optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    public CourseMarket updateCourseMarket(String id, CourseMarket courseMarket) {
        CourseMarket one = this.getCourseMarketById(id);
        if (one!=null){
            one.setCharge(courseMarket.getCharge());
            one.setStartTime(courseMarket.getStartTime());//课程有效期，开始时间
            one.setEndTime(courseMarket.getEndTime());//课程有效期，结束时间
            one.setPrice(courseMarket.getPrice());
            one.setQq(courseMarket.getQq());
            one.setValid(courseMarket.getValid());
            courseMarketRepository.save(one);
        }else {
            one=new CourseMarket();
            BeanUtils.copyProperties(courseMarket,one);
            one.setId(id);
            courseMarketRepository.save(one);
        }
        return one;
    }

    @Transactional
    public ResponseResult addCoursePic(String courseId, String pic) {
        CoursePic coursePic=null;
        Optional<CoursePic> optional = coursePicRepository.findById(courseId);
        if (optional.isPresent()){
            coursePic = optional.get();
        }
        if (coursePic==null) {
            coursePic = new CoursePic();
        }
        coursePic.setCourseid(courseId);
        coursePic.setPic(pic);
        coursePicRepository.save(coursePic);
        return new ResponseResult(CommonCode.SUCCESS);
    }
    public CoursePic findCoursepic(String courseId) {
        Optional<CoursePic> optional = coursePicRepository.findById(courseId);
        if (optional.isPresent()){
            CoursePic coursePic = optional.get();
            return coursePic;
        }
        return null;
    }
    @Transactional
    public ResponseResult deleteCoursePic(String courseId) {
        long result = coursePicRepository.deleteByCourseid(courseId);
        if (result>0){
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }

    public CourseView courseview(String id) {
        CourseView courseView=new CourseView();
        Optional<CourseBase> courseBaseOptional = courseBaseRepository.findById(id);
        if (courseBaseOptional.isPresent()){
            courseView.setCourseBase(courseBaseOptional.get());
        }
        Optional<CoursePic> coursePicOptional = coursePicRepository.findById(id);
        if (coursePicOptional.isPresent()){
            courseView.setCoursePic(coursePicOptional.get());
        }
        Optional<CourseMarket> courseMarketOptional = courseMarketRepository.findById(id);
        if (courseMarketOptional.isPresent()){
            courseView.setCourseMarket(courseMarketOptional.get());
        }
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        courseView.setTeachplanNode(teachplanNode);

        return courseView;
    }

    public CoursePublishResult preview(String id) {
        CmsPage cmsPage=new CmsPage();
        CourseBase one = this.findCourseBaseById(id);
        //站点
        cmsPage.setSiteId(publish_siteId);//课程预览站点
        //模板
        cmsPage.setTemplateId(publish_templateId);
        //页面名称
        cmsPage.setPageName(id+".html");
        //页面别名
        cmsPage.setPageAliase(one.getName());
        //页面访问路径
        cmsPage.setPageWebPath(publish_page_webpath);
        //页面存储路径
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);
        //数据url
        cmsPage.setDataUrl(publish_dataUrlPre+id);
        //远程请求cms保存页面信息
        CmsPageResult cmsPageResult = cmsPageClient.save(cmsPage);
        if (!cmsPageResult.isSuccess()){
            return new CoursePublishResult(CommonCode.FAIL,null);
        }
        CmsPage cmsPage1 = cmsPageResult.getCmsPage();
        String pageId = cmsPage1.getPageId();
        String pageurl=previewUrl+pageId;
        return new CoursePublishResult(CommonCode.SUCCESS,pageurl);
    }

    //根据id查询课程基本信息
    public CourseBase findCourseBaseById(String courseId){
        Optional<CourseBase> baseOptional = courseBaseRepository.findById(courseId);
        if(baseOptional.isPresent()){
            CourseBase courseBase = baseOptional.get();
            return courseBase;
        }
        ExceptionCast.cast(CourseCode.COURSE_GET_NOTEXISTS);
        return null;
    }

    @Transactional
    public CoursePublishResult publish(String id) {
        CmsPage cmsPage=new CmsPage();
        CourseBase one = this.findCourseBaseById(id);
        //站点
        cmsPage.setSiteId(publish_siteId);//课程预览站点
        //模板
        cmsPage.setTemplateId(publish_templateId);
        //页面名称
        cmsPage.setPageName(id+".html");
        //页面别名
        cmsPage.setPageAliase(one.getName());
        //页面访问路径
        cmsPage.setPageWebPath(publish_page_webpath);
        //页面存储路径
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);
        //数据url
        cmsPage.setDataUrl(publish_dataUrlPre+id);

        CmsPostPageResult cmsPostPageResult = cmsPageClient.postPageQuick(cmsPage);
        if (!cmsPostPageResult.isSuccess()){
            return new CoursePublishResult(CommonCode.FAIL,null);
        }
        CourseBase courseBase = this.saveCoursePubState(id);
        if (courseBase==null){
            return new CoursePublishResult(CommonCode.FAIL,null);
        }

        CoursePub coursePub = createCoursePub(id);
        saveCoursePub(id,coursePub);
        String pageUrl = cmsPostPageResult.getPageUrl();

        saveTeachplanMediaPub(id);
        return new CoursePublishResult(CommonCode.SUCCESS,pageUrl);
    }

    private void saveTeachplanMediaPub(String courseId){
            teachplanMediaPubRepository.deleteByCourseId(courseId);
        List<TeachplanMedia> teachplanMedialist = teachplanMediaRepository.findByCourseId(courseId);
        ArrayList<TeachplanMediaPub> teachplanMediaPubs=new ArrayList<>();
        for (TeachplanMedia teachplanMedia:teachplanMedialist){
            TeachplanMediaPub teachplanMediaPub=new TeachplanMediaPub();
            BeanUtils.copyProperties(teachplanMedia,teachplanMediaPub);
            teachplanMediaPub.setTimestamp(new Date());
            teachplanMediaPubs.add(teachplanMediaPub);
        }
        teachplanMediaPubRepository.saveAll(teachplanMediaPubs);
    }

    private CoursePub saveCoursePub(String id,CoursePub coursePub){
        CoursePub coursePubNew=null;
        Optional<CoursePub> optionalCoursePub = coursePubRepository.findById(id);
        if (optionalCoursePub.isPresent()){
            coursePubNew = optionalCoursePub.get();
        }else {
            coursePubNew=new CoursePub();
        }
        BeanUtils.copyProperties(coursePub,coursePubNew);
        coursePubNew.setId(id);
        coursePubNew.setTimestamp(new Date());
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        String format = simpleDateFormat.format(new Date());
        coursePubNew.setPubTime(format);
        coursePubRepository.save(coursePubNew);
        return coursePubNew;
    }
    private CoursePub createCoursePub(String id){
        CoursePub coursePub=new CoursePub();
        Optional<CourseBase> optionalCourseBase = courseBaseRepository.findById(id);
        if (optionalCourseBase.isPresent()){
            CourseBase courseBase = optionalCourseBase.get();
            BeanUtils.copyProperties(courseBase,coursePub);
        }

        Optional<CoursePic> optionalCoursePic = coursePicRepository.findById(id);
        if (optionalCoursePic.isPresent()){
            CoursePic coursePic = optionalCoursePic.get();
            BeanUtils.copyProperties(coursePic,coursePub);
        }

        Optional<CourseMarket> optionalCourseMarket = courseMarketRepository.findById(id);
        if (optionalCourseMarket.isPresent()){
            CourseMarket courseMarket = optionalCourseMarket.get();
            BeanUtils.copyProperties(courseMarket,coursePub);
        }

        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        String jsonString = JSON.toJSONString(teachplanNode);
        coursePub.setTeachplan(jsonString);
        return coursePub;
    }

    private CourseBase saveCoursePubState(String courseID){
        CourseBase courseBase = this.findCourseBaseById(courseID);
        courseBase.setStatus("202002");
        courseBaseRepository.save(courseBase);
        return courseBase;
    }

    public ResponseResult savemedia(TeachplanMedia teachplanMedia) {
        if (teachplanMedia==null|| org.apache.commons.lang.StringUtils.isEmpty(teachplanMedia.getTeachplanId())){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        String teachplanId=teachplanMedia.getTeachplanId();
        Optional<Teachplan> optionalTeachplan = teachplanRepository.findById(teachplanId);
        if (!optionalTeachplan.isPresent()){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        Teachplan teachplan = optionalTeachplan.get();
        String grade = teachplan.getGrade();
        if (org.apache.commons.lang.StringUtils.isEmpty(grade)||!grade.equals("3")){
            ExceptionCast.cast(CourseCode.COURSE_MEDIA_TEACHPLAN_GRADEERROR);
        }
        TeachplanMedia one=null;
        Optional<TeachplanMedia> optionalTeachplanMedia = teachplanMediaRepository.findById(teachplanId);
        if (optionalTeachplanMedia.isPresent()){
            one = optionalTeachplanMedia.get();
        }else {
            one=new TeachplanMedia();
        }
        one.setTeachplanId(teachplanId);
        one.setCourseId(teachplanMedia.getCourseId());
        one.setMediaFileOriginalName(teachplanMedia.getMediaFileOriginalName());
        one.setMediaId(teachplanMedia.getMediaId());
        one.setMediaUrl(teachplanMedia.getMediaUrl());
        teachplanMediaRepository.save(one);
        return new ResponseResult(CommonCode.SUCCESS);
    }
}
