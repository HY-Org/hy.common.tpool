package org.hy.common.thread.event;





/**
 * 任务组的事件监听器接口
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-28
 */
public interface TaskGroupListener 
{
    
    /**
     * 启用任务组所有任务的事件
     * 
     * @param e
     */
    public void startupAllTask(TaskGroupEvent e);
    
    
    
    /**
     * 任务组中任务都完成后的事件
     * 
     * @param e
     */
    public void finishAllTask(TaskGroupEvent e);
    
}
