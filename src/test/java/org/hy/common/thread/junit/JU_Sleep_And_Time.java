package org.hy.common.thread.junit;

import org.hy.common.Date;
import org.hy.common.xml.log.Logger;
import org.junit.Test;




/**
 * 测试Sleep，在系统时间紊乱情况下的情况
 * 
 * @author     ZhengWei(HY)
 * @createDate 2021-04-08
 */
public class JU_Sleep_And_Time
{
    
    private static final Logger $Logger = Logger.getLogger(JU_Sleep_And_Time.class ,true);
    
    
    
    @Test
    public void test_SleepTime() throws InterruptedException
    {
        $Logger.info(Date.getNowTime().getFullMilli());
        
        Thread.sleep(5 * 1000);
        
        $Logger.info(Date.getNowTime().getFullMilli());
    }
    
}
