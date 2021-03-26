package org.hy.common.thread.ui;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import org.hy.common.thread.ThreadBase;





/**
 * 视窗化监视线程池的情况
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-11-24
 */
public class ThreadPoolWatch extends JFrame
{
    private static final long      serialVersionUID = -8949345803871596595L;
    
    private static ThreadPoolWatch $ThreadPoolWatch;
    
    
    private JScrollPane            jpane;
    
    private JTable                 jtable;
    
    private DefaultTableModel      jtableMode;
    
    
    
    public synchronized static ThreadPoolWatch getInstance()
    {
        if ( $ThreadPoolWatch == null )
        {
            $ThreadPoolWatch = new ThreadPoolWatch();
        }
        
        return $ThreadPoolWatch;
    }
    
    
    private ThreadPoolWatch()
    {
        this.init();
    }
    
    
    /**
     * 更新监视窗口的标题
     * 
     * @param i_Title
     */
    public void updateTitle(String i_Title)
    {
        this.setTitle("Thread Pool Watch    "+ i_Title);
    }
    
    
    /**
     * 向表格中添加一行，返回值为添加行所在的行号（下标从零开始）
     * 
     * @param i_ThreadBase
     * @return
     */
    public synchronized int addRow(ThreadBase i_ThreadBase)
    {
        int        v_Ret     = this.jtableMode.getRowCount();
        String []  v_RowData = new String[6];
        int        v_ColNo   = 0;
        
        
        v_RowData[v_ColNo++] = i_ThreadBase.getThreadNo();
        
        if ( i_ThreadBase.getTaskObject() == null )
        {
            v_RowData[v_ColNo++] = " ";
            v_RowData[v_ColNo++] = "0";
            v_RowData[v_ColNo++] = i_ThreadBase.getThreadRunStatus().toString();
            v_RowData[v_ColNo++] = String.valueOf(i_ThreadBase.getExecuteTaskCount());
            v_RowData[v_ColNo++] = " ";
        }
        else
        {
            v_RowData[v_ColNo++] = i_ThreadBase.getTaskObject().getTaskName();
            v_RowData[v_ColNo++] = "0";
            v_RowData[v_ColNo++] = i_ThreadBase.getThreadRunStatus().toString();
            v_RowData[v_ColNo++] = String.valueOf(i_ThreadBase.getExecuteTaskCount());
            v_RowData[v_ColNo++] = i_ThreadBase.getTaskObject().getTaskDesc();
        }
        
        
        this.jtableMode.addRow(v_RowData);
        
        return v_Ret;
    }
    
    
    /**
     * 更新表格中的数据
     * 
     * @param i_RowIndex  行号（下标从零开始）
     * @param i_ColIndex  列号（下标从零开始）
     * @param i_Value     新值
     */
    public void updateRow(int i_RowIndex ,int i_ColIndex ,String i_NewValue)
    {
        if ( this.jtableMode.getRowCount() > i_RowIndex )
        {
            this.jtableMode.setValueAt(i_NewValue, i_RowIndex, i_ColIndex);
        }
    }
    
    
    /**
     * 更新表格中的数据
     * 
     * @param i_RowIndex     行号（下标从零开始）
     * @param i_ColIndexObj  列号常量
     * @param i_Value        新值
     */
    public void updateRow(int i_RowIndex ,WatchTableColumnIndex i_ColIndexObj ,String i_NewValue)
    {
        int v_ColIndex = 0;
        
        if ( WatchTableColumnIndex.$ThreadNo.equals(i_ColIndexObj) )
        {
            v_ColIndex = 0;
        }
        else if ( WatchTableColumnIndex.$TaskName.equals(i_ColIndexObj) )
        {
            v_ColIndex = 1;
        }
        else if ( WatchTableColumnIndex.$TotalTime.equals(i_ColIndexObj) )
        {
            v_ColIndex = 2;
        }
        else if ( WatchTableColumnIndex.$RunStatus.equals(i_ColIndexObj) )
        {
            v_ColIndex = 3;
        }
        else if ( WatchTableColumnIndex.$ExecCount.equals(i_ColIndexObj) )
        {
            v_ColIndex = 4;
        }
        else if ( WatchTableColumnIndex.$TaskDesc.equals(i_ColIndexObj) )
        {
            v_ColIndex = 5;
        }
        else
        {
            return;
        }
        
        
        this.updateRow(i_RowIndex, v_ColIndex ,i_NewValue);
    }
    
    
    /**
     * 监视
     * 
     * @param i_Millis
     */
    public void watch()
    {
        this.setVisible(true);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }
    
    
    private void init()
    {
        // 设置窗口
        this.setEnabled(true);
        this.setTitle("Thread Pool Watch");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);      // 关闭方式为: 只关闭自己,不关闭整个程序。
        
        
        
        // 设置表格--数据模式
        this.jtableMode = new DefaultTableModel();
        this.jtableMode.addColumn("Thread No");          // 线程编号
        this.jtableMode.addColumn("Task No");            // 任务编号
        this.jtableMode.addColumn("Total Time");         // 任务累计用时
        this.jtableMode.addColumn("Run Status");         // 线程运行状态
        this.jtableMode.addColumn("Exec Count");         // 已执行完成的任务次数
        this.jtableMode.addColumn("Task Desc");          // 任务描述
        
        
        
        // 设置表格
        this.jtable = new JTable();
        this.jtable.setModel(this.jtableMode);
        this.jtable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        this.jtable.getColumnModel().getColumn(0).setPreferredWidth(100);
        this.jtable.getColumnModel().getColumn(1).setPreferredWidth(240);
        this.jtable.getColumnModel().getColumn(2).setPreferredWidth(100);
        this.jtable.getColumnModel().getColumn(3).setPreferredWidth(80);
        this.jtable.getColumnModel().getColumn(4).setPreferredWidth(80);
        this.jtable.getColumnModel().getColumn(5).setPreferredWidth(800);
        
        
        
        // 设置面板
        this.jpane = new JScrollPane();
        this.jpane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.jpane.setVerticalScrollBarPolicy(  ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        
        
        this.jpane.getViewport().add(this.jtable);
        this.add(this.jpane);
    }
    
    
    
    public static void main(String argc[]) 
    {
        ThreadPoolWatch v_WatchFrame = new ThreadPoolWatch();
        
        v_WatchFrame.setVisible(true);
        
    }
    
}
