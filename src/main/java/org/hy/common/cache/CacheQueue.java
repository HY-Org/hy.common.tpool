package org.hy.common.cache;

import org.hy.common.Queue;





/**
 * 高速缓存 -- 队列型(先进先出)
 * 
 * 此缓存建立在 Queue 的基础上。即从缓存中取出一个元素，缓存中就少一个元素。
 * 当缓存中的缓存元素小于 cacheNextSize 时，将自动扩展。
 * 
 * <O>指缓存中元素的对象类型
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-11-25
 */
public abstract class CacheQueue<O> extends Cache<Queue<O> ,O>
{
    
    /**
     * 回调方法。获取缓存中存放的元数据(保护类型)
     * 
     * @return
     */
    @Override
    protected abstract O callBack_NewCacheMetadata();
    
    
    
    /**
     * 构造器
     */
    public CacheQueue()
    {
        super(new Queue<O>());
    }
    
    
    /**
     * 构造器
     * 
     * @param i_CacheInitSize  缓存初始化大小
     * @param i_CacheNextSize  缓存每次扩展大小
     */
    public CacheQueue(int i_CacheInitSize ,int i_CacheNextSize)
    {
        super(new Queue<O>() ,i_CacheInitSize ,i_CacheNextSize);
    }
    
    
    /**
     * 回调方法。将new好的元数对象实例存放在缓存中(保护类型)
     * 
     * @param i_CacheMetadata  元数据对象实例
     */
    @Override
    protected void callBack_AddCacheMetadata(O i_CacheMetadata)
    {
        this.cache.put(i_CacheMetadata);
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
        O v_Obj = this.cache.get();
        
        if ( this.cache.size() <= this.cacheNextSize )
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
        return (int)this.cache.getPutedCount();
    }
    
    
    /**
     * 获取被有效使用过的对象实例的个数
     * 
     * @return
     */
    @Override
    public int getUsedCount()
    {
        return (int)this.cache.getOutedCount();
    }
    
    
    /**
     * 获取缓存现有大小
     * 
     * @return
     */
    @Override
    public long size()
    {
        return this.cache.size();
    }
    
    
    /**
     * 清空缓存
     */
    @Override
    public void clear()
    {
        this.cache.clear();
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
