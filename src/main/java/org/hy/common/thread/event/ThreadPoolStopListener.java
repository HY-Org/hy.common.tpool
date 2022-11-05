package org.hy.common.thread.event;





/**
 * 停止线程线中所有线程的事件监听器接口
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-27
 */
public interface ThreadPoolStopListener 
{
    
    /**
     * 停止线程线中所有线程之前
     * 
     * @param e
     * @return   返回值表示是否继续
     */
    public boolean stopBefore(ThreadPoolStopEvent e);
    
    

    /**
     * 停止线程线中所有线程进度
     * 
     * @param e
     */
    public void stopProcess(ThreadPoolStopEvent e);
    
    
    
    /**
     * 停止线程线中所有线程完成之后
     * 
     * @param e
     */
    public void stopAfter(ThreadPoolStopEvent e);
    
}
