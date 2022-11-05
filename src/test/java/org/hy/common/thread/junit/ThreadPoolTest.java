package org.hy.common.thread.junit;

import static org.junit.Assert.assertTrue;

import org.hy.common.thread.ThreadBase;
import org.hy.common.thread.ThreadPool;
import org.hy.common.xml.log.Logger;





/**
 * 对自己编写的线程池功能，进行整体的测试
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-11-23
 */
public class ThreadPoolTest
{
    private static final Logger $Logger = Logger.getLogger(ThreadPoolTest.class ,true);
    
    
    
    public void test()
    {
        ThreadPool.setMaxThread(1000);
        ThreadPool.setMinThread(10);
        ThreadPool.setMinIdleThread(2);
        ThreadPool.setIntervalTime(100);
        ThreadPool.setIdleTimeKill(60);
        ThreadPool.setWatch(true);
        
        for (int i=1; i<=100; i++)
        {
            TestThreadTask v_Task = new TestThreadTask();
            
            ThreadBase v_ThreadBase = ThreadPool.getThreadInstance(v_Task);
            
            v_ThreadBase.startupAndExecuteTask();
            
            // System.out.flush();
            $Logger.info(i + " 秒");
            $Logger.info(ThreadPool.showDetailInfo());
            ThreadPool.sleep(500);
        }
        
        
        while ( ThreadPool.getActiveThreadCount() >= 1 )
        {
            $Logger.info("-- " + ThreadPool.showInfo());
            ThreadPool.sleep(1000);
        }
        $Logger.info("-- " + ThreadPool.showInfo());
        
        ThreadPool.shutdownAllThread();
        
        assertTrue(true);
    }
    
}
