package com.xuecheng.order.service;

import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.framework.domain.task.XcTaskHis;
import com.xuecheng.order.dao.XcTaskHisRepository;
import com.xuecheng.order.dao.XcTaskRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {
    @Autowired
    XcTaskRepository xcTaskRepository;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    XcTaskHisRepository xcTaskHisRepository;

    public List<XcTask> findTaskList(Date updateTime,int size){
        Pageable pageable=new PageRequest(0,size);

        Page<XcTask> xcTaskPage = xcTaskRepository.findByUpdateTimeBefore(pageable, updateTime);
        List<XcTask> content = xcTaskPage.getContent();
        return content;

    }


    public void publish(XcTask xcTask, String ex, String routingKey) {
        Optional<XcTask> optionalXcTask = xcTaskRepository.findById(xcTask.getId());
        if (optionalXcTask.isPresent()){
            rabbitTemplate.convertAndSend(ex,routingKey,xcTask);
            XcTask task = optionalXcTask.get();
            task.setUpdateTime(new Date());
            xcTaskRepository.save(task);
        }

    }
    @Transactional
    public int getTask(String id, int version) {
        int count = xcTaskRepository.updateTaskVersion(id, version);
        return count;
    }

    @Transactional
    public void finishTask(String taskId){
        Optional<XcTask> optional = xcTaskRepository.findById(taskId);
        if (optional.isPresent()){
            XcTaskHis xcTaskHis=new XcTaskHis();
            XcTask xcTask = optional.get();
            xcTask.setDeleteTime(new Date());
            BeanUtils.copyProperties(xcTask,xcTaskHis);
            xcTaskHisRepository.save(xcTaskHis);
            xcTaskRepository.delete(xcTask);
        }
    }
}
