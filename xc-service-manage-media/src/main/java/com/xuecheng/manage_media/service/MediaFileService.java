package com.xuecheng.manage_media.service;

import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.request.QueryMediaFileRequest;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class MediaFileService {
    @Autowired
    MediaFileRepository mediaFileRepository;
    public QueryResponseResult<MediaFile> findList( int page, int size, QueryMediaFileRequest queryMediaFileRequest) {
        if (queryMediaFileRequest==null){
            queryMediaFileRequest=new QueryMediaFileRequest();
        }

        MediaFile mediaFile=new MediaFile();
        if (!StringUtils.isEmpty(queryMediaFileRequest.getTag())){
            mediaFile.setTag(queryMediaFileRequest.getTag());
        }
        if (!StringUtils.isEmpty(queryMediaFileRequest.getFileOriginalName())){
            mediaFile.setFileOriginalName(queryMediaFileRequest.getFileOriginalName());
        }
        if (!StringUtils.isEmpty(queryMediaFileRequest.getProcessStatus())){
            mediaFile.setProcessStatus(queryMediaFileRequest.getProcessStatus());
        }
        ExampleMatcher exampleMatcher=ExampleMatcher.matching()
                .withMatcher("tag",ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher("fileOriginalName",ExampleMatcher.GenericPropertyMatchers.contains());
        Example<MediaFile> example=Example.of(mediaFile,exampleMatcher);
        if (page<=0){
            page=1;
        }
        page=page-1;
        if (size<=0){
            size=10;
        }
        Pageable pageable=new PageRequest(page,size);

        Page<MediaFile> all = mediaFileRepository.findAll(example, pageable);
        long totalElements = all.getTotalElements();
        List<MediaFile> content = all.getContent();
        QueryResult<MediaFile> queryResult=new QueryResult();
        queryResult.setTotal(totalElements);
        queryResult.setList(content);
        QueryResponseResult queryResponseResult=new QueryResponseResult(CommonCode.SUCCESS,queryResult);
        return queryResponseResult;
    }
}
