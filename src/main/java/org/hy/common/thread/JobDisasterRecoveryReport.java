package org.hy.common.thread;

import org.hy.common.Date;
import org.hy.common.xml.SerializableDef;





/**
 * 定时任务的灾备多活集群服务报告
 *
 * @author      ZhengWei(HY)
 * @createDate  2019-02-26
 * @version     v1.0
 */
public class JobDisasterRecoveryReport extends SerializableDef
{
    
    private static final long serialVersionUID = 3006584976844777702L;
    
    

    /** 主机名称 */
    private String   hostName;
    
    /** 端口号 */
    private int      port;
    
    /** 服务本身是否正常 */
    private boolean  isOK;
    
    /** 是否为Master主服务 */
    private boolean  isMaster;
    
    /** 启动时间。即 startup() 方法的执行时间 */
    private Date     startTime;
    
    /** 得到Master执行权限的时间点。当为Slave时，此属性为NULL */
    private Date     masterTime;

    
    
    /**
     * 获取：主机名称
     */
    public String getHostName()
    {
        return hostName;
    }
    

    
    /**
     * 获取：端口号
     */
    public int getPort()
    {
        return port;
    }
    

    
    /**
     * 获取：是否为Master主服务
     */
    public boolean isMaster()
    {
        return isMaster;
    }
    

    
    /**
     * 获取：启动时间。即 startup() 方法的执行时间
     */
    public Date getStartTime()
    {
        return startTime;
    }
    

    
    /**
     * 设置：主机名称
     * 
     * @param hostName
     */
    public void setHostName(String hostName)
    {
        this.hostName = hostName;
    }
    

    
    /**
     * 设置：端口号
     * 
     * @param port
     */
    public void setPort(int port)
    {
        this.port = port;
    }
    

    
    /**
     * 设置：是否为Master主服务
     * 
     * @param isMaster
     */
    public void setMaster(boolean isMaster)
    {
        this.isMaster = isMaster;
    }
    

    
    /**
     * 设置：启动时间。即 startup() 方法的执行时间
     * 
     * @param startTime
     */
    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }


    
    /**
     * 获取：服务本身是否正常
     */
    public boolean isOK()
    {
        return isOK;
    }
    

    
    /**
     * 设置：服务本身是否正常
     * 
     * @param isOK
     */
    public void setOK(boolean isOK)
    {
        this.isOK = isOK;
    }


    
    /**
     * 获取：得到Master执行权限的时间点。当为Slave时，此属性为NULL
     */
    public Date getMasterTime()
    {
        return masterTime;
    }
    

    
    /**
     * 设置：得到Master执行权限的时间点。当为Slave时，此属性为NULL
     * 
     * @param masterTime
     */
    public void setMasterTime(Date masterTime)
    {
        this.masterTime = masterTime;
    }
    
}
