package org.hy.common.thread.junit;

import org.hy.common.thread.Jobs;
import org.hy.common.thread.ThreadPool;
import org.hy.common.xml.XJava;

import org.hy.common.Date;
import org.hy.common.thread.Job;
import org.hy.common.xml.annotation.Xjava;





/**
 * 测试单元：任务配置信息
 * 
 * @author      ZhengWei(HY)
 * @createDate  2014-07-23
 */
@Xjava
public class JU_Jobs
{
    
    public static void main(String [] args) throws Exception
    {
        ThreadPool.setMaxThread(2);
        ThreadPool.setMinThread(0);
        ThreadPool.setMinIdleThread(0);
        ThreadPool.setIntervalTime(100);
        ThreadPool.setIdleTimeKill(10);
        ThreadPool.setWatch(true);
        
        
        XJava.parserAnnotation("org.hy.common.thread.junit.JU_Jobs");
        
        Jobs v_Jobs  = new Jobs();
        Job  v_Job01 = new Job();
        Job  v_Job02 = new Job();
        
        v_Job01.setCode("TEST_001");
        v_Job01.setName("测试A");
        v_Job01.setIntervalType(Job.$IntervalType_Minute);
        v_Job01.setIntervalLen(1);
        v_Job01.setStartTime("2014-01-01 00:00:00");
        v_Job01.setXjavaID("JU_Jobs");
        v_Job01.setMethodName("test_Job_Execute");
        v_Job01.setInitExecute(true);
        
        v_Job02.setCode("TEST_002");
        v_Job02.setName("测试B");
        v_Job02.setIntervalType(Job.$IntervalType_Minute);
        v_Job02.setIntervalLen(1);
        v_Job02.setStartTime("2014-01-01 00:00:30");
        v_Job02.setXjavaID("JU_Jobs");
        v_Job02.setMethodName("test_Job_Execute");
        v_Job02.setInitExecute(true);
        
        
        v_Jobs.addJob(v_Job01);
        v_Jobs.addJob(v_Job02);
        v_Jobs.startup();
    }
    
    
    
    public void test_Job_Execute()
    {
        ThreadPool.sleep(5 * 1000);
        System.out.println("Job Time: " + new Date());
    }
    
}
