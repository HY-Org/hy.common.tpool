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
        for (int i=1; i<=2; i++)
        {
            Job v_Job = new Job();
            
            v_Job.setCode("TEST_" + i);
            v_Job.setName("测试" + i);
            v_Job.setIntervalType(Job.$IntervalType_Minute);
            v_Job.setIntervalLen(1);
            v_Job.setStartTime("2014-01-01 00:00:00");
            v_Job.setXid("JU_Jobs");
            v_Job.setMethodName("test_Job_Execute");
            v_Job.setInitExecute(true);
            
            v_Jobs.addJob(v_Job);
        }
        
        Job v_Job = new Job();
        v_Job.setCode("TEST_" + 0);
        v_Job.setName("测试" + 0);
        v_Job.setIntervalType(Job.$IntervalType_Minute);
        v_Job.setIntervalLen(1);
        v_Job.setStartTime("2014-01-01 00:00:00");
        v_Job.setXid("JU_Jobs");
        v_Job.setMethodName("test_Job_Execute02");
        v_Job.setInitExecute(true);
        
        v_Jobs.addJob(v_Job);
        
        v_Jobs.startup();
    }
    
    
    
    public void test_Job_Execute()
    {
        System.out.println("Job Time: " + (new Date()).getFullMilli());
        ThreadPool.sleep(5 * 1000);
    }
    
    
    public void test_Job_Execute02()
    {
        ThreadPool.sleep(6 * 1000);
        System.out.println();
    }
    
}
