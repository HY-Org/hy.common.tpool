package org.hy.common.thread;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.hy.common.thread.event.DefaultThreadPoolStopEvent;
import org.hy.common.thread.event.ThreadPoolStopEvent;
import org.hy.common.thread.event.ThreadPoolStopListener;





/**
 * 停止线程池
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-06-08
 */
public class ThreadPoolStop implements Runnable
{
	/** 自定义事件的监听器集合 */
	private Collection<ThreadPoolStopListener>    tPoolStopListeners;
	
	
	
	/**
	 * 注册事件
	 * 
	 * @param e
	 */
	public void addThreadPoolStopListener(ThreadPoolStopListener e)
	{
		if ( this.tPoolStopListeners == null )
		{
			this.tPoolStopListeners = new HashSet<ThreadPoolStopListener>();
		}
		
		this.tPoolStopListeners.add(e);
	}
	
	
	
	/**
	 * 移除事件
	 * 
	 * @param e
	 */
	public void removeThreadPoolStopListener(ThreadPoolStopListener e)
	{
		if ( this.tPoolStopListeners == null )
		{
			return;
		}
		
		this.tPoolStopListeners.remove(e);
	}
	
	
	
	/**
	 * 触发停止线程线中所有线程之前的事件
	 * 
	 * @param i_Event
	 * @return   返回值表示是否继续
	 */
	protected boolean fireThreadPoolStopBeforeListener(ThreadPoolStopEvent i_Event)
	{
		if ( this.tPoolStopListeners == null )
		{
			return true;
		}
		
		return notifyThreadPoolStopBeforeListeners(i_Event);
	}
	
	
	
	/**
	 * 触发停止线程线中每一个线程的事件
	 * 
	 * @param i_Event
	 */
	protected void fireThreadPoolStopingListener(ThreadPoolStopEvent i_Event)
	{
		if ( this.tPoolStopListeners == null )
		{
			return;
		}
		
		notifyThreadPoolStopingListeners(i_Event);
	}
	
	
	
	/**
	 * 触发停止线程线中所有线程完成之后的事件
	 * 
	 * @param i_Event
	 */
	protected void fireThreadPoolStopAfterListener(ThreadPoolStopEvent i_Event)
	{
		if ( this.tPoolStopListeners == null )
		{
			return;
		}
		
		notifyThreadPoolStopAfterListeners(i_Event);
	}

	
	
	/**
	 * 通知所有注册停止线程线中所有线程之前的事件监听的对象
	 * 
	 * @param i_Event
	 * @return   返回值表示是否继续
	 */
	private boolean notifyThreadPoolStopBeforeListeners(ThreadPoolStopEvent i_Event)
	{
		Iterator<ThreadPoolStopListener> v_Iter       = this.tPoolStopListeners.iterator();
		boolean                          v_IsContinue = true;

		while ( v_IsContinue && v_Iter.hasNext() ) 
		{
			ThreadPoolStopListener v_Listener = v_Iter.next();

			v_IsContinue = v_Listener.stopBefore(i_Event);
		}
		
		return v_IsContinue;
	}
	
	
	
	/**
	 * 通知所有注册停止线程线中所有线程进度事件监听的对象
	 * 
	 * @param i_Event
	 */
	private void notifyThreadPoolStopingListeners(ThreadPoolStopEvent i_Event)
	{
		Iterator<ThreadPoolStopListener> v_Iter = this.tPoolStopListeners.iterator(); 

		while ( v_Iter.hasNext() ) 
		{
			ThreadPoolStopListener v_Listener = v_Iter.next();

			v_Listener.stopProcess(i_Event);
		}
	}

	
	
	/**
	 * 通知所有注册停止线程线中所有线程完成之后的事件监听的对象
	 * 
	 * @param i_Event
	 */
	private void notifyThreadPoolStopAfterListeners(ThreadPoolStopEvent i_Event)
	{
		Iterator<ThreadPoolStopListener> v_Iter = this.tPoolStopListeners.iterator();

		while ( v_Iter.hasNext() ) 
		{
			ThreadPoolStopListener v_Listener = v_Iter.next();

			v_Listener.stopAfter(i_Event);
		}
	}
	
	
	
	public void run()
	{
		int v_ThreadPoolSize = ThreadPool.THREADPOOL.size();
		DefaultThreadPoolStopEvent v_Event = new DefaultThreadPoolStopEvent(this ,v_ThreadPoolSize);
		
		this.fireThreadPoolStopBeforeListener(v_Event);
		
		
		
		for (int v_Index=v_ThreadPoolSize-1; v_Index>=0; v_Index--)
		{
			ThreadBase v_ThreadBase = ThreadPool.getThreadBase(v_Index);
			
			if ( v_ThreadBase != null )
			{
				while ( v_ThreadBase.isHaveTask() )
				{
					try
					{
						Thread.sleep(50);
					}
					catch (Exception exce)
					{
						exce.printStackTrace();
					}
				}
				
				v_ThreadBase.shutdown();
			}
		}
		
		
		
		while ( ThreadPool.THREADPOOL.size() >= 1)
		{
			ThreadBase v_ThreadBase = null;
			
			try
			{
    			v_ThreadBase = ThreadPool.THREADPOOL.remove(ThreadPool.THREADPOOL.size() - 1);
    			
    			v_ThreadBase.shutdown();
			}
			catch (Exception exce)
			{
			    exce.printStackTrace();
			}
			v_ThreadBase = null;
			
			
			v_Event.setCompleteSize(v_ThreadPoolSize - ThreadPool.THREADPOOL.size());
			this.fireThreadPoolStopingListener(v_Event);
		}

		
		
		v_Event.setCompleteSize(v_ThreadPoolSize);
		this.fireThreadPoolStopAfterListener(v_Event);
	}
	
}
