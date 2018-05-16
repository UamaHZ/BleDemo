package com.example.ble.bledemo;

/**
 * Created by guozhen.hou on 2018/2/6.
 */

public class BleConfigConstant {
    public static final String NOT_DETECTED_DEVICE = "未检测到相关设备";
    public static final String MANUAL_OPEN_BLUETOOTH = "要使用摇一摇智能开门功能,请手动打开蓝牙";

    /**
     * 服务uid
     */
    public final static String SERVICE_UUID = "72a2f746-0132-0819-7ba0-f4c73b96597c";
    /**
     * 通知uid
     */
    public final static String NOTIFY_UUID = "96b54186-f57c-ada0-8c00-91ae0ef46a06";
    /**
     * 写uid
     */
    public final static String WRITE_UUID = "e05cd458-47c7-723e-c799-514fa46e27e9";

    /**
     * 协议包头
     */
    public final static String PROTOCOL_PACKAGE_HEAD = "FE01007600010001";

    public static final String DOOR_OPEN_SUCCESS = "0";//最终开门结果，成功
    public static final String DOOR_OPEN_ERROR = "1";//最终开门结果失败

}
