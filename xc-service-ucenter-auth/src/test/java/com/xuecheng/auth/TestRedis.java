package com.xuecheng.auth;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 * @version 1.0
 **/
@SpringBootTest
@RunWith(SpringRunner.class)
public class TestRedis {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    //创建jwt令牌
    @Test
    public void testRedis(){
        //定义key
        String key = "user_token:97ca6e9a-f925-4b5c-b01c-76e4712e1b5c";
        //定义value
        Map<String,String> value = new HashMap<>();
        value.put("jwt","eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb21wYW55SWQiOm51bGwsInVzZXJwaWMiOm51bGwsInVzZXJfbmFtZSI6Iml0Y2FzdCIsInNjb3BlIjpbImFwcCJdLCJuYW1lIjpudWxsLCJ1dHlwZSI6bnVsbCwiaWQiOm51bGwsImV4cCI6MTU4MTM5MTYzMywianRpIjoiOTdjYTZlOWEtZjkyNS00YjVjLWIwMWMtNzZlNDcxMmUxYjVjIiwiY2xpZW50X2lkIjoiWGNXZWJBcHAifQ.AFV53ZwOZIb545u0qrgTqHnloGVf53vWsQGAktHATd9zMoq8grQ3R7d7FWwEgP21PloHaAGTD4jfeukSZiEQ39ZtG1DBKxI-bYWmHK_YqYPHAF7dLzhh0rjcbUtXAoPHrBQv9ijXG1sRTmII8cO_39necJTG0jS-niy0C-2Vsoxb4o2A2OLfxgxf8Bo2D1Ux8aYG3MmqO73gGETZRaaVX4L6n-oEX0fO8IrwtatzF0ppRPXUZcaiKciPGNkrvcVvOeDUzOaJOMFNankVieAtGyibSrWqL9b9r64kd8di7vvfA9xypC8xB2pFvtZgEF5oKahWIX-6PpLS8SxjjMe1WA");
        value.put("refresh_token","eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb21wYW55SWQiOm51bGwsInVzZXJwaWMiOm51bGwsInVzZXJfbmFtZSI6Iml0Y2FzdCIsInNjb3BlIjpbImFwcCJdLCJhdGkiOiI5N2NhNmU5YS1mOTI1LTRiNWMtYjAxYy03NmU0NzEyZTFiNWMiLCJuYW1lIjpudWxsLCJ1dHlwZSI6bnVsbCwiaWQiOm51bGwsImV4cCI6MTU4MTM5MTYzMywianRpIjoiNzM4ZjlkNGQtOWJlZS00ZTcxLTkzZTQtOGJmNjRjMDZlNTYzIiwiY2xpZW50X2lkIjoiWGNXZWJBcHAifQ.j7VP2Bx-L8luJ6kD8aeXSnLZ20FuPi4nXz-099lbV06f3Hs6JzMpElLSqAf0qfsz_a-s6IGdcC0ee24HyUp2ogV7ZsGdK0bZMRyjMXlhWQ6uAMgmtx743sl7QWh0niJkkrnWSboQrF0dMOQiJ1R4WRglSwUddbz0zQXcoF8VbVW-E-oGTqdAHmDOW-crflQ9kdPhVw1U9xannWj5EglhiHZaHnC8bqiaORfkBUKzsegooR724Wm19u4CIzS3hnQDjcTfx-00wu1BC9yq4x_xJtthHBptKeo0Z6hfoTO6BhotSwcbCt_0N-NdlGl7CrTK2hmepJOBDlg4lsus5kIvaw");
        String jsonString = JSON.toJSONString(value);
        //校验key是否存在，如果不存在则返回-2
        Long expire = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        System.out.println(expire);
        //存储数据
        stringRedisTemplate.boundValueOps(key).set(jsonString,30, TimeUnit.SECONDS);
        //获取数据
        String string = stringRedisTemplate.opsForValue().get(key);
        System.out.println(string);


    }


}
