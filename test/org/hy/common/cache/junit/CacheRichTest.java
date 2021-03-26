package org.hy.common.cache.junit;

import static org.junit.Assert.*;

import org.hy.common.thread.ThreadPool;
import org.hy.common.xml.log.Logger;
import org.junit.Test;





/**
 * 高速缓存 -- 财富型 的测试单元
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-11-29
 */
public class CacheRichTest 
{
    
    private static final Logger $Logger = Logger.getLogger(CacheRichTest.class ,true);
    
    

    @Test
    public void testNewObject() 
    {
        ThreadPool.setMaxThread(100);
        ThreadPool.setMinThread(1);
        ThreadPool.setMinIdleThread(0);
        ThreadPool.setIntervalTime(1000);
        ThreadPool.setIdleTimeKill(30);
        ThreadPool.setWatch(true);
        
        
        
        
        
        long v_StartTime = System.currentTimeMillis();
        
        CacheRichSample v_Cache = new CacheRichSample();
        
        long v_EndTime = System.currentTimeMillis();
        
        $Logger.info("-- Create CacheSample Time Length: " + (v_EndTime - v_StartTime));
        
        ThreadPool.sleep(10000);
        
        
        
        CacheRichSampleMetadata v_Metadata = null;
        for (int i=0; i<1000; i++)
        {
            v_StartTime = System.currentTimeMillis();

            v_Metadata = v_Cache.newObject();

            v_EndTime = System.currentTimeMillis();
            $Logger.info(i + "-- 创建用时: " + (v_EndTime - v_StartTime) 
                           + "       元数据创建时间: " + v_Metadata.toString());
            
            ThreadPool.sleep(10);
        }
        
        assertTrue(true);
        
        ThreadPool.sleep(600000);
        
        ThreadPool.shutdownAllThread();
    }

}
