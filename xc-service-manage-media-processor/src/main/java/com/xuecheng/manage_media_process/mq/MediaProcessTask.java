package com.xuecheng.manage_media_process.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.MediaFileProcess_m3u8;
import com.xuecheng.framework.utils.HlsVideoUtil;
import com.xuecheng.framework.utils.Mp4VideoUtil;
import com.xuecheng.manage_media_process.dao.MediaFileRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MediaProcessTask {

    @Autowired
    MediaFileRepository mediaFileRepository;
    @Value("${xc-service-manage-media.ffmpeg-path}")
    String ffmpeg_path;
    @Value("${xc-service-manage-media.video-location}")
    String video_location;
    @RabbitListener(queues = {"${xc-service-manage-media.mq.queue-media-video-processor}"},containerFactory="customContainerFactory")
    public void receiveMediaProcessTask(String msg){
        Map map = JSON.parseObject(msg, Map.class);
        String mediaId= (String) map.get("mediaId");
        Optional<MediaFile> optionalMediaFile = mediaFileRepository.findById(mediaId);
        if (!optionalMediaFile.isPresent()){
            return;
        }
        MediaFile mediaFile = optionalMediaFile.get();
        String fileType = mediaFile.getFileType();
        if (!fileType.equals("avi")){
            mediaFile.setProcessStatus("303004");
            mediaFileRepository.save(mediaFile);
            return;
        }else {
            mediaFile.setProcessStatus("303001");
            mediaFileRepository.save(mediaFile);
        }
        String video_path=video_location+mediaFile.getFilePath()+mediaFile.getFileName();
        String mp4_name=mediaFile.getFileId()+".mp4";
        String mp4_location=video_location+mediaFile.getFilePath();
        Mp4VideoUtil mp4VideoUtil=new Mp4VideoUtil(ffmpeg_path,video_path,mp4_name,mp4_location);
        String result = mp4VideoUtil.generateMp4();
        if (result==null||!result.equals("success")){
            mediaFile.setProcessStatus("303003");
            MediaFileProcess_m3u8 mediaFileProcess_m3u8=new MediaFileProcess_m3u8();
            mediaFileProcess_m3u8.setErrormsg(result);
            mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
            mediaFileRepository.save(mediaFile);
            return;
        }
        String mp4_video_path=video_location+mediaFile.getFilePath()+mp4_name;
        String m3u8_name=mediaFile.getFileId()+".m3u8";
        String m3u8folder_path=video_location+mediaFile.getFilePath()+"hls/";
        HlsVideoUtil hlsVideoUtil=new HlsVideoUtil(ffmpeg_path,mp4_video_path,m3u8_name,m3u8folder_path);
        String generateM3u8 = hlsVideoUtil.generateM3u8();
        if (generateM3u8==null||!generateM3u8.equals("success")){
            mediaFile.setProcessStatus("303003");
            MediaFileProcess_m3u8 mediaFileProcess_m3u8=new MediaFileProcess_m3u8();
            mediaFileProcess_m3u8.setErrormsg(generateM3u8);
            mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
            mediaFileRepository.save(mediaFile);
            return;
        }
        List<String> ts_list = hlsVideoUtil.get_ts_list();
        mediaFile.setProcessStatus("303002");
        MediaFileProcess_m3u8 mediaFileProcess_m3u8=new MediaFileProcess_m3u8();
        mediaFileProcess_m3u8.setTslist(ts_list);
        mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
        String fileUrl=mediaFile.getFilePath()+"hls/"+m3u8_name;
        mediaFile.setFileUrl(fileUrl);
        mediaFileRepository.save(mediaFile);

    }
}
