package org.hy.common.thread;

import org.hy.common.Date;
import org.hy.common.xml.log.Logger;





/**
 * 操作系统是否正常的测试（如来回调整时间、时间跳跃、时间波动等）
 *
 * @author      ZhengWei(HY)
 * @createDate  2018-12-05
 * @version     v1.0
 */
public class SysTimeTest
{
    private static final Logger $Logger = Logger.getLogger(SysTimeTest.class ,true);
    
    
    /**
     * 间隔1秒输出当前时间的测试
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-12-05
     * @version     v1.0
     *
     * @throws InterruptedException
     */
    public String show1000() throws InterruptedException
    {
        return show(1000);
    }
    
    
    
    /**
     * 间隔0.5秒输出当前时间的测试
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-12-05
     * @version     v1.0
     *
     * @throws InterruptedException
     */
    public String show500() throws InterruptedException
    {
        return show(500);
    }
    
    
    
    /**
     * 间隔0.2秒输出当前时间的测试
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-12-05
     * @version     v1.0
     *
     * @throws InterruptedException
     */
    public String show200() throws InterruptedException
    {
        return show(200);
    }
    
    
    
    /**
     * 间隔0.1秒输出当前时间的测试
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-12-05
     * @version     v1.0
     *
     * @throws InterruptedException
     */
    public String show100() throws InterruptedException
    {
        return show(100);
    }
    
    
    
    /**
     * 间隔0.05秒输出当前时间的测试
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-12-05
     * @version     v1.0
     *
     * @throws InterruptedException
     */
    public String show50() throws InterruptedException
    {
        return show(50);
    }
    
    
    
    /**
     * 间隔0.02秒输出当前时间的测试
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-12-05
     * @version     v1.0
     *
     * @throws InterruptedException
     */
    public String show20() throws InterruptedException
    {
        return show(20);
    }
    
    
    
    /**
     * 间隔0.01秒输出当前时间的测试
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-12-05
     * @version     v1.0
     *
     * @throws InterruptedException
     */
    public String show10() throws InterruptedException
    {
        return show(10);
    }
    
    
    
    /**
     * 间隔多少毫秒输出当前操作系统的时间一次。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-12-05
     * @version     v1.0
     *
     * @param i_Millisecond  间隔毫秒
     * @throws InterruptedException
     */
    public String show(long i_Millisecond) throws InterruptedException
    {
        StringBuilder v_Buffer = new StringBuilder();
        long          v_Count  = 60 * 1000 / i_Millisecond;
        
        for (int i=1; i<=v_Count; i++)
        {
            v_Buffer.append(Date.getNowTime().getFullMilli()).append("\n");
            
            Thread.sleep(i_Millisecond);
        }
        
        $Logger.info("System time test：Output every " + i_Millisecond + " millisecond.\n" + v_Buffer.toString());
        return v_Buffer.toString();
    }
    
}
