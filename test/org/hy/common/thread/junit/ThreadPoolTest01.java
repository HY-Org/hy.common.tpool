package org.hy.common.thread.junit;

import static org.junit.Assert.*;

import org.hy.common.thread.ThreadBase;
import org.hy.common.thread.ThreadPool;
import org.hy.common.xml.log.Logger;
import org.junit.Test;





/**
 * 对自己编写的线程池功能，进行整体的测试
 * 
 * 主要测试线程中生成线程是否有问题
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-11-25
 */
public class ThreadPoolTest01
{
    private static final Logger $Logger = Logger.getLogger(ThreadPoolTest01.class ,true);
    
    

    @Test
    public void test()
    {
        ThreadPool.setMaxThread(1000);
        ThreadPool.setMinThread(10);
        ThreadPool.setMinIdleThread(2);
        ThreadPool.setIntervalTime(100);
        ThreadPool.setIdleTimeKill(600);
        ThreadPool.setWatch(true);
        
        
        
        
        
        ThreadBase v_ThreadBase = ThreadPool.getThreadInstance(new TestThreadTask01());
        v_ThreadBase.startupAndExecuteTask();
        
        
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
