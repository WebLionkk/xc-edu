package com.xuecheng.api.learning;

import com.xuecheng.framework.domain.learning.GetMediaResult;
import io.swagger.annotations.ApiOperation;

public interface CourseLearningControllerApi {
    @ApiOperation("获取课程学习地址")
    public GetMediaResult getmedia(String courseId, String teachplanId);
}
