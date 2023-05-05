package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Event;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    // 处理事件
    public void fireEvent(Event event){
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }
}
