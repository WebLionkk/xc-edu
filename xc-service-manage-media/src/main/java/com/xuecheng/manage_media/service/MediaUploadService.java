package com.xuecheng.manage_media.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.response.CheckChunkResult;
import com.xuecheng.framework.domain.media.response.MediaCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_media.config.RabbitMQConfig;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sun.misc.IOUtils;

import java.io.*;
import java.util.*;

@Service
public class MediaUploadService {
    @Autowired
    MediaFileRepository mediaFileRepository;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Value("${xc-service-manage-media.upload-location}")
    String upload_location;
    @Value(("${xc-service-manage-media.mq.routingkey-media-video}"))
    String routingkey_medi_video;
    private String getFileFolderPath(String fileMd5){
        return upload_location+fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/";
    }
    private String getFilePath(String fileMd5,String fileExt){
        return upload_location+fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/"+fileMd5+"."+fileExt;
    }
    private String getChunkFileFolderPath(String fileMd5){
        return upload_location+fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/chunk/";
    }
    public ResponseResult register(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        String fileFolderPath = this.getFileFolderPath(fileMd5);
        String filePath = this.getFilePath(fileMd5, fileExt);
        File file=new File(filePath);
        boolean exists = file.exists();

        Optional<MediaFile> optionalMediaFile = mediaFileRepository.findById(fileMd5);
        if (exists&&optionalMediaFile.isPresent()){
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_EXIST);
        }
        File fileFolder = new File(fileFolderPath);
        if (!fileFolder.exists()){
            fileFolder.mkdirs();
        }

        return new ResponseResult(CommonCode.SUCCESS);
    }

    public CheckChunkResult checkchunk(String fileMd5, Integer chunk, Integer chunkSize) {
        String chunkFileFolderPath = this.getChunkFileFolderPath(fileMd5);
        File file = new File(chunkFileFolderPath + chunk);
        if (file.exists()){
            return new CheckChunkResult(CommonCode.SUCCESS,true);
        }else {
            return new CheckChunkResult(CommonCode.SUCCESS,false);
        }
    }

    public ResponseResult uploadchunk(MultipartFile file, Integer chunk, String fileMd5) {
        String chunkFileFolderPath = this.getChunkFileFolderPath(fileMd5);
        File chunkFileFolder = new File(chunkFileFolderPath);
        if (!chunkFileFolder.exists()){
            chunkFileFolder.mkdirs();
        }
        InputStream inputStream=null;
        OutputStream outputStream=null;
        try {
            inputStream=file.getInputStream();
            outputStream=new FileOutputStream(new File(chunkFileFolderPath+chunk));
            org.apache.commons.io.IOUtils.copy(inputStream,outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    private boolean checkFileMd5(File file,String fileMd5){
        try {
            FileInputStream fileInputStream= new FileInputStream(file);
            String md5Hex = DigestUtils.md5Hex(fileInputStream);
            if (fileMd5.equalsIgnoreCase(md5Hex)){
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }
    public ResponseResult mergechunks(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        String chunkFileFolderPath = this.getChunkFileFolderPath(fileMd5);
        File chunkFileFolder = new File(chunkFileFolderPath);
        File[] files = chunkFileFolder.listFiles();
        List<File> fileList = Arrays.asList(files);
        String filePath = this.getFilePath(fileMd5, fileExt);
        File mergeFile = new File(filePath);
        mergeFile = this.mergeFile(fileList, mergeFile);
        if (mergeFile==null){
            ExceptionCast.cast(MediaCode.MERGE_FILE_FAIL);
        }
        boolean checkFileMd5 = this.checkFileMd5(mergeFile, fileMd5);
        if (!checkFileMd5){
            ExceptionCast.cast(MediaCode.MERGE_FILE_FAIL);
        }

        MediaFile mediaFile=new MediaFile();
        mediaFile.setFileId(fileMd5);
        mediaFile.setFileOriginalName(fileName);
        mediaFile.setFileName(fileMd5+"."+fileExt);
        mediaFile.setFilePath(fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/");
        mediaFile.setFileSize(fileSize);
        mediaFile.setUploadTime(new Date());
        mediaFile.setMimeType(mimetype);
        mediaFile.setFileType(fileExt);
        mediaFile.setFileStatus("301002");
        mediaFileRepository.save(mediaFile);

        this.sendProcessVideoMsg(mediaFile.getFileId());
        return new ResponseResult(CommonCode.SUCCESS);
    }

    public ResponseResult sendProcessVideoMsg(String mediaId){
        Optional<MediaFile> optionalMediaFile = mediaFileRepository.findById(mediaId);
        if (!optionalMediaFile.isPresent()){
            ExceptionCast.cast(CommonCode.FAIL);
        }
        Map<String,String> map=new HashMap<>();
        map.put("mediaId",mediaId);
        String jsonString = JSON.toJSONString(map);
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EX_MEDIA_PROCESSTASK,routingkey_medi_video,jsonString);
        } catch (AmqpException e) {
            e.printStackTrace();
            return new ResponseResult(CommonCode.FAIL);
        }

        return new ResponseResult(CommonCode.SUCCESS);
    }
    private File mergeFile(List<File> files,File mergeFile){
        try {
            if (mergeFile.exists()){
            mergeFile.delete();
        }else {
                mergeFile.createNewFile();
        }
            Collections.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    if (Integer.parseInt(o1.getName())>Integer.parseInt(o2.getName())){
                        return 1;
                    }
                    return -1;
                }
            });

            RandomAccessFile raf_write=new RandomAccessFile(mergeFile,"rw");
            byte[] b=new byte[1024];
            for (File chunkFile:files){
                RandomAccessFile raf_read=new RandomAccessFile(chunkFile,"r");
                int len=-1;
                while ((len=raf_read.read(b))!=-1){
                    raf_write.write(b,0,len);
                }
                raf_read.close();
            }
            raf_write.close();
            return mergeFile;
        } catch (IOException e) {
        e.printStackTrace();
        return null;
    }
    }
}
