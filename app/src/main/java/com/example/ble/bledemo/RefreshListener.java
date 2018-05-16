package com.example.ble.bledemo;

/**
 * 处理蓝牙开门对应的接口
 */
public interface RefreshListener {

    /**
     * * 开始连接的处理
     *
     * @return true 连接有效,false 连接无效
     */
    boolean startConnect();


    /**
     * 完成一次数据发送周期
     */
    void completeOneCycleProcess(String receiveNotify);
}
