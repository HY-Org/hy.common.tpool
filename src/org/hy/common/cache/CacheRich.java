package org.hy.common.cache;

import java.util.List;
import java.util.Vector;

import org.hy.common.thread.ThreadPool;





/**
 * 高速缓存 -- 财富型
 * 
 * 此缓存如同投资与收益。先投资(预先在缓存中创建后对象)，之后等待收益(缓存中创建好的对象被使用)。
 * 
 * 缓存分为：
 *   1. 容量大小(总资产=投资+收益)  RichSize 
 *   2. 缓存大小(投资)            InvestSize
 *   3. 使用大小(收益)            IncomeSize
 * 
 * 当缓存大小小于 cacheNextSize 时，将自动扩展(投资)
 * 
 * 此缓存的容量大小只增加不减少，除非 clear();
 * 
 * <O>指缓存中元素的对象类型
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-11-29
 */
public abstract class CacheRich<O> extends Cache<List<O> ,O>
{
	
	/** 使用大小(收益) */
	protected int                      incomeSize;
	
	
	
	/**
	 * 回调方法。获取缓存中存放的元数据(保护类型)
	 * 
	 * @return
	 */
	protected abstract O callBack_NewCacheMetadata();
	
	
	
	/**
	 * 构造器
	 */
	public CacheRich()
	{
		super(new Vector<O>());
	}
	
	
	/**
	 * 构造器
	 * 
	 * @param i_CacheInitSize  缓存初始化大小
	 * @param i_CacheNextSize  缓存每次扩展大小
	 */
	public CacheRich(int i_CacheInitSize ,int i_CacheNextSize)
	{
		super(new Vector<O>() ,i_CacheInitSize ,i_CacheNextSize);
	}
	
	
	/**
	 * 回调方法。将new好的元数对象实例存放在缓存中(保护类型)
	 * 
	 * @param i_CacheMetadata  元数据对象实例
	 */
	@Override
	protected void callBack_AddCacheMetadata(O i_CacheMetadata)
	{
		this.cache.add(i_CacheMetadata);
	}
	
	
	/**
	 * 从缓存中获取一个 new 好的实例对象
	 * 
	 * 这是向外提供的最主要的方法
	 * 
	 * @return
	 */
	@Override
	public synchronized O newObject()
	{
		O v_Obj = null;
		
		while ( v_Obj == null )
		{
			if ( this.size() >= 1 )
			{
				v_Obj = this.cache.get(this.incomeSize);
			}
			
			if ( v_Obj == null )
			{
				ThreadPool.sleep(50);
			}
		}
		
		this.incomeSize++;
		
		if ( this.size() < this.cacheNextSize )
		{
			this.nextCache();
		}

		return v_Obj; 
	}
	
	
	/**
	 * 获取曾经创建过的对象实例的个数
	 * 
	 * @return
	 */
	@Override
	public int getCreatedCount()
	{
		return this.cache.size();
	}
	
	
	/**
	 * 获取被有效使用过的对象实例的个数
	 * 
	 * @return
	 */
	@Override
	public int getUsedCount()
	{
		return this.incomeSize;
	}
	
	
	/**
	 * 获取缓存现有大小
	 * 
	 * @return
	 */
	@Override
	public int size()
	{
		return this.cache.size() - this.incomeSize;
	}
	
	
	/**
	 * 清空缓存
	 */
	@Override
	public void clear()
	{
		this.cache.clear();
		this.incomeSize = 0;
	}
	

	/**
	 * 获取被有效使用过的对象实例的集合
	 * 
	 * @return
	 */
	public List<O> getUsedList()
	{
		if ( this.incomeSize >= 1 )
		{
			return this.cache.subList(0, this.incomeSize);
		}
		else
		{
			return new Vector<O>();
		}
	}
	
	
	/*
    ZhengWei(HY) Del 2016-07-30
    不能实现这个方法。首先JDK中的Hashtable、ArrayList中也没有实现此方法。
    它会在元素还有用，但集合对象本身没有用时，释放元素对象
    
    一些与finalize相关的方法，由于一些致命的缺陷，已经被废弃了
	protected void finalize() throws Throwable 
	{
		super.finalize();
	}
	*/
	
}
